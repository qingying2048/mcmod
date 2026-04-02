package com.qing_ying.ark.menu;

import com.mojang.datafixers.util.Pair;
import com.qing_ying.ark.ui.ArkUiLayout;
import java.util.Map;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.inventory.RecipeBookType;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;

public final class ArkInventoryMenu extends RecipeBookMenu<CraftingInput, CraftingRecipe> {
    public static final int CONTAINER_ID = 0;
    public static final int RESULT_SLOT = 0;
    public static final int CRAFT_SLOT_START = 1;
    public static final int CRAFT_SLOT_COUNT = 4;
    public static final int CRAFT_SLOT_END = 5;
    public static final int ARMOR_SLOT_START = 5;
    public static final int ARMOR_SLOT_COUNT = 4;
    public static final int ARMOR_SLOT_END = 9;
    public static final int INV_SLOT_START = 9;
    public static final int INV_SLOT_END = 36;
    public static final int USE_ROW_SLOT_START = 36;
    public static final int USE_ROW_SLOT_END = 45;
    public static final int SHIELD_SLOT = 45;

    public static final int SCREEN_WIDTH = ArkUiLayout.FRAME_WIDTH;
    public static final int SCREEN_HEIGHT = ArkUiLayout.FRAME_HEIGHT;
    public static final int HOTBAR_X = ArkUiLayout.HOTBAR_X;
    public static final int HOTBAR_Y = ArkUiLayout.HOTBAR_Y;
    public static final int ARMOR_X = ArkUiLayout.ARMOR_X;
    public static final int ARMOR_Y = ArkUiLayout.ARMOR_Y;
    public static final int OFFHAND_X = ArkUiLayout.OFFHAND_X;
    public static final int OFFHAND_Y = ArkUiLayout.OFFHAND_Y;
    public static final int CRAFT_X = ArkUiLayout.CRAFT_X;
    public static final int CRAFT_Y = ArkUiLayout.CRAFT_Y;
    public static final int RESULT_X = ArkUiLayout.RESULT_X;
    public static final int RESULT_Y = ArkUiLayout.RESULT_Y;
    private static final int OFFSCREEN_X = -2000;
    private static final int OFFSCREEN_Y = -2000;

    public static final ResourceLocation BLOCK_ATLAS = ResourceLocation.withDefaultNamespace("textures/atlas/blocks.png");
    public static final ResourceLocation EMPTY_ARMOR_SLOT_HELMET = ResourceLocation.withDefaultNamespace("item/empty_armor_slot_helmet");
    public static final ResourceLocation EMPTY_ARMOR_SLOT_CHESTPLATE = ResourceLocation.withDefaultNamespace("item/empty_armor_slot_chestplate");
    public static final ResourceLocation EMPTY_ARMOR_SLOT_LEGGINGS = ResourceLocation.withDefaultNamespace("item/empty_armor_slot_leggings");
    public static final ResourceLocation EMPTY_ARMOR_SLOT_BOOTS = ResourceLocation.withDefaultNamespace("item/empty_armor_slot_boots");
    public static final ResourceLocation EMPTY_ARMOR_SLOT_SHIELD = ResourceLocation.withDefaultNamespace("item/empty_armor_slot_shield");
    private static final Map<EquipmentSlot, ResourceLocation> TEXTURE_EMPTY_SLOTS = Map.of(
            EquipmentSlot.FEET, EMPTY_ARMOR_SLOT_BOOTS,
            EquipmentSlot.LEGS, EMPTY_ARMOR_SLOT_LEGGINGS,
            EquipmentSlot.CHEST, EMPTY_ARMOR_SLOT_CHESTPLATE,
            EquipmentSlot.HEAD, EMPTY_ARMOR_SLOT_HELMET
    );
    private static final EquipmentSlot[] SLOT_IDS = new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private final CraftingContainer craftSlots = new TransientCraftingContainer(this, 2, 2);
    private final ResultContainer resultSlots = new ResultContainer();
    public final boolean active;
    private final Player owner;

    public ArkInventoryMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, inventory.player);
    }

    public ArkInventoryMenu(int containerId, Inventory inventory, Player owner) {
        super(ArkMenus.ARK_INVENTORY.get(), containerId);
        this.active = true;
        this.owner = owner;
        this.addSlot(new ResultSlot(inventory.player, this.craftSlots, this.resultSlots, 0, RESULT_X, RESULT_Y));

        for (int row = 0; row < 2; row++) {
            for (int column = 0; column < 2; column++) {
                this.addSlot(new Slot(this.craftSlots, column + row * 2, CRAFT_X + column * 18, CRAFT_Y + row * 18));
            }
        }

        for (int armorIndex = 0; armorIndex < 4; armorIndex++) {
            EquipmentSlot equipmentSlot = SLOT_IDS[armorIndex];
            ResourceLocation emptySlotTexture = TEXTURE_EMPTY_SLOTS.get(equipmentSlot);
            this.addSlot(new ArkArmorSlot(inventory, owner, equipmentSlot, 39 - armorIndex, ARMOR_X, ARMOR_Y + armorIndex * 18, emptySlotTexture));
        }

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                this.addSlot(new Slot(inventory, column + (row + 1) * 9, OFFSCREEN_X + column * 18, OFFSCREEN_Y + row * 18));
            }
        }

        for (int slot = 0; slot < 9; slot++) {
            this.addSlot(new Slot(inventory, slot, HOTBAR_X + slot * 18, HOTBAR_Y));
        }

        this.addSlot(new Slot(inventory, 40, OFFHAND_X, OFFHAND_Y) {
            @Override
            public void setByPlayer(ItemStack newStack, ItemStack oldStack) {
                owner.onEquipItem(EquipmentSlot.OFFHAND, oldStack, newStack);
                super.setByPlayer(newStack, oldStack);
            }

            @Override
            public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
                return Pair.of(ArkInventoryMenu.BLOCK_ATLAS, ArkInventoryMenu.EMPTY_ARMOR_SLOT_SHIELD);
            }
        });
    }

    public static Component title() {
        return Component.translatable("screen.ark.inventory");
    }

    public static boolean isHotbarSlot(int slot) {
        return slot >= USE_ROW_SLOT_START && slot < USE_ROW_SLOT_END || slot == SHIELD_SLOT;
    }

    @Override
    public void fillCraftSlotsStackedContents(StackedContents stackedContents) {
        this.craftSlots.fillStackedContents(stackedContents);
    }

    @Override
    public void clearCraftingContent() {
        this.resultSlots.clearContent();
        this.craftSlots.clearContent();
    }

    @Override
    public boolean recipeMatches(RecipeHolder<CraftingRecipe> recipe) {
        return recipe.value().matches(this.craftSlots.asCraftInput(), this.owner.level());
    }

    @Override
    public void slotsChanged(Container container) {
        if (!this.owner.level().isClientSide) {
            CraftingInput craftingInput = this.craftSlots.asCraftInput();
            ServerPlayer serverPlayer = (ServerPlayer) this.owner;
            ItemStack result = ItemStack.EMPTY;
            Optional<RecipeHolder<CraftingRecipe>> optional = this.owner.level()
                    .getServer()
                    .getRecipeManager()
                    .getRecipeFor(RecipeType.CRAFTING, craftingInput, this.owner.level(), (RecipeHolder<CraftingRecipe>) null);
            if (optional.isPresent()) {
                RecipeHolder<CraftingRecipe> recipeHolder = optional.get();
                CraftingRecipe craftingRecipe = recipeHolder.value();
                if (this.resultSlots.setRecipeUsed(this.owner.level(), serverPlayer, recipeHolder)) {
                    ItemStack assembled = craftingRecipe.assemble(craftingInput, this.owner.level().registryAccess());
                    if (assembled.isItemEnabled(this.owner.level().enabledFeatures())) {
                        result = assembled;
                    }
                }
            }
            this.resultSlots.setItem(0, result);
            this.setRemoteSlot(0, result);
            serverPlayer.connection.send(new ClientboundContainerSetSlotPacket(this.containerId, this.incrementStateId(), 0, result));
        }
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.resultSlots.clearContent();
        if (!player.level().isClientSide) {
            this.clearContainer(player, this.craftSlots);
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);
        if (slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            itemstack = slotStack.copy();
            EquipmentSlot equipmentSlot = player.getEquipmentSlotForItem(itemstack);
            if (slotIndex == RESULT_SLOT) {
                if (!this.moveItemStackTo(slotStack, INV_SLOT_START, INV_SLOT_END, false)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(slotStack, itemstack);
            } else if (slotIndex >= CRAFT_SLOT_START && slotIndex < CRAFT_SLOT_END) {
                if (!this.moveItemStackTo(slotStack, INV_SLOT_START, INV_SLOT_END, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (slotIndex >= ARMOR_SLOT_START && slotIndex < ARMOR_SLOT_END) {
                if (!this.moveItemStackTo(slotStack, INV_SLOT_START, INV_SLOT_END, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (equipmentSlot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR && !this.slots.get(8 - equipmentSlot.getIndex()).hasItem()) {
                int armorSlot = 8 - equipmentSlot.getIndex();
                if (!this.moveItemStackTo(slotStack, armorSlot, armorSlot + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (equipmentSlot == EquipmentSlot.OFFHAND && !this.slots.get(SHIELD_SLOT).hasItem()) {
                if (!this.moveItemStackTo(slotStack, SHIELD_SLOT, SHIELD_SLOT + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (slotIndex >= INV_SLOT_START && slotIndex < INV_SLOT_END) {
                if (!this.moveItemStackTo(slotStack, USE_ROW_SLOT_START, USE_ROW_SLOT_END, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (slotIndex >= USE_ROW_SLOT_START && slotIndex < USE_ROW_SLOT_END) {
                if (!this.moveItemStackTo(slotStack, INV_SLOT_START, INV_SLOT_END, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(slotStack, INV_SLOT_START, USE_ROW_SLOT_END, false)) {
                return ItemStack.EMPTY;
            }

            if (slotStack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY, itemstack);
            } else {
                slot.setChanged();
            }

            if (slotStack.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, slotStack);
            if (slotIndex == RESULT_SLOT) {
                player.drop(slotStack, false);
            }
        }

        return itemstack;
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.container != this.resultSlots && super.canTakeItemForPickAll(stack, slot);
    }

    @Override
    public int getResultSlotIndex() {
        return RESULT_SLOT;
    }

    @Override
    public int getGridWidth() {
        return this.craftSlots.getWidth();
    }

    @Override
    public int getGridHeight() {
        return this.craftSlots.getHeight();
    }

    @Override
    public int getSize() {
        return 5;
    }

    public CraftingContainer getCraftSlots() {
        return this.craftSlots;
    }

    @Override
    public RecipeBookType getRecipeBookType() {
        return RecipeBookType.CRAFTING;
    }

    @Override
    public boolean shouldMoveToInventory(int slotIndex) {
        return slotIndex != this.getResultSlotIndex();
    }
}
