package com.qing_ying.ark.event;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.qing_ying.ark.Ark;
import com.qing_ying.ark.config.ArkConfig;
import com.qing_ying.ark.data.ArkAttachments;
import com.qing_ying.ark.data.BackpackEntry;
import com.qing_ying.ark.data.BackpackSlot;
import com.qing_ying.ark.data.HotbarBinding;
import com.qing_ying.ark.data.PlayerArkState;
import com.qing_ying.ark.menu.ArkInventoryMenu;
import com.qing_ying.ark.stat.ArkOptionalStatController;
import com.qing_ying.ark.stat.ArkStat;
import java.util.Collection;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDrownEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerXpEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public final class ArkCommonEvents {
    private static final ResourceLocation HEALTH_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(Ark.MOD_ID, "health_bonus");
    private static final ResourceLocation MELEE_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(Ark.MOD_ID, "melee_bonus");
    private static final ResourceLocation SPEED_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(Ark.MOD_ID, "speed_bonus");
    private static final ResourceLocation STEP_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(Ark.MOD_ID, "step_bonus");
    private static final ResourceLocation WEIGHT_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(Ark.MOD_ID, "weight_penalty");
    private static final ResourceLocation THIRST_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(Ark.MOD_ID, "thirst_penalty");
    private static final double HUNGER_EFFECT_DRAIN_PER_TICK = 0.0075D;
    public static final int DROP_ONE = 1;
    public static final int DROP_TEN = 10;
    public static final int DROP_ALL = -1;

    private ArkCommonEvents() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(ArkCommonEvents::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(ArkCommonEvents::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(ArkCommonEvents::onPickupXp);
        NeoForge.EVENT_BUS.addListener(ArkCommonEvents::onItemPickup);
        NeoForge.EVENT_BUS.addListener(ArkCommonEvents::onJump);
        NeoForge.EVENT_BUS.addListener(ArkCommonEvents::onUseFinish);
        NeoForge.EVENT_BUS.addListener(ArkCommonEvents::onDrown);
        NeoForge.EVENT_BUS.addListener(ArkCommonEvents::onClone);
        NeoForge.EVENT_BUS.addListener(ArkCommonEvents::onLogin);
        NeoForge.EVENT_BUS.addListener(ArkCommonEvents::onRespawn);
        NeoForge.EVENT_BUS.addListener(ArkCommonEvents::onDrops);
    }

    private static void onRegisterCommands(RegisterCommandsEvent event) {
        registerCommands(event.getDispatcher());
    }

    private static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("ark")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("respec")
                        .executes(context -> respecTargets(context.getSource(), List.of(context.getSource().getPlayerOrException())))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(context -> respecTargets(context.getSource(), EntityArgument.getPlayers(context, "targets")))))
                .then(Commands.literal("addpoint")
                        .then(Commands.argument("amount", IntegerArgumentType.integer())
                                .executes(context -> addPoints(context.getSource(), List.of(context.getSource().getPlayerOrException()), IntegerArgumentType.getInteger(context, "amount")))
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(context -> addPoints(context.getSource(), EntityArgument.getPlayers(context, "targets"), IntegerArgumentType.getInteger(context, "amount"))))))
                .then(Commands.literal("setpoint")
                        .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                .executes(context -> setPoints(context.getSource(), List.of(context.getSource().getPlayerOrException()), IntegerArgumentType.getInteger(context, "amount")))
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(context -> setPoints(context.getSource(), EntityArgument.getPlayers(context, "targets"), IntegerArgumentType.getInteger(context, "amount"))))))
                .then(Commands.literal("addxp")
                        .then(Commands.argument("amount", IntegerArgumentType.integer())
                                .executes(context -> addXp(context.getSource(), List.of(context.getSource().getPlayerOrException()), IntegerArgumentType.getInteger(context, "amount")))
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(context -> addXp(context.getSource(), EntityArgument.getPlayers(context, "targets"), IntegerArgumentType.getInteger(context, "amount"))))))
                .then(Commands.literal("addarkxp")
                        .then(Commands.argument("amount", IntegerArgumentType.integer())
                                .executes(context -> addXp(context.getSource(), List.of(context.getSource().getPlayerOrException()), IntegerArgumentType.getInteger(context, "amount")))
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(context -> addXp(context.getSource(), EntityArgument.getPlayers(context, "targets"), IntegerArgumentType.getInteger(context, "amount"))))))
                .then(Commands.literal("setxp")
                        .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                .executes(context -> setXp(context.getSource(), List.of(context.getSource().getPlayerOrException()), IntegerArgumentType.getInteger(context, "amount")))
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(context -> setXp(context.getSource(), EntityArgument.getPlayers(context, "targets"), IntegerArgumentType.getInteger(context, "amount"))))))
                .then(Commands.literal("setlevel")
                        .then(Commands.argument("level", IntegerArgumentType.integer(1))
                                .executes(context -> setLevel(context.getSource(), List.of(context.getSource().getPlayerOrException()), IntegerArgumentType.getInteger(context, "level")))
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(context -> setLevel(context.getSource(), EntityArgument.getPlayers(context, "targets"), IntegerArgumentType.getInteger(context, "level"))))))
                .then(Commands.literal("refill")
                        .executes(context -> refillTargets(context.getSource(), List.of(context.getSource().getPlayerOrException())))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(context -> refillTargets(context.getSource(), EntityArgument.getPlayers(context, "targets"))))));
    }

    private static int respecTargets(CommandSourceStack source, Collection<ServerPlayer> targets) {
        int total = 0;
        for (ServerPlayer player : targets) {
            PlayerArkState state = ArkAttachments.get(player);
            total += state.resetAllocatedPoints();
            applyDerivedState(player, state);
            ArkAttachments.sync(player);
        }
        final int refunded = total;
        source.sendSuccess(() -> Component.translatable("command.ark.respec_done", refunded), true);
        return total;
    }

    private static int addPoints(CommandSourceStack source, Collection<ServerPlayer> targets, int amount) {
        for (ServerPlayer player : targets) {
            PlayerArkState state = ArkAttachments.get(player);
            state.addAvailablePoints(amount);
            ArkAttachments.sync(player);
        }
        source.sendSuccess(() -> Component.translatable("command.ark.points_changed", amount, targets.size()), true);
        return targets.size();
    }

    private static int setPoints(CommandSourceStack source, Collection<ServerPlayer> targets, int amount) {
        for (ServerPlayer player : targets) {
            PlayerArkState state = ArkAttachments.get(player);
            state.setAvailablePoints(amount);
            ArkAttachments.sync(player);
        }
        source.sendSuccess(() -> Component.translatable("command.ark.points_set", amount, targets.size()), true);
        return targets.size();
    }

    private static int addXp(CommandSourceStack source, Collection<ServerPlayer> targets, int amount) {
        for (ServerPlayer player : targets) {
            PlayerArkState state = ArkAttachments.get(player);
            state.addExperience(amount);
            ArkAttachments.sync(player);
        }
        source.sendSuccess(() -> Component.translatable("command.ark.xp_changed", amount, targets.size()), true);
        return targets.size();
    }

    private static int setXp(CommandSourceStack source, Collection<ServerPlayer> targets, int amount) {
        for (ServerPlayer player : targets) {
            PlayerArkState state = ArkAttachments.get(player);
            long maxXp = Math.max(0L, ArkConfig.xpForNextLevel(state.getModLevel()) - 1L);
            state.setModXp(Math.min(maxXp, amount));
            ArkAttachments.sync(player);
        }
        source.sendSuccess(() -> Component.translatable("command.ark.xp_set", amount, targets.size()), true);
        return targets.size();
    }

    private static int setLevel(CommandSourceStack source, Collection<ServerPlayer> targets, int level) {
        for (ServerPlayer player : targets) {
            PlayerArkState state = ArkAttachments.get(player);
            state.setModLevel(level);
            state.setModXp(0L);
            state.setAvailablePoints(Math.max(0, (level - 1) - spentPoints(state)));
            applyDerivedState(player, state);
            ArkAttachments.sync(player);
        }
        source.sendSuccess(() -> Component.translatable("command.ark.level_set", level, targets.size()), true);
        return targets.size();
    }

    private static int refillTargets(CommandSourceStack source, Collection<ServerPlayer> targets) {
        for (ServerPlayer player : targets) {
            PlayerArkState state = ArkAttachments.get(player);
            state.addStamina(state.maxFor(ArkStat.STAMINA));
            state.addOxygen(state.maxFor(ArkStat.OXYGEN));
            state.addFood(state.maxFor(ArkStat.FOOD));
            if (ArkOptionalStatController.isActive(ArkStat.WATER)) {
                state.addWater(state.maxFor(ArkStat.WATER));
            }
            player.setHealth(player.getMaxHealth());
            ArkAttachments.sync(player);
        }
        source.sendSuccess(() -> Component.translatable("command.ark.refill_done", targets.size()), true);
        return targets.size();
    }

    private static int spentPoints(PlayerArkState state) {
        int spent = 0;
        for (ArkStat stat : ArkStat.values()) {
            spent += state.getAllocatedPoints(stat);
        }
        return spent;
    }

    private static void onPickupXp(PlayerXpEvent.PickupXp event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.level().isClientSide) return;
        PlayerArkState state = ArkAttachments.get(player);
        state.addExperience(event.getOrb().value);
        if (state.isDirty()) ArkAttachments.sync(player);
    }

    private static void onItemPickup(ItemEntityPickupEvent.Pre event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        ItemStack liveStack = event.getItemEntity().getItem();
        if (liveStack.isEmpty()) return;
        PlayerArkState state = ArkAttachments.get(player);
        ItemStack original = liveStack.copy();
        state.storeStack(original);
        liveStack.setCount(0);
        event.setCanPickup(TriState.FALSE);
        player.take(event.getItemEntity(), original.getCount());
        player.awardStat(Stats.ITEM_PICKED_UP.get(original.getItem()), original.getCount());
        player.onItemPickup(event.getItemEntity());
        event.getItemEntity().discard();
        ArkAttachments.sync(player);
    }

    private static void onJump(LivingEvent.LivingJumpEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        PlayerArkState state = ArkAttachments.get(player);
        if (isOverweight(player, state) || state.getStamina() <= 0.0D) {
            player.setDeltaMovement(player.getDeltaMovement().x, Math.min(0.0D, player.getDeltaMovement().y), player.getDeltaMovement().z);
            return;
        }
        double efficiency = ArkConfig.foodEfficiencyMultiplier(state.getAllocatedPoints(ArkStat.FOOD));
        state.addStamina(-ArkConfig.staminaJumpDrain());
        state.addFood(-ArkConfig.foodJumpDrain() / efficiency);
        if (ArkOptionalStatController.isActive(ArkStat.WATER)) {
            state.addWater(-ArkConfig.waterJumpDrain());
        }
    }

    private static void onUseFinish(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        PlayerArkState state = ArkAttachments.get(player);
        ItemStack used = event.getItem();
        FoodProperties food = used.getFoodProperties(player);
        if (food != null) {
            state.addFood(food.nutrition() * 6.0D);
            if (ArkOptionalStatController.isActive(ArkStat.WATER)) {
                state.addWater(Math.min(6.0D, food.nutrition() * 0.75D));
            }
        }
        if (ArkOptionalStatController.isActive(ArkStat.WATER)) {
            double waterRestore = ArkConfig.waterRestoreFor(used);
            if (waterRestore > 0.0D) {
                state.addWater(waterRestore);
            }
        }
    }

    private static void onDrown(LivingDrownEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        PlayerArkState state = ArkAttachments.get(player);
        if (state.getOxygen() > 0.0D) {
            event.setCanceled(true);
        }
    }

    private static void onClone(PlayerEvent.Clone event) {
        PlayerArkState oldState = ArkAttachments.get(event.getOriginal());
        PlayerArkState newState = ArkAttachments.get(event.getEntity());
        newState.copyPersistentFrom(oldState, !event.isWasDeath());
        if (event.getEntity() instanceof ServerPlayer player) {
            applyDerivedState(player, newState);
            ArkAttachments.sync(player);
        }
    }

    private static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PlayerArkState state = ArkAttachments.get(player);
            applyDerivedState(player, state);
            ArkAttachments.sync(player);
        }
    }

    private static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PlayerArkState state = ArkAttachments.get(player);
            applyDerivedState(player, state);
            ArkAttachments.sync(player);
        }
    }

    private static void onDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        PlayerArkState state = ArkAttachments.get(player);
        boolean any = false;
        for (BackpackSlot slot : state.backpackSlots()) {
            if (!slot.isEmpty() && slot.entry().storedCount() > 0) {
                any = true;
                break;
            }
        }
        if (!any) {
            return;
        }
        for (BackpackSlot slot : state.backpackSlots()) {
            if (slot.isEmpty()) {
                continue;
            }
            BackpackEntry entry = slot.entry();
            int remaining = entry.storedCount();
            while (remaining > 0) {
                int chunk = Math.min(Math.max(1, entry.template().getMaxStackSize()), remaining);
                ItemEntity drop = new ItemEntity(player.level(), player.getX(), player.getY(), player.getZ(), entry.template().copyWithCount(chunk));
                event.getDrops().add(drop);
                remaining -= chunk;
            }
        }
        state.clearBackpack();
    }

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.level().isClientSide) return;
        PlayerArkState state = ArkAttachments.get(player);
        siphonMainInventory(player, state);
        boolean hotbarChanged = syncHotbar(player, state);
        tickResources(player, state);
        applyDerivedState(player, state);
        if (hotbarChanged) {
            player.containerMenu.broadcastChanges();
        }
        if (state.isDirty() && player.tickCount % ArkConfig.syncIntervalTicks() == 0) {
            ArkAttachments.sync(player);
        }
    }

    private static void siphonMainInventory(ServerPlayer player, PlayerArkState state) {
        Inventory inventory = player.getInventory();
        boolean movedAny = false;
        for (int slot = 9; slot < 36; slot++) {
            ItemStack stack = inventory.items.get(slot);
            if (!stack.isEmpty()) {
                state.storeStack(stack.copy());
                inventory.items.set(slot, ItemStack.EMPTY);
                movedAny = true;
            }
        }
        if (movedAny) {
            inventory.setChanged();
            player.containerMenu.broadcastChanges();
        }
    }

    private static boolean syncHotbar(ServerPlayer player, PlayerArkState state) {
        Inventory inventory = player.getInventory();
        ItemStack carried = player.containerMenu.getCarried();
        boolean changed = false;
        for (int slot = 0; slot < 9; slot++) {
            HotbarBinding binding = state.getHotbarBinding(slot);
            ItemStack liveStack = inventory.items.get(slot);
            if (binding == null) {
                continue;
            }
            if (!binding.isBound()) {
                if (!liveStack.isEmpty()) {
                    adoptHotbarStack(state, inventory, slot, liveStack.copy());
                    changed = true;
                }
                continue;
            }

            BackpackEntry entry = state.getEntry(binding.entryId());
            if (entry == null) {
                if (!liveStack.isEmpty()) {
                    adoptHotbarStack(state, inventory, slot, liveStack.copy());
                } else {
                    state.clearHotbarBinding(slot);
                }
                changed = true;
                continue;
            }

            if (!liveStack.isEmpty() && !entry.sameItem(liveStack)) {
                adoptHotbarStack(state, inventory, slot, liveStack.copy());
                changed = true;
                continue;
            }

            changed |= reconcileHotbarProjection(state, inventory, slot, entry, binding, liveStack, carried);
        }
        if (changed) {
            inventory.setChanged();
        }
        return changed;
    }

    private static void adoptHotbarStack(PlayerArkState state, Inventory inventory, int slot, ItemStack stack) {
        if (stack.isEmpty()) {
            state.clearHotbarBinding(slot);
            inventory.items.set(slot, ItemStack.EMPTY);
            return;
        }
        PlayerArkState.ItemStackResult result = state.storeStack(stack);
        inventory.items.set(slot, ItemStack.EMPTY);
        if (result.targetEntry() != null) {
            state.bindHotbar(slot, result.targetEntry().entryId());
            refillBindingSlot(inventory, slot, result.targetEntry(), state.getHotbarBinding(slot));
            state.markDirty();
        } else {
            state.clearHotbarBinding(slot);
        }
    }

    private static boolean reconcileHotbarProjection(PlayerArkState state, Inventory inventory, int slotIndex, BackpackEntry entry, HotbarBinding binding, ItemStack liveStack, ItemStack carried) {
        boolean changed = false;
        if (!liveStack.isEmpty()) {
            if (!entry.matches(liveStack)) {
                entry.setTemplate(liveStack);
                state.markDirty();
                changed = true;
            }
            if (binding.reservedCount() != liveStack.getCount()) {
                binding.setReservedCount(liveStack.getCount());
                state.markDirty();
                changed = true;
            }
            return changed;
        }

        if (!carried.isEmpty() && entry.matches(carried)) {
            if (binding.reservedCount() != carried.getCount()) {
                binding.setReservedCount(carried.getCount());
                state.markDirty();
                changed = true;
            }
            return changed;
        }

        if (binding.reservedCount() > 0) {
            binding.setReservedCount(0);
            state.markDirty();
            changed = true;
        }

        if (entry.storedCount() <= 0) {
            if (state.reservedCountFor(entry.entryId()) <= 0) {
                state.removeEntry(entry.entryId());
                inventory.items.set(slotIndex, ItemStack.EMPTY);
                changed = true;
            }
            return changed;
        }

        changed |= refillBindingSlot(inventory, slotIndex, entry, binding);
        return changed;
    }

    private static boolean refillBindingSlot(Inventory inventory, int slotIndex, BackpackEntry entry, HotbarBinding binding) {
        if (binding == null || entry == null) {
            return false;
        }
        int refill = Math.min(Math.max(1, entry.template().getMaxStackSize()), entry.storedCount());
        if (refill <= 0) {
            return false;
        }
        ItemStack reserved = entry.extract(refill);
        inventory.items.set(slotIndex, reserved);
        binding.setReservedCount(reserved.getCount());
        return true;
    }

    private static void tickResources(ServerPlayer player, PlayerArkState state) {
        boolean waterActive = ArkOptionalStatController.isActive(ArkStat.WATER);
        if (player.isCreative() || player.isSpectator()) {
            state.addStamina(state.maxFor(ArkStat.STAMINA));
            state.addOxygen(state.maxFor(ArkStat.OXYGEN));
            state.addFood(state.maxFor(ArkStat.FOOD));
            if (waterActive) {
                state.addWater(state.maxFor(ArkStat.WATER));
            }
            FoodData foodData = player.getFoodData();
            foodData.setFoodLevel(20);
            foodData.setSaturation(20.0F);
            foodData.setExhaustion(0.0F);
            return;
        }

        double foodEfficiency = ArkConfig.foodEfficiencyMultiplier(state.getAllocatedPoints(ArkStat.FOOD));
        boolean usingBow = player.isUsingItem() && player.getUseItem().getItem() instanceof BowItem;
        boolean moving = player.getDeltaMovement().horizontalDistanceSqr() > 1.0E-4D;

        state.addFood(-ArkConfig.foodPassiveDrain() / foodEfficiency);
        if (waterActive) {
            state.addWater(-ArkConfig.waterPassiveDrain());
            if (player.isOnFire()) {
                state.addWater(-ArkConfig.waterFireDrain());
            }
        }

        MobEffectInstance hungerEffect = player.getEffect(MobEffects.HUNGER);
        if (hungerEffect != null) {
            state.addFood(-(HUNGER_EFFECT_DRAIN_PER_TICK * (hungerEffect.getAmplifier() + 1)) / foodEfficiency);
        }

        if (player.isSprinting()) {
            state.addStamina(-ArkConfig.staminaSprintDrain());
            state.addFood(-ArkConfig.foodSprintDrain() / foodEfficiency);
            if (waterActive) {
                state.addWater(-ArkConfig.waterSprintDrain());
            }
        } else if (state.getFood() > 0.0D && !moving && !usingBow) {
            double regen = waterActive && state.getWater() <= ArkConfig.waterLowThreshold() ? ArkConfig.staminaIdleRegen() * 0.25D : ArkConfig.staminaIdleRegen();
            state.addStamina(regen);
        } else if (state.getFood() > 0.0D && !usingBow) {
            double regen = waterActive && state.getWater() <= ArkConfig.waterLowThreshold() ? ArkConfig.staminaWalkRegen() * 0.25D : ArkConfig.staminaWalkRegen();
            state.addStamina(regen);
        }

        if (usingBow) {
            state.addStamina(-ArkConfig.staminaBowDrain());
            if (state.getStamina() <= 0.0D) {
                player.releaseUsingItem();
                player.hurt(player.damageSources().generic(), ArkConfig.staminaBowBreakDamage());
            }
        }

        if (state.getStamina() <= 0.0D || state.getFood() <= 0.0D) {
            player.setSprinting(false);
        }

        if (player.isEyeInFluid(FluidTags.WATER)) {
            if (state.getOxygen() > 0.0D) {
                state.addOxygen(-ArkConfig.oxygenUnderwaterDrain());
                player.setAirSupply(player.getMaxAirSupply());
            }
        } else {
            state.addOxygen(ArkConfig.oxygenRegen());
            player.setAirSupply(player.getMaxAirSupply());
        }

        if (player.level().getGameRules().getBoolean(GameRules.RULE_NATURAL_REGENERATION)
                && player.isHurt()
                && state.getFood() >= ArkConfig.foodRegenCost()
                && player.tickCount % 10 == 0) {
            state.addFood(-ArkConfig.foodRegenCost() / foodEfficiency);
            player.heal(ArkConfig.foodRegenHeal());
        }

        if (isOverweight(player, state) && player.isInWater()) {
            double sink = 0.08D + Math.min(0.45D, overweightRatio(player, state) * 0.12D);
            player.setDeltaMovement(player.getDeltaMovement().x, Math.min(player.getDeltaMovement().y, -sink), player.getDeltaMovement().z);
        }

        syncVanillaFoodCompat(player, state, waterActive);
    }

    private static void syncVanillaFoodCompat(ServerPlayer player, PlayerArkState state, boolean waterActive) {
        FoodData foodData = player.getFoodData();
        boolean fullySatisfied = state.getFood() >= state.maxFor(ArkStat.FOOD) - 0.001D
                && (!waterActive || state.getWater() >= state.maxFor(ArkStat.WATER) - 0.001D)
                && !player.isHurt();
        int compatLevel = fullySatisfied ? 20 : 17;
        foodData.setFoodLevel(compatLevel);
        foodData.setSaturation(0.0F);
        foodData.setExhaustion(0.0F);
    }

    public static void handleSpendPoint(ServerPlayer player, ArkStat stat) {
        PlayerArkState state = ArkAttachments.get(player);
        if (state.spendPoint(stat)) {
            applyDerivedState(player, state);
            ArkAttachments.sync(player);
        }
    }

    public static void handleOpenArkInventory(ServerPlayer player) {
        player.openMenu(new SimpleMenuProvider((containerId, inventory, ignored) -> new ArkInventoryMenu(containerId, inventory, player), ArkInventoryMenu.title()));
    }

    public static void handleBackpackGridClick(ServerPlayer player, int slotIndex, int mouseButton) {
        PlayerArkState state = ArkAttachments.get(player);
        AbstractContainerMenu menu = player.containerMenu;
        ItemStack carried = menu.getCarried();

        BackpackSlot slot = state.getSlot(slotIndex);
        BackpackEntry entry = slot == null ? null : slot.entry();
        if (entry != null && state.isEntryBound(entry.entryId())) {
            collapseBoundHotbar(player, state, entry.entryId());
            slot = state.getSlot(slotIndex);
            entry = slot == null ? null : slot.entry();
        }

        if (mouseButton == 0) {
            if (carried.isEmpty()) {
                if (entry == null) {
                    return;
                }
                BackpackEntry removed = state.removeEntryAt(slotIndex);
                if (removed != null) {
                    menu.setCarried(removed.asCarriedStack());
                }
            } else if (entry == null) {
                state.placeStackAt(slotIndex, carried.copy());
                menu.setCarried(ItemStack.EMPTY);
            } else if (entry.matches(carried)) {
                int moved = entry.insert(carried);
                carried.shrink(moved);
                if (carried.isEmpty()) {
                    menu.setCarried(ItemStack.EMPTY);
                }
                state.markDirty();
            } else {
                ItemStack oldStack = entry.asCarriedStack();
                state.setEntryAt(slotIndex, state.createEntry(carried));
                menu.setCarried(oldStack);
            }
        } else if (mouseButton == 1) {
            if (entry == null) {
                return;
            }
            if (carried.isEmpty()) {
                ItemStack taken = entry.extract(Math.min(64, entry.storedCount()));
                if (entry.storedCount() <= 0) {
                    state.removeEntryAt(slotIndex);
                } else {
                    state.markDirty();
                }
                menu.setCarried(taken);
            } else if (entry.matches(carried)) {
                int moved = entry.insert(carried);
                carried.shrink(moved);
                if (carried.isEmpty()) {
                    menu.setCarried(ItemStack.EMPTY);
                }
                state.markDirty();
            }
        }

        applyDerivedState(player, state);
        menu.broadcastChanges();
        ArkAttachments.sync(player);
    }

    public static void handleBackpackScroll(ServerPlayer player, int delta) {
        PlayerArkState state = ArkAttachments.get(player);
        state.setScrollOffset(Math.max(0, state.getScrollOffset() + delta));
        ArkAttachments.sync(player);
    }

    public static void stashCarried(ServerPlayer player) {
        ItemStack carried = player.containerMenu.getCarried();
        if (carried.isEmpty()) return;
        PlayerArkState state = ArkAttachments.get(player);
        state.storeStack(carried.copy());
        player.containerMenu.setCarried(ItemStack.EMPTY);
        player.containerMenu.broadcastChanges();
        ArkAttachments.sync(player);
    }

    public static void dropCarried(ServerPlayer player, boolean single) {
        ItemStack carried = player.containerMenu.getCarried();
        if (carried.isEmpty()) {
            return;
        }
        int amount = single ? 1 : carried.getCount();
        ItemStack dropped = carried.split(amount);
        player.drop(dropped, false);
        if (carried.isEmpty()) {
            player.containerMenu.setCarried(ItemStack.EMPTY);
        }
        player.containerMenu.broadcastChanges();
    }

    public static void dropBackpackSlot(ServerPlayer player, int slotIndex, int amountMode) {
        PlayerArkState state = ArkAttachments.get(player);
        BackpackSlot slot = state.getSlot(slotIndex);
        if (slot == null || slot.isEmpty()) {
            return;
        }
        BackpackEntry entry = slot.entry();
        if (state.isEntryBound(entry.entryId())) {
            collapseBoundHotbar(player, state, entry.entryId());
            slot = state.getSlot(slotIndex);
            entry = slot == null ? null : slot.entry();
        }
        if (entry == null) {
            return;
        }
        int amount = resolveDropAmount(amountMode, entry.storedCount());
        if (amount <= 0) {
            return;
        }
        dropFromTemplate(player, entry.template(), amount);
        entry.extract(amount);
        if (entry.storedCount() <= 0) {
            state.removeEntryAt(slotIndex);
        } else {
            state.markDirty();
        }
        player.containerMenu.broadcastChanges();
        ArkAttachments.sync(player);
    }

    public static void dropHotbarBinding(ServerPlayer player, int slot, int amountMode) {
        if (slot < 0 || slot >= 9) {
            return;
        }
        Inventory inventory = player.getInventory();
        PlayerArkState state = ArkAttachments.get(player);
        HotbarBinding binding = state.getHotbarBinding(slot);
        if (binding == null || !binding.isBound()) {
            return;
        }
        ItemStack live = inventory.items.get(slot);
        if (live.isEmpty()) {
            return;
        }
        int amount = resolveDropAmount(amountMode, live.getCount());
        if (amount <= 0) {
            return;
        }
        ItemStack dropped = live.split(amount);
        player.drop(dropped, false);
        binding.setReservedCount(live.getCount());
        if (live.isEmpty()) {
            inventory.items.set(slot, ItemStack.EMPTY);
        }
        inventory.setChanged();
        player.containerMenu.broadcastChanges();
        ArkAttachments.sync(player);
    }

    public static void quickMoveToBackpack(ServerPlayer player, int slotIndex) {
        AbstractContainerMenu menu = player.containerMenu;
        if (slotIndex < 0 || slotIndex >= menu.slots.size()) {
            return;
        }
        Slot slot = menu.slots.get(slotIndex);
        if (!slot.hasItem() || !slot.mayPickup(player)) {
            return;
        }
        PlayerArkState state = ArkAttachments.get(player);
        int guard = 0;
        while (slot.hasItem() && guard++ < 256) {
            ItemStack current = slot.getItem();
            ItemStack taken = slot.safeTake(current.getCount(), current.getCount(), player);
            if (taken.isEmpty()) {
                break;
            }
            state.storeStack(taken.copy());
            if (slot.container == player.getInventory()) {
                break;
            }
        }
        siphonMainInventory(player, state);
        player.containerMenu.broadcastChanges();
        ArkAttachments.sync(player);
    }

    public static void bindHotbarFromBackpack(ServerPlayer player, int backpackSlotIndex, int hotbarSlot) {
        if (hotbarSlot < 0 || hotbarSlot >= 9) {
            return;
        }
        PlayerArkState state = ArkAttachments.get(player);
        Inventory inventory = player.getInventory();
        AbstractContainerMenu menu = player.containerMenu;
        BackpackSlot sourceSlot = state.getSlot(backpackSlotIndex);
        BackpackEntry sourceEntry = sourceSlot == null || sourceSlot.isEmpty() ? null : sourceSlot.entry();
        ItemStack carried = menu.getCarried();

        if (sourceEntry == null && carried.isEmpty()) {
            return;
        }

        long entryId;
        if (sourceEntry != null) {
            entryId = sourceEntry.entryId();
        } else {
            BackpackEntry restored = state.placeStackAt(backpackSlotIndex, carried.copy());
            if (restored == null) {
                return;
            }
            menu.setCarried(ItemStack.EMPTY);
            entryId = restored.entryId();
        }

        HotbarBinding targetBinding = state.getHotbarBinding(hotbarSlot);
        if (targetBinding != null && targetBinding.isBound() && targetBinding.entryId() != entryId) {
            collapseBoundHotbar(player, state, targetBinding.entryId());
        } else {
            ItemStack live = inventory.items.get(hotbarSlot);
            if (!live.isEmpty()) {
                state.storeStack(live.copy());
                inventory.items.set(hotbarSlot, ItemStack.EMPTY);
                inventory.setChanged();
            }
        }

        int previousSlot = state.boundSlotFor(entryId);
        if (previousSlot >= 0 && previousSlot != hotbarSlot) {
            collapseBoundHotbar(player, state, entryId);
        }

        state.bindHotbar(hotbarSlot, entryId);
        syncHotbar(player, state);
        inventory.setChanged();
        menu.broadcastChanges();
        ArkAttachments.sync(player);
    }

    public static void clearHotbarBinding(ServerPlayer player, int slot) {
        if (slot < 0 || slot >= 9) {
            return;
        }
        PlayerArkState state = ArkAttachments.get(player);
        if (!state.hasHotbarBinding(slot)) {
            return;
        }
        collapseBoundHotbar(player, state, state.getHotbarBinding(slot).entryId());
        state.clearHotbarBinding(slot);
        player.getInventory().items.set(slot, ItemStack.EMPTY);
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
        ArkAttachments.sync(player);
    }

    private static void collapseBoundHotbar(ServerPlayer player, PlayerArkState state, long entryId) {
        int slot = state.boundSlotFor(entryId);
        if (slot < 0) {
            return;
        }
        BackpackEntry entry = state.getEntry(entryId);
        if (entry == null) {
            state.clearHotbarBinding(slot);
            return;
        }
        ItemStack live = player.getInventory().items.get(slot);
        if (!live.isEmpty() && entry.sameItem(live)) {
            entry.insert(live.copy());
        }
        player.getInventory().items.set(slot, ItemStack.EMPTY);
        state.clearHotbarBinding(slot);
        player.getInventory().setChanged();
        state.markDirty();
    }

    private static int resolveDropAmount(int amountMode, int available) {
        if (available <= 0) {
            return 0;
        }
        if (amountMode == DROP_ALL) {
            return available;
        }
        return Math.min(Math.max(1, amountMode), available);
    }

    private static void dropFromTemplate(ServerPlayer player, ItemStack template, int amount) {
        int remaining = amount;
        while (remaining > 0) {
            int chunk = Math.min(Math.max(1, template.getMaxStackSize()), remaining);
            player.drop(template.copyWithCount(chunk), false);
            remaining -= chunk;
        }
    }

    public static void applyDerivedState(ServerPlayer player, PlayerArkState state) {
        boolean waterActive = ArkOptionalStatController.isActive(ArkStat.WATER);
        double healthBonus = state.getAllocatedPoints(ArkStat.HEALTH) * ArkConfig.healthPerPoint();
        double meleeBonus = state.getAllocatedPoints(ArkStat.MELEE_DAMAGE) * ArkConfig.meleePerPoint();
        double speedBonus = state.getAllocatedPoints(ArkStat.MOVE_SPEED) * ArkConfig.speedPerPoint();
        double weightPenalty = isOverweight(player, state) ? -Math.min(0.95D, 1.0D - Math.exp(-overweightRatio(player, state) * 1.75D)) : 0.0D;
        double thirstPenalty = waterActive && state.getWater() <= ArkConfig.waterLowThreshold() ? -0.25D : 0.0D;
        double stepBonus = state.getAllocatedPoints(ArkStat.MOVE_SPEED) >= ArkConfig.stepThreshold() ? 0.45D : 0.0D;

        setModifier(player.getAttribute(Attributes.MAX_HEALTH), HEALTH_MODIFIER_ID, healthBonus, AttributeModifier.Operation.ADD_VALUE);
        setModifier(player.getAttribute(Attributes.ATTACK_DAMAGE), MELEE_MODIFIER_ID, meleeBonus, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        setModifier(player.getAttribute(Attributes.MOVEMENT_SPEED), SPEED_MODIFIER_ID, speedBonus, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        setModifier(player.getAttribute(Attributes.MOVEMENT_SPEED), WEIGHT_MODIFIER_ID, weightPenalty, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        setModifier(player.getAttribute(Attributes.MOVEMENT_SPEED), THIRST_MODIFIER_ID, thirstPenalty, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        setModifier(player.getAttribute(Attributes.STEP_HEIGHT), STEP_MODIFIER_ID, stepBonus, AttributeModifier.Operation.ADD_VALUE);

        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    private static void setModifier(AttributeInstance instance, ResourceLocation id, double amount, AttributeModifier.Operation operation) {
        if (instance == null) return;
        instance.removeModifier(id);
        if (Math.abs(amount) > 1.0E-6D) {
            instance.addOrUpdateTransientModifier(new AttributeModifier(id, amount, operation));
        }
    }

    public static double calculateTotalWeight(Player player, PlayerArkState state) {
        double total = 0.0D;
        Inventory inventory = player.getInventory();
        for (ItemStack hotbar : inventory.items.subList(0, 9)) {
            total += weightOf(hotbar);
        }
        for (ItemStack armor : inventory.armor) total += weightOf(armor);
        for (ItemStack offhand : inventory.offhand) total += weightOf(offhand);
        for (BackpackSlot slot : state.backpackSlots()) {
            if (!slot.isEmpty()) {
                total += ArkConfig.weightFor(slot.entry().template()) * slot.entry().storedCount();
            }
        }
        return total;
    }

    public static double weightCapacity(PlayerArkState state) {
        return Math.max(1.0D, state.maxFor(ArkStat.WEIGHT));
    }

    private static boolean isOverweight(Player player, PlayerArkState state) {
        return calculateTotalWeight(player, state) > weightCapacity(state);
    }

    private static double overweightRatio(Player player, PlayerArkState state) {
        return Math.max(0.0D, calculateTotalWeight(player, state) / weightCapacity(state) - 1.0D);
    }

    private static double weightOf(ItemStack stack) {
        return stack.isEmpty() ? 0.0D : ArkConfig.weightFor(stack) * stack.getCount();
    }
}
