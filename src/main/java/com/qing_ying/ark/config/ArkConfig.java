package com.qing_ying.ark.config;

import com.qing_ying.ark.Ark;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class ArkConfig {
    public static final ModConfigSpec SPEC;

    private static final ModConfigSpec.IntValue LEVEL_BASE_XP;
    private static final ModConfigSpec.IntValue LEVEL_STEP_XP;
    private static final ModConfigSpec.IntValue SYNC_INTERVAL_TICKS;

    private static final ModConfigSpec.DoubleValue HEALTH_BASE;
    private static final ModConfigSpec.DoubleValue HEALTH_PER_POINT;
    private static final ModConfigSpec.DoubleValue STAMINA_BASE;
    private static final ModConfigSpec.DoubleValue STAMINA_PER_POINT;
    private static final ModConfigSpec.DoubleValue OXYGEN_BASE;
    private static final ModConfigSpec.DoubleValue OXYGEN_PER_POINT;
    private static final ModConfigSpec.DoubleValue FOOD_BASE;
    private static final ModConfigSpec.DoubleValue FOOD_PER_POINT;
    private static final ModConfigSpec.DoubleValue WATER_BASE;
    private static final ModConfigSpec.DoubleValue WATER_PER_POINT;
    private static final ModConfigSpec.DoubleValue WEIGHT_BASE;
    private static final ModConfigSpec.DoubleValue WEIGHT_PER_POINT;
    private static final ModConfigSpec.DoubleValue MELEE_PER_POINT;
    private static final ModConfigSpec.DoubleValue SPEED_PER_POINT;
    private static final ModConfigSpec.IntValue STEP_THRESHOLD;

    private static final ModConfigSpec.DoubleValue STAMINA_SPRINT_DRAIN;
    private static final ModConfigSpec.DoubleValue STAMINA_BOW_DRAIN;
    private static final ModConfigSpec.DoubleValue STAMINA_IDLE_REGEN;
    private static final ModConfigSpec.DoubleValue STAMINA_WALK_REGEN;
    private static final ModConfigSpec.DoubleValue STAMINA_JUMP_DRAIN;
    private static final ModConfigSpec.DoubleValue STAMINA_BOW_BREAK_DAMAGE;

    private static final ModConfigSpec.DoubleValue OXYGEN_UNDERWATER_DRAIN;
    private static final ModConfigSpec.DoubleValue OXYGEN_REGEN;

    private static final ModConfigSpec.DoubleValue FOOD_PASSIVE_DRAIN;
    private static final ModConfigSpec.DoubleValue FOOD_SPRINT_DRAIN;
    private static final ModConfigSpec.DoubleValue FOOD_JUMP_DRAIN;
    private static final ModConfigSpec.DoubleValue FOOD_EFFICIENCY_PER_POINT;
    private static final ModConfigSpec.DoubleValue FOOD_REGEN_COST;
    private static final ModConfigSpec.DoubleValue FOOD_REGEN_HEAL;

    private static final ModConfigSpec.DoubleValue WATER_PASSIVE_DRAIN;
    private static final ModConfigSpec.DoubleValue WATER_SPRINT_DRAIN;
    private static final ModConfigSpec.DoubleValue WATER_JUMP_DRAIN;
    private static final ModConfigSpec.DoubleValue WATER_FIRE_DRAIN;
    private static final ModConfigSpec.DoubleValue WATER_BOTTLE_RESTORE;
    private static final ModConfigSpec.DoubleValue WATER_SOURCE_RESTORE;
    private static final ModConfigSpec.DoubleValue WATER_LOW_THRESHOLD;

    private static final ModConfigSpec.DoubleValue DEFAULT_ITEM_WEIGHT;
    private static final ModConfigSpec.DoubleValue LIGHT_ITEM_WEIGHT;
    private static final ModConfigSpec.DoubleValue MEDIUM_ITEM_WEIGHT;
    private static final ModConfigSpec.DoubleValue HEAVY_ITEM_WEIGHT;
    private static final ModConfigSpec.ConfigValue<List<? extends String>> ITEM_WEIGHT_OVERRIDES;

    public static final TagKey<Item> LIGHT_ITEMS = ItemTags.create(ResourceLocation.fromNamespaceAndPath(Ark.MOD_ID, "light_items"));
    public static final TagKey<Item> MEDIUM_ITEMS = ItemTags.create(ResourceLocation.fromNamespaceAndPath(Ark.MOD_ID, "medium_items"));
    public static final TagKey<Item> HEAVY_ITEMS = ItemTags.create(ResourceLocation.fromNamespaceAndPath(Ark.MOD_ID, "heavy_items"));

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("progression");
        LEVEL_BASE_XP = builder.defineInRange("levelBaseXp", 30, 1, Integer.MAX_VALUE);
        LEVEL_STEP_XP = builder.defineInRange("levelStepXp", 15, 0, Integer.MAX_VALUE);
        SYNC_INTERVAL_TICKS = builder.defineInRange("syncIntervalTicks", 5, 1, 100);
        builder.pop();

        builder.push("stats");
        HEALTH_BASE = builder.defineInRange("healthBase", 20.0D, 1.0D, 1024.0D);
        HEALTH_PER_POINT = builder.defineInRange("healthPerPoint", 4.0D, 0.0D, 256.0D);
        STAMINA_BASE = builder.defineInRange("staminaBase", 100.0D, 1.0D, 4096.0D);
        STAMINA_PER_POINT = builder.defineInRange("staminaPerPoint", 15.0D, 0.0D, 512.0D);
        OXYGEN_BASE = builder.defineInRange("oxygenBase", 100.0D, 1.0D, 4096.0D);
        OXYGEN_PER_POINT = builder.defineInRange("oxygenPerPoint", 20.0D, 0.0D, 512.0D);
        FOOD_BASE = builder.defineInRange("foodBase", 100.0D, 1.0D, 4096.0D);
        FOOD_PER_POINT = builder.defineInRange("foodPerPoint", 15.0D, 0.0D, 512.0D);
        WATER_BASE = builder.defineInRange("waterBase", 100.0D, 1.0D, 4096.0D);
        WATER_PER_POINT = builder.defineInRange("waterPerPoint", 15.0D, 0.0D, 512.0D);
        WEIGHT_BASE = builder.defineInRange("weightBase", 200.0D, 1.0D, 100000.0D);
        WEIGHT_PER_POINT = builder.defineInRange("weightPerPoint", 25.0D, 0.0D, 4096.0D);
        MELEE_PER_POINT = builder.defineInRange("meleePerPoint", 0.08D, 0.0D, 10.0D);
        SPEED_PER_POINT = builder.defineInRange("speedPerPoint", 0.025D, 0.0D, 10.0D);
        STEP_THRESHOLD = builder.defineInRange("stepThreshold", 5, 0, 1024);
        builder.pop();

        builder.push("stamina");
        STAMINA_SPRINT_DRAIN = builder.defineInRange("sprintDrainPerTick", 0.18D, 0.0D, 128.0D);
        STAMINA_BOW_DRAIN = builder.defineInRange("bowDrainPerTick", 0.75D, 0.0D, 128.0D);
        STAMINA_IDLE_REGEN = builder.defineInRange("idleRegenPerTick", 1.75D, 0.0D, 128.0D);
        STAMINA_WALK_REGEN = builder.defineInRange("walkRegenPerTick", 0.4D, 0.0D, 128.0D);
        STAMINA_JUMP_DRAIN = builder.defineInRange("jumpDrain", 8.0D, 0.0D, 128.0D);
        STAMINA_BOW_BREAK_DAMAGE = builder.defineInRange("bowBreakDamage", 2.0D, 0.0D, 128.0D);
        builder.pop();

        builder.push("oxygen");
        OXYGEN_UNDERWATER_DRAIN = builder.defineInRange("underwaterDrainPerTick", 0.6D, 0.0D, 128.0D);
        OXYGEN_REGEN = builder.defineInRange("regenPerTick", 1.5D, 0.0D, 128.0D);
        builder.pop();

        builder.push("food");
        FOOD_PASSIVE_DRAIN = builder.defineInRange("passiveDrainPerTick", 0.002D, 0.0D, 16.0D);
        FOOD_SPRINT_DRAIN = builder.defineInRange("sprintDrainPerTick", 0.035D, 0.0D, 16.0D);
        FOOD_JUMP_DRAIN = builder.defineInRange("jumpDrain", 1.5D, 0.0D, 64.0D);
        FOOD_EFFICIENCY_PER_POINT = builder.defineInRange("efficiencyPerPoint", 0.1D, 0.0D, 4.0D);
        FOOD_REGEN_COST = builder.defineInRange("regenCost", 2.0D, 0.0D, 64.0D);
        FOOD_REGEN_HEAL = builder.defineInRange("regenHeal", 1.0D, 0.0D, 64.0D);
        builder.pop();

        builder.push("water");
        WATER_PASSIVE_DRAIN = builder.defineInRange("passiveDrainPerTick", 0.003D, 0.0D, 16.0D);
        WATER_SPRINT_DRAIN = builder.defineInRange("sprintDrainPerTick", 0.045D, 0.0D, 16.0D);
        WATER_JUMP_DRAIN = builder.defineInRange("jumpDrain", 0.4D, 0.0D, 64.0D);
        WATER_FIRE_DRAIN = builder.defineInRange("fireDrainPerTick", 0.08D, 0.0D, 64.0D);
        WATER_BOTTLE_RESTORE = builder.defineInRange("waterBottleRestore", 25.0D, 0.0D, 512.0D);
        WATER_SOURCE_RESTORE = builder.defineInRange("waterSourceRestore", 18.0D, 0.0D, 512.0D);
        WATER_LOW_THRESHOLD = builder.defineInRange("lowThreshold", 15.0D, 0.0D, 4096.0D);
        builder.pop();

        builder.push("weight");
        DEFAULT_ITEM_WEIGHT = builder.defineInRange("defaultItemWeight", 1.0D, 0.0D, 4096.0D);
        LIGHT_ITEM_WEIGHT = builder.defineInRange("lightItemWeight", 0.5D, 0.0D, 4096.0D);
        MEDIUM_ITEM_WEIGHT = builder.defineInRange("mediumItemWeight", 2.0D, 0.0D, 4096.0D);
        HEAVY_ITEM_WEIGHT = builder.defineInRange("heavyItemWeight", 5.0D, 0.0D, 4096.0D);
        ITEM_WEIGHT_OVERRIDES = builder.defineListAllowEmpty(List.of("itemWeightOverrides"), List.of("minecraft:shulker_box=12.0"), value -> value instanceof String string && string.contains("="));
        builder.pop();
        SPEC = builder.build();
    }

    private ArkConfig() {
    }

    public static long xpForNextLevel(int currentLevel) { return LEVEL_BASE_XP.get() + (long) Math.max(0, currentLevel - 1) * LEVEL_STEP_XP.get(); }
    public static int syncIntervalTicks() { return SYNC_INTERVAL_TICKS.get(); }
    public static double healthBase() { return HEALTH_BASE.get(); }
    public static double healthPerPoint() { return HEALTH_PER_POINT.get(); }
    public static double staminaBase() { return STAMINA_BASE.get(); }
    public static double staminaPerPoint() { return STAMINA_PER_POINT.get(); }
    public static double oxygenBase() { return OXYGEN_BASE.get(); }
    public static double oxygenPerPoint() { return OXYGEN_PER_POINT.get(); }
    public static double foodBase() { return FOOD_BASE.get(); }
    public static double foodPerPoint() { return FOOD_PER_POINT.get(); }
    public static double waterBase() { return WATER_BASE.get(); }
    public static double waterPerPoint() { return WATER_PER_POINT.get(); }
    public static double weightBase() { return WEIGHT_BASE.get(); }
    public static double weightPerPoint() { return WEIGHT_PER_POINT.get(); }
    public static double meleePerPoint() { return MELEE_PER_POINT.get(); }
    public static double speedPerPoint() { return SPEED_PER_POINT.get(); }
    public static int stepThreshold() { return STEP_THRESHOLD.get(); }
    public static double staminaSprintDrain() { return STAMINA_SPRINT_DRAIN.get(); }
    public static double staminaBowDrain() { return STAMINA_BOW_DRAIN.get(); }
    public static double staminaIdleRegen() { return STAMINA_IDLE_REGEN.get(); }
    public static double staminaWalkRegen() { return STAMINA_WALK_REGEN.get(); }
    public static double staminaJumpDrain() { return STAMINA_JUMP_DRAIN.get(); }
    public static float staminaBowBreakDamage() { return STAMINA_BOW_BREAK_DAMAGE.get().floatValue(); }
    public static double oxygenUnderwaterDrain() { return OXYGEN_UNDERWATER_DRAIN.get(); }
    public static double oxygenRegen() { return OXYGEN_REGEN.get(); }
    public static double foodPassiveDrain() { return FOOD_PASSIVE_DRAIN.get(); }
    public static double foodSprintDrain() { return FOOD_SPRINT_DRAIN.get(); }
    public static double foodJumpDrain() { return FOOD_JUMP_DRAIN.get(); }
    public static double foodEfficiencyPerPoint() { return FOOD_EFFICIENCY_PER_POINT.get(); }
    public static double foodRegenCost() { return FOOD_REGEN_COST.get(); }
    public static float foodRegenHeal() { return FOOD_REGEN_HEAL.get().floatValue(); }
    public static double waterPassiveDrain() { return WATER_PASSIVE_DRAIN.get(); }
    public static double waterSprintDrain() { return WATER_SPRINT_DRAIN.get(); }
    public static double waterJumpDrain() { return WATER_JUMP_DRAIN.get(); }
    public static double waterFireDrain() { return WATER_FIRE_DRAIN.get(); }
    public static double waterBottleRestore() { return WATER_BOTTLE_RESTORE.get(); }
    public static double waterSourceRestore() { return WATER_SOURCE_RESTORE.get(); }
    public static double waterLowThreshold() { return WATER_LOW_THRESHOLD.get(); }

    public static double foodEfficiencyMultiplier(int foodPoints) {
        return 1.0D + Math.max(0, foodPoints) * foodEfficiencyPerPoint();
    }

    public static double weightFor(ItemStack stack) {
        if (stack.isEmpty()) return 0.0D;
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        String key = itemId + "=";
        for (String entry : ITEM_WEIGHT_OVERRIDES.get()) {
            if (entry.startsWith(key)) {
                try { return Double.parseDouble(entry.substring(key.length())); } catch (NumberFormatException ignored) { }
            }
        }
        if (stack.is(HEAVY_ITEMS)) return HEAVY_ITEM_WEIGHT.get();
        if (stack.is(MEDIUM_ITEMS)) return MEDIUM_ITEM_WEIGHT.get();
        if (stack.is(LIGHT_ITEMS)) return LIGHT_ITEM_WEIGHT.get();
        return DEFAULT_ITEM_WEIGHT.get() * (64.0D / Math.max(1, stack.getMaxStackSize()));
    }

    public static double waterRestoreFor(ItemStack stack) {
        if (stack.is(Items.HONEY_BOTTLE)) return WATER_BOTTLE_RESTORE.get() * 0.6D;
        if (stack.is(Items.POTION)) {
            PotionContents contents = stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
            if (contents.potion().isPresent() && contents.potion().get().is(Potions.WATER)) return WATER_BOTTLE_RESTORE.get();
        }
        return 0.0D;
    }
}