package com.qing_ying.ark.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.datafixers.util.Pair;
import com.qing_ying.ark.data.ArkAttachments;
import com.qing_ying.ark.data.BackpackEntry;
import com.qing_ying.ark.data.PlayerArkState;
import com.qing_ying.ark.event.ArkCommonEvents;
import com.qing_ying.ark.network.ArkNetworking;
import com.qing_ying.ark.stat.ArkStat;
import com.qing_ying.ark.ui.ArkUiLayout;
import com.qing_ying.ark.ui.ArkUiMetrics;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

public final class ArkWrappedContainerScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {
    private static final int OFFSCREEN_X = -2000;
    private static final int OFFSCREEN_Y = -2000;

    private final Inventory playerInventory;
    private final List<SlotSnapshot> slotSnapshots = new ArrayList<>();
    private final List<Slot> containerSlots = new ArrayList<>();
    private final List<Slot> hotbarSlots = new ArrayList<>();
    private final BackpackGridView backpackView = new BackpackGridView();
    private ArkUiMetrics metrics;
    private EditBox searchBox;
    private float localMouseX;
    private float localMouseY;
    private Integer pendingHotbarBindSlot;
    private String searchText = "";
    private boolean restoreSearchFocus;

    public ArkWrappedContainerScreen(T menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.playerInventory = inventory;
        this.imageWidth = ArkUiLayout.FRAME_WIDTH;
        this.imageHeight = ArkUiLayout.FRAME_HEIGHT;
        this.titleLabelY = 10000;
        this.inventoryLabelY = 10000;
        cacheSlots();
    }

    @Override
    protected void init() {
        super.init();
        this.metrics = ArkUiMetrics.create(this.width, this.height);
        this.leftPos = 0;
        this.topPos = 0;
        applySlotLayout();
        initSearchBox();
        refreshBackpackView();
    }

    @Override
    public void resize(Minecraft minecraft, int width, int height) {
        this.searchText = this.searchBox != null ? this.searchBox.getValue() : this.searchText;
        this.restoreSearchFocus = this.searchBox != null && this.searchBox.isFocused();
        super.resize(minecraft, width, height);
    }

    @Override
    public void removed() {
        restoreSlotLayout();
        super.removed();
    }

    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int leftPos, int topPos, int button) {
        return mouseX < 0.0D || mouseY < 0.0D || mouseX >= this.imageWidth || mouseY >= this.imageHeight;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        refreshBackpackView();
        this.localMouseX = (float) toLocalX(mouseX);
        this.localMouseY = (float) toLocalY(mouseY);
        this.renderTransparentBackground(guiGraphics);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(this.metrics.frameLeft(), this.metrics.frameTop(), 0.0F);
        guiGraphics.pose().scale(this.metrics.scaleF(), this.metrics.scaleF(), 1.0F);
        super.render(guiGraphics, Mth.floor(this.localMouseX), Mth.floor(this.localMouseY), partialTick);
        guiGraphics.pose().popPose();
        renderEquipmentPreviewScreen(guiGraphics, mouseX, mouseY);
        renderActiveTooltips(guiGraphics, mouseX, mouseY);
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBg(guiGraphics, partialTick, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int frameRight = ArkUiLayout.FRAME_WIDTH;

        guiGraphics.fill(RenderType.guiOverlay(), 0, 0, frameRight, ArkUiLayout.FRAME_HEIGHT, 0xD0121719);
        guiGraphics.fill(RenderType.guiOverlay(), 0, 0, frameRight, ArkUiLayout.HEADER_HEIGHT, 0xFF6EAD8D);
        guiGraphics.fill(RenderType.guiOverlay(), ArkUiLayout.BACKPACK_X - 6, ArkUiLayout.PANEL_TOP, ArkUiLayout.BACKPACK_X + ArkUiLayout.BACKPACK_COLUMNS * ArkUiLayout.BACKPACK_CELL + 12, ArkUiLayout.PANEL_BOTTOM, 0x661A2426);
        guiGraphics.fill(RenderType.guiOverlay(), ArkUiLayout.MIDDLE_X - 6, ArkUiLayout.PANEL_TOP, ArkUiLayout.RIGHT_PANEL_X - 14, ArkUiLayout.PANEL_BOTTOM, 0x661A2426);
        guiGraphics.fill(RenderType.guiOverlay(), ArkUiLayout.RIGHT_PANEL_X - 6, ArkUiLayout.PANEL_TOP, frameRight - 12, ArkUiLayout.PANEL_BOTTOM, 0x661A2426);
        guiGraphics.fill(RenderType.guiOverlay(), ArkUiLayout.MIDDLE_X - 2, ArkUiLayout.MIDDLE_DIVIDER_Y, ArkUiLayout.RIGHT_PANEL_X - 16, ArkUiLayout.MIDDLE_DIVIDER_Y + 2, 0x665E6E65);

        renderBackpackGrid(guiGraphics, mouseX, mouseY);
        renderMenuFrames(guiGraphics);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        PlayerArkState state = state();
        Player player = player();
        if (state == null || player == null) {
            return;
        }
        guiGraphics.drawString(this.font, Component.translatable("screen.ark.backpack"), ArkUiLayout.BACKPACK_X, 10, 0xFFF2F5E9, false);
        guiGraphics.drawString(this.font, Component.translatable("screen.ark.equipment"), ArkUiLayout.MIDDLE_X, 10, 0xFFF2F5E9, false);
        guiGraphics.drawString(this.font, this.title, ArkUiLayout.RIGHT_PANEL_X, 10, 0xFFF2F5E9, false);
        guiGraphics.drawString(this.font, Component.translatable("hud.ark.points", state.getAvailablePoints()), ArkUiLayout.MIDDLE_X + 8, ArkUiLayout.MIDDLE_BOTTOM_Y + 8, 0xFFB8F2A9, false);

        int lineY = ArkUiLayout.MIDDLE_BOTTOM_Y + 24;
        for (ArkStat stat : ArkStat.values()) {
            if (!stat.enabled()) {
                continue;
            }
            guiGraphics.drawString(this.font, statLine(stat, player, state), ArkUiLayout.MIDDLE_X + 8, lineY, 0xFFECEDE3, false);
            drawPlusBox(guiGraphics, lineY - 2, state.getAvailablePoints() > 0, mouseX, mouseY);
            lineY += 11;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double localX = toLocalX(mouseX);
        double localY = toLocalY(mouseY);
        if (handleHotbarClear(localX, localY, button) || handleStatClick(localX, localY, button) || handleBackpackClick(localX, localY, button)) {
            return true;
        }
        this.pendingHotbarBindSlot = null;
        if (this.hasClickedOutside(localX, localY, this.leftPos, this.topPos, button) && !this.menu.getCarried().isEmpty() && (button == 0 || button == 1)) {
            PacketDistributor.sendToServer(new ArkNetworking.DropCarriedPayload(button == 1));
            return true;
        }
        return super.mouseClicked(localX, localY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return super.mouseReleased(toLocalX(mouseX), toLocalY(mouseY), button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return super.mouseDragged(toLocalX(mouseX), toLocalY(mouseY), button, dragX / this.metrics.scale(), dragY / this.metrics.scale());
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        double localX = toLocalX(mouseX);
        double localY = toLocalY(mouseY);
        if (isMouseOverBackpack(localX, localY)) {
            int delta = scrollY > 0 ? -1 : 1;
            if (this.backpackView.isFiltering()) {
                PlayerArkState state = state();
                if (state != null) {
                    this.backpackView.scroll(state, delta);
                }
            } else {
                PacketDistributor.sendToServer(new ArkNetworking.BackpackScrollPayload(delta));
            }
            return true;
        }
        return super.mouseScrolled(localX, localY, scrollX, scrollY);
    }

    @Override
    protected void slotClicked(Slot slot, int slotId, int mouseButton, ClickType clickType) {
        if (clickType == ClickType.QUICK_MOVE && slotId >= 0) {
            this.pendingHotbarBindSlot = null;
            PacketDistributor.sendToServer(new ArkNetworking.QuickMoveToBackpackPayload(slotId));
            return;
        }
        int hotbarSlot = hotbarSlotIndex(slot, slotId);
        if (hotbarSlot >= 0 && clickType == ClickType.PICKUP && mouseButton == 0 && this.pendingHotbarBindSlot != null && !this.menu.getCarried().isEmpty()) {
            PacketDistributor.sendToServer(new ArkNetworking.BindHotbarPayload(this.pendingHotbarBindSlot, hotbarSlot));
            this.pendingHotbarBindSlot = null;
            return;
        }
        this.pendingHotbarBindSlot = null;
        super.slotClicked(slot, slotId, mouseButton, clickType);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.searchBox != null && this.searchBox.isFocused()) {
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
        if (handleHotbarNumberBinding(keyCode, scanCode)) {
            return true;
        }
        if (this.minecraft != null && this.minecraft.options.keyDrop.isActiveAndMatches(InputConstants.getKey(keyCode, scanCode))) {
            int mode = Screen.hasControlDown() ? ArkCommonEvents.DROP_ALL : (Screen.hasShiftDown() ? ArkCommonEvents.DROP_TEN : ArkCommonEvents.DROP_ONE);
            Integer slotIndex = slotIndexAt(this.localMouseX, this.localMouseY);
            if (slotIndex != null) {
                PacketDistributor.sendToServer(new ArkNetworking.DropBackpackSlotPayload(slotIndex, mode));
                return true;
            }
            PlayerArkState state = state();
            if (state != null) {
                for (Slot slot : this.hotbarSlots) {
                    int hotbarSlot = hotbarSlotIndex(slot, slot.index);
                    if (hotbarSlot >= 0 && this.localMouseX >= slot.x && this.localMouseX < slot.x + 16 && this.localMouseY >= slot.y && this.localMouseY < slot.y + 16 && state.hasHotbarBinding(hotbarSlot)) {
                        PacketDistributor.sendToServer(new ArkNetworking.DropHotbarBindingPayload(hotbarSlot, mode));
                        return true;
                    }
                }
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        return super.charTyped(codePoint, modifiers);
    }

    private void cacheSlots() {
        this.slotSnapshots.clear();
        for (int index = 0; index < this.menu.slots.size(); index++) {
            Slot slot = this.menu.slots.get(index);
            SlotRole role = SlotRole.HIDDEN;
            if (slot.container == this.playerInventory) {
                if (slot.getContainerSlot() >= 0 && slot.getContainerSlot() < 9) {
                    role = SlotRole.HOTBAR;
                }
            } else {
                role = SlotRole.CONTAINER;
            }
            this.slotSnapshots.add(new SlotSnapshot(index, slot, role));
        }
    }

    private void applySlotLayout() {
        this.containerSlots.clear();
        this.hotbarSlots.clear();

        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        for (SlotSnapshot snapshot : this.slotSnapshots) {
            if (snapshot.role() != SlotRole.CONTAINER) {
                continue;
            }
            minX = Math.min(minX, snapshot.original().x);
            minY = Math.min(minY, snapshot.original().y);
        }
        if (minX == Integer.MAX_VALUE) {
            minX = 0;
            minY = 0;
        }

        for (SlotSnapshot snapshot : this.slotSnapshots) {
            Slot relocated = switch (snapshot.role()) {
                case CONTAINER -> new RelocatedSlot(snapshot.original(), ArkUiLayout.RIGHT_SLOT_X + snapshot.original().x - minX, ArkUiLayout.RIGHT_SLOT_Y + snapshot.original().y - minY);
                case HOTBAR -> new RelocatedSlot(snapshot.original(), ArkUiLayout.HOTBAR_X + snapshot.original().getContainerSlot() * 18, ArkUiLayout.HOTBAR_Y);
                case HIDDEN -> new RelocatedSlot(snapshot.original(), OFFSCREEN_X, OFFSCREEN_Y);
            };
            this.menu.slots.set(snapshot.listIndex(), relocated);
            if (snapshot.role() == SlotRole.CONTAINER) {
                this.containerSlots.add(relocated);
            } else if (snapshot.role() == SlotRole.HOTBAR) {
                this.hotbarSlots.add(relocated);
            }
        }
        this.hotbarSlots.sort(Comparator.comparingInt(Slot::getContainerSlot));
    }

    private void restoreSlotLayout() {
        for (SlotSnapshot snapshot : this.slotSnapshots) {
            this.menu.slots.set(snapshot.listIndex(), snapshot.original());
        }
        this.containerSlots.clear();
        this.hotbarSlots.clear();
    }

    private void initSearchBox() {
        this.searchBox = new EditBox(this.font, ArkUiLayout.SEARCH_X, ArkUiLayout.SEARCH_Y, ArkUiLayout.SEARCH_WIDTH, ArkUiLayout.SEARCH_HEIGHT, Component.translatable("screen.ark.search"));
        this.searchBox.setMaxLength(48);
        this.searchBox.setHint(Component.translatable("screen.ark.search_hint"));
        this.searchBox.setResponder(value -> {
            this.searchText = value;
            refreshBackpackView();
        });
        this.searchBox.setValue(this.searchText);
        this.searchBox.setFocused(this.restoreSearchFocus);
        this.restoreSearchFocus = false;
        this.addRenderableWidget(this.searchBox);
    }

    private void refreshBackpackView() {
        PlayerArkState state = state();
        if (state != null) {
            this.backpackView.setQuery(state, this.searchText);
        }
    }

    private void renderActiveTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (this.menu.getCarried().isEmpty() && this.hoveredSlot != null && this.hoveredSlot.hasItem()) {
            ItemStack hoveredStack = this.hoveredSlot.getItem();
            guiGraphics.renderTooltip(this.font, this.getTooltipFromContainerItem(hoveredStack), hoveredStack.getTooltipImage(), hoveredStack, mouseX, mouseY);
            return;
        }
        BackpackEntry entry = entryAt(toLocalX(mouseX), toLocalY(mouseY));
        if (entry != null) {
            guiGraphics.renderTooltip(this.font, entry.asDisplayStack(), mouseX, mouseY);
        }
    }

    private void renderMenuFrames(GuiGraphics guiGraphics) {
        for (Slot slot : this.containerSlots) {
            drawSlotFrame(guiGraphics, slot.x - 1, slot.y - 1);
        }
        for (Slot slot : this.hotbarSlots) {
            drawSlotFrame(guiGraphics, slot.x - 1, slot.y - 1);
        }
    }

    private void drawSlotFrame(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.fill(RenderType.guiOverlay(), x, y, x + 18, y + 18, 0x88293335);
        guiGraphics.fill(RenderType.guiOverlay(), x + 1, y + 1, x + 17, y + 17, 0x44212627);
    }

    private void renderBackpackGrid(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        PlayerArkState state = state();
        if (state == null) {
            return;
        }
        for (int row = 0; row < ArkUiLayout.BACKPACK_VISIBLE_ROWS; row++) {
            for (int column = 0; column < ArkUiLayout.BACKPACK_COLUMNS; column++) {
                int local = row * ArkUiLayout.BACKPACK_COLUMNS + column;
                int cellX = ArkUiLayout.BACKPACK_X + column * ArkUiLayout.BACKPACK_CELL;
                int cellY = ArkUiLayout.BACKPACK_Y + row * ArkUiLayout.BACKPACK_CELL;
                BackpackEntry entry = this.backpackView.entryForCell(state, local);
                boolean hovered = mouseX >= cellX && mouseX < cellX + 18 && mouseY >= cellY && mouseY < cellY + 18;
                int boundSlot = entry == null ? -1 : state.boundSlotFor(entry.entryId());
                int background = hovered ? 0xAA50786D : 0x66323D3E;
                if (boundSlot >= 0) {
                    background = hovered ? 0xFF6FA56A : 0xCC5C8350;
                }
                guiGraphics.fill(RenderType.guiOverlay(), cellX - 1, cellY - 1, cellX + 18, cellY + 18, background);
                if (boundSlot >= 0) {
                    guiGraphics.fill(RenderType.guiOverlay(), cellX, cellY, cellX + 17, cellY + 1, 0xFFB9E4A6);
                    guiGraphics.fill(RenderType.guiOverlay(), cellX, cellY + 16, cellX + 17, cellY + 17, 0xFF40613A);
                }
                if (entry != null) {
                    ItemStack display = entry.asDisplayStack();
                    guiGraphics.renderItem(display, cellX + 1, cellY + 1);
                    guiGraphics.renderItemDecorations(this.font, display, cellX + 1, cellY + 1, shortNumber(state.displayCountFor(entry.entryId())));
                    if (boundSlot >= 0) {
                        guiGraphics.drawString(this.font, Integer.toString(boundSlot + 1), cellX + 11, cellY + 10, 0xFF9EE39A, false);
                    }
                }
            }
        }

        if (this.backpackView.isFiltering() && !this.backpackView.hasResults()) {
            guiGraphics.drawCenteredString(this.font, Component.translatable("screen.ark.search_empty"), ArkUiLayout.BACKPACK_X + (ArkUiLayout.BACKPACK_COLUMNS * ArkUiLayout.BACKPACK_CELL) / 2, ArkUiLayout.BACKPACK_Y + 48, 0xFFB3BAB2);
        }
    }

    private void renderEquipmentPreviewScreen(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        Player player = player();
        if (player == null) {
            return;
        }
        int boxLeft = ArkUiLayout.MIDDLE_X + 8;
        int boxRight = ArkUiLayout.RIGHT_PANEL_X - 18;
        int boxTop = ArkUiLayout.MIDDLE_TOP_Y + 4;
        int boxBottom = ArkUiLayout.MIDDLE_DIVIDER_Y - 6;
        int boxWidth = Math.max(1, boxRight - boxLeft);
        int boxHeight = Math.max(1, boxBottom - boxTop);
        int boxSize = Math.max(24, (int) (Math.min(boxWidth, boxHeight) * 0.48f));
        double scale = this.metrics.scale();
        int screenLeft = this.metrics.frameLeft() + (int) Math.round(boxLeft * scale);
        int screenRight = this.metrics.frameLeft() + (int) Math.round(boxRight * scale);
        int screenTop = this.metrics.frameTop() + (int) Math.round(boxTop * scale);
        int screenBottom = this.metrics.frameTop() + (int) Math.round(boxBottom * scale);
        int screenSize = Math.max(1, (int) Math.round(boxSize * scale));
        InventoryScreen.renderEntityInInventoryFollowsMouse(
                guiGraphics,
                screenLeft,
                screenTop,
                screenRight,
                screenBottom,
                screenSize,
                0.0625F,
                mouseX,
                mouseY,
                player
        );
    }

    private boolean handleBackpackClick(double mouseX, double mouseY, int button) {
        Integer cellIndex = gridCellIndexAt(mouseX, mouseY);
        if (cellIndex == null) {
            return false;
        }
        PlayerArkState state = state();
        if (state == null) {
            return true;
        }
        Integer slotIndex = this.backpackView.slotIndexForCell(state, cellIndex);
        BackpackEntry entry = this.backpackView.entryForCell(state, cellIndex);
        if (button == 0) {
            this.pendingHotbarBindSlot = this.menu.getCarried().isEmpty() && entry != null ? slotIndex : null;
        }
        if (slotIndex == null) {
            return true;
        }
        PacketDistributor.sendToServer(new ArkNetworking.BackpackGridClickPayload(slotIndex, button));
        return true;
    }

    private boolean handleStatClick(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return false;
        }
        PlayerArkState state = state();
        if (state == null || state.getAvailablePoints() <= 0) {
            return false;
        }
        int lineY = ArkUiLayout.MIDDLE_BOTTOM_Y + 24;
        for (ArkStat stat : ArkStat.values()) {
            if (!stat.enabled()) {
                continue;
            }
            int x = ArkUiLayout.MIDDLE_X + 142;
            int y = lineY - 2;
            if (mouseX >= x && mouseX < x + 14 && mouseY >= y && mouseY < y + 14) {
                PacketDistributor.sendToServer(new ArkNetworking.SpendStatPayload(stat));
                return true;
            }
            lineY += 11;
        }
        return false;
    }

    private boolean handleHotbarClear(double mouseX, double mouseY, int button) {
        if (button != 1 || !this.menu.getCarried().isEmpty()) {
            return false;
        }
        PlayerArkState state = state();
        if (state == null) {
            return false;
        }
        for (Slot slot : this.hotbarSlots) {
            int hotbarSlot = hotbarSlotIndex(slot, slot.index);
            if (hotbarSlot >= 0 && mouseX >= slot.x && mouseX < slot.x + 16 && mouseY >= slot.y && mouseY < slot.y + 16 && state.hasHotbarBinding(hotbarSlot)) {
                PacketDistributor.sendToServer(new ArkNetworking.ClearHotbarBindingPayload(hotbarSlot));
                return true;
            }
        }
        return false;
    }

    private boolean handleHotbarNumberBinding(int keyCode, int scanCode) {
        if (this.minecraft == null || Screen.hasControlDown() || Screen.hasShiftDown()) {
            return false;
        }
        Integer slotIndex = slotIndexAt(this.localMouseX, this.localMouseY);
        if (slotIndex == null || entryAt(this.localMouseX, this.localMouseY) == null) {
            return false;
        }
        for (int hotbar = 0; hotbar < 9; hotbar++) {
            if (this.minecraft.options.keyHotbarSlots[hotbar].isActiveAndMatches(InputConstants.getKey(keyCode, scanCode))) {
                PacketDistributor.sendToServer(new ArkNetworking.BindHotbarPayload(slotIndex, hotbar));
                this.pendingHotbarBindSlot = null;
                return true;
            }
        }
        return false;
    }

    private boolean isMouseOverBackpack(double mouseX, double mouseY) {
        int x = ArkUiLayout.BACKPACK_X - 2;
        int y = ArkUiLayout.BACKPACK_Y - 2;
        int width = ArkUiLayout.BACKPACK_COLUMNS * ArkUiLayout.BACKPACK_CELL;
        int height = ArkUiLayout.BACKPACK_VISIBLE_ROWS * ArkUiLayout.BACKPACK_CELL;
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private Integer gridCellIndexAt(double mouseX, double mouseY) {
        if (!isMouseOverBackpack(mouseX, mouseY)) {
            return null;
        }
        int localX = (int) ((mouseX - ArkUiLayout.BACKPACK_X) / ArkUiLayout.BACKPACK_CELL);
        int localY = (int) ((mouseY - ArkUiLayout.BACKPACK_Y) / ArkUiLayout.BACKPACK_CELL);
        if (localX < 0 || localX >= ArkUiLayout.BACKPACK_COLUMNS || localY < 0 || localY >= ArkUiLayout.BACKPACK_VISIBLE_ROWS) {
            return null;
        }
        return localY * ArkUiLayout.BACKPACK_COLUMNS + localX;
    }

    private Integer slotIndexAt(double mouseX, double mouseY) {
        PlayerArkState state = state();
        Integer cellIndex = gridCellIndexAt(mouseX, mouseY);
        if (state == null || cellIndex == null) {
            return null;
        }
        return this.backpackView.slotIndexForCell(state, cellIndex);
    }

    private BackpackEntry entryAt(double mouseX, double mouseY) {
        PlayerArkState state = state();
        Integer cellIndex = gridCellIndexAt(mouseX, mouseY);
        if (state == null || cellIndex == null) {
            return null;
        }
        return this.backpackView.entryForCell(state, cellIndex);
    }

    private int hotbarSlotIndex(Slot slot, int slotId) {
        if (slotId >= 0 && slotId < this.menu.slots.size()) {
            Slot indexedSlot = this.menu.slots.get(slotId);
            if (indexedSlot != null && indexedSlot.container == this.playerInventory && indexedSlot.getContainerSlot() >= 0 && indexedSlot.getContainerSlot() < 9) {
                return indexedSlot.getContainerSlot();
            }
        }
        if (slot != null && slot.container == this.playerInventory && slot.getContainerSlot() >= 0 && slot.getContainerSlot() < 9) {
            return slot.getContainerSlot();
        }
        return -1;
    }

    private void drawPlusBox(GuiGraphics guiGraphics, int y, boolean active, int mouseX, int mouseY) {
        int x = ArkUiLayout.MIDDLE_X + 142;
        boolean hovered = mouseX >= x && mouseX < x + 14 && mouseY >= y && mouseY < y + 14;
        int fill = active ? (hovered ? 0xFF89B866 : 0xFF6C9250) : 0xFF4A4A45;
        guiGraphics.fill(RenderType.guiOverlay(), x, y, x + 14, y + 14, fill);
        guiGraphics.drawCenteredString(this.font, "+", x + 7, y + 3, 0xFFF5F6EC);
    }

    private Component statLine(ArkStat stat, Player player, PlayerArkState state) {
        return switch (stat) {
            case HEALTH -> Component.translatable("screen.ark.stat_line", stat.displayName(), shortNumber(player.getHealth()) + "/" + shortNumber(player.getMaxHealth()), state.getAllocatedPoints(stat));
            case STAMINA -> Component.translatable("screen.ark.stat_line", stat.displayName(), shortNumber(state.getStamina()) + "/" + shortNumber(state.maxFor(stat)), state.getAllocatedPoints(stat));
            case OXYGEN -> Component.translatable("screen.ark.stat_line", stat.displayName(), shortNumber(state.getOxygen()) + "/" + shortNumber(state.maxFor(stat)), state.getAllocatedPoints(stat));
            case FOOD -> Component.translatable("screen.ark.stat_line", stat.displayName(), shortNumber(state.getFood()) + "/" + shortNumber(state.maxFor(stat)), state.getAllocatedPoints(stat));
            case WATER -> Component.translatable("screen.ark.stat_line", stat.displayName(), shortNumber(state.getWater()) + "/" + shortNumber(state.maxFor(stat)), state.getAllocatedPoints(stat));
            case WEIGHT -> Component.translatable("screen.ark.stat_line", stat.displayName(), shortNumber(ArkCommonEvents.calculateTotalWeight(player, state)) + "/" + shortNumber(ArkCommonEvents.weightCapacity(state)), state.getAllocatedPoints(stat));
            case MELEE_DAMAGE -> Component.translatable("screen.ark.stat_line", stat.displayName(), "+" + shortNumber(state.getAllocatedPoints(stat) * com.qing_ying.ark.config.ArkConfig.meleePerPoint() * 100.0D) + "%", state.getAllocatedPoints(stat));
            case MOVE_SPEED -> Component.translatable("screen.ark.stat_line", stat.displayName(), "+" + shortNumber(state.getAllocatedPoints(stat) * com.qing_ying.ark.config.ArkConfig.speedPerPoint() * 100.0D) + "%", state.getAllocatedPoints(stat));
            case RESISTANCE -> Component.literal("");
        };
    }

    private Player player() {
        return Minecraft.getInstance().player;
    }

    private PlayerArkState state() {
        Player player = player();
        return player == null ? null : ArkAttachments.get(player);
    }

    private double toLocalX(double mouseX) {
        return this.metrics.toLocalX(mouseX);
    }

    private double toLocalY(double mouseY) {
        return this.metrics.toLocalY(mouseY);
    }

    private static String shortNumber(double value) {
        if (value >= 1_000_000) {
            return String.format(Locale.ROOT, "%.1fm", value / 1_000_000.0D);
        }
        if (value >= 10_000) {
            return String.format(Locale.ROOT, "%.1fk", value / 1_000.0D);
        }
        if (Math.abs(value - Math.rint(value)) < 0.01D) {
            return Integer.toString((int) Math.rint(value));
        }
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private record SlotSnapshot(int listIndex, Slot original, SlotRole role) {
    }

    private enum SlotRole {
        CONTAINER,
        HOTBAR,
        HIDDEN
    }

    private static final class RelocatedSlot extends Slot {
        private final Slot target;

        private RelocatedSlot(Slot target, int x, int y) {
            super(target.container, target.getContainerSlot(), x, y);
            this.target = target;
        }

        @Override
        public void onQuickCraft(ItemStack newStack, ItemStack originalStack) {
            this.target.onQuickCraft(newStack, originalStack);
        }

        @Override
        public void onTake(Player player, ItemStack stack) {
            this.target.onTake(player, stack);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return this.target.mayPlace(stack);
        }

        @Override
        public ItemStack getItem() {
            return this.target.getItem();
        }

        @Override
        public boolean hasItem() {
            return this.target.hasItem();
        }

        @Override
        public void setByPlayer(ItemStack newStack, ItemStack oldStack) {
            this.target.setByPlayer(newStack, oldStack);
        }

        @Override
        public void set(ItemStack stack) {
            this.target.set(stack);
        }

        @Override
        public void setChanged() {
            this.target.setChanged();
        }

        @Override
        public int getMaxStackSize() {
            return this.target.getMaxStackSize();
        }

        @Override
        public int getMaxStackSize(ItemStack stack) {
            return this.target.getMaxStackSize(stack);
        }

        @Override
        public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
            return this.target.getNoItemIcon();
        }

        @Override
        public ItemStack remove(int amount) {
            return this.target.remove(amount);
        }

        @Override
        public boolean isActive() {
            return this.target.isActive();
        }

        @Override
        public boolean mayPickup(Player player) {
            return this.target.mayPickup(player);
        }

        @Override
        public int getSlotIndex() {
            return this.target.getSlotIndex();
        }

        @Override
        public boolean isSameInventory(Slot other) {
            return this.target.isSameInventory(other instanceof RelocatedSlot relocated ? relocated.target : other);
        }

        @Override
        public Slot setBackground(ResourceLocation atlas, ResourceLocation sprite) {
            this.target.setBackground(atlas, sprite);
            return this;
        }
    }
}
