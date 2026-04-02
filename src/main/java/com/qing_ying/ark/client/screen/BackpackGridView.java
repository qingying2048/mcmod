package com.qing_ying.ark.client.screen;

import com.qing_ying.ark.data.BackpackEntry;
import com.qing_ying.ark.data.BackpackSlot;
import com.qing_ying.ark.data.PlayerArkState;
import com.qing_ying.ark.ui.ArkUiLayout;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.util.Mth;

public final class BackpackGridView {
    private static final int VISIBLE_CELLS = ArkUiLayout.BACKPACK_COLUMNS * ArkUiLayout.BACKPACK_VISIBLE_ROWS;

    private final List<Integer> filteredSlotIndices = new ArrayList<>();
    private String query = "";
    private String normalizedQuery = "";
    private int filteredScrollRow;

    public void setQuery(PlayerArkState state, String query) {
        String safeQuery = query == null ? "" : query;
        String normalized = normalize(safeQuery);
        boolean changed = !safeQuery.equals(this.query) || !normalized.equals(this.normalizedQuery);
        this.query = safeQuery;
        this.normalizedQuery = normalized;
        if (changed) {
            this.filteredScrollRow = 0;
        }
        rebuild(state);
    }

    public void refresh(PlayerArkState state) {
        rebuild(state);
    }

    public String query() {
        return this.query;
    }

    public boolean isFiltering() {
        return !this.normalizedQuery.isEmpty();
    }

    public boolean hasResults() {
        return !this.isFiltering() || !this.filteredSlotIndices.isEmpty();
    }

    public void scroll(PlayerArkState state, int delta) {
        if (!this.isFiltering()) {
            return;
        }
        this.filteredScrollRow = Mth.clamp(this.filteredScrollRow + delta, 0, maxFilteredScrollRow());
        rebuild(state);
    }

    public Integer slotIndexForCell(PlayerArkState state, int cellIndex) {
        if (cellIndex < 0 || cellIndex >= VISIBLE_CELLS) {
            return null;
        }
        if (!this.isFiltering()) {
            return state.getScrollOffset() * ArkUiLayout.BACKPACK_COLUMNS + cellIndex;
        }
        int filteredIndex = this.filteredScrollRow * ArkUiLayout.BACKPACK_COLUMNS + cellIndex;
        return filteredIndex >= 0 && filteredIndex < this.filteredSlotIndices.size() ? this.filteredSlotIndices.get(filteredIndex) : null;
    }

    public BackpackEntry entryForCell(PlayerArkState state, int cellIndex) {
        Integer slotIndex = slotIndexForCell(state, cellIndex);
        if (slotIndex == null) {
            return null;
        }
        BackpackSlot slot = state.getSlot(slotIndex);
        return slot == null || slot.isEmpty() ? null : slot.entry();
    }

    public boolean allowPlacementIntoEmptyCell() {
        return !this.isFiltering();
    }

    public static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return Normalizer.normalize(value.trim(), Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
    }

    private void rebuild(PlayerArkState state) {
        this.filteredSlotIndices.clear();
        if (state == null) {
            this.filteredScrollRow = 0;
            return;
        }
        if (!this.isFiltering()) {
            return;
        }
        for (int i = 0; i < state.backpackSlots().size(); i++) {
            BackpackSlot slot = state.getSlot(i);
            if (slot == null || slot.isEmpty()) {
                continue;
            }
            if (normalize(slot.entry().template().getHoverName().getString()).contains(this.normalizedQuery)) {
                this.filteredSlotIndices.add(i);
            }
        }
        this.filteredScrollRow = Mth.clamp(this.filteredScrollRow, 0, maxFilteredScrollRow());
    }

    private int maxFilteredScrollRow() {
        return Math.max(0, Mth.positiveCeilDiv(this.filteredSlotIndices.size(), ArkUiLayout.BACKPACK_COLUMNS) - ArkUiLayout.BACKPACK_VISIBLE_ROWS);
    }
}
