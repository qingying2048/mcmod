package com.qing_ying.ark.data;

import com.qing_ying.ark.config.ArkConfig;
import com.qing_ying.ark.stat.ArkStat;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.INBTSerializable;

public final class PlayerArkState implements INBTSerializable<CompoundTag> {
    private static final int HOTBAR_SIZE = 9;
    private static final int GRID_COLUMNS = 6;
    private static final int TRAILING_FREE_ROWS = 4;

    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerArkState> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public PlayerArkState decode(RegistryFriendlyByteBuf buf) {
            PlayerArkState state = new PlayerArkState();
            state.initialized = buf.readBoolean();
            state.modXp = buf.readVarLong();
            state.modLevel = buf.readVarInt();
            state.availablePoints = buf.readVarInt();
            for (ArkStat stat : ArkStat.values()) {
                state.allocatedPoints.put(stat, buf.readVarInt());
            }
            state.stamina = buf.readDouble();
            state.oxygen = buf.readDouble();
            state.food = buf.readDouble();
            state.water = buf.readDouble();
            state.nextEntryId = Math.max(1L, buf.readVarLong());
            int backpackSize = buf.readVarInt();
            state.backpackSlots.clear();
            for (int i = 0; i < backpackSize; i++) {
                state.backpackSlots.add(BackpackSlot.read(buf));
            }
            for (int i = 0; i < HOTBAR_SIZE; i++) {
                state.hotbarBindings[i] = HotbarBinding.read(buf);
            }
            state.scrollOffset = buf.readVarInt();
            state.normalize();
            state.dirty = false;
            return state;
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, PlayerArkState state) {
            buf.writeBoolean(state.initialized);
            buf.writeVarLong(state.modXp);
            buf.writeVarInt(state.modLevel);
            buf.writeVarInt(state.availablePoints);
            for (ArkStat stat : ArkStat.values()) {
                buf.writeVarInt(state.getAllocatedPoints(stat));
            }
            buf.writeDouble(state.stamina);
            buf.writeDouble(state.oxygen);
            buf.writeDouble(state.food);
            buf.writeDouble(state.water);
            buf.writeVarLong(state.nextEntryId);
            buf.writeVarInt(state.backpackSlots.size());
            for (BackpackSlot slot : state.backpackSlots) {
                slot.write(buf);
            }
            for (HotbarBinding binding : state.hotbarBindings) {
                binding.write(buf);
            }
            buf.writeVarInt(state.scrollOffset);
        }
    };

    private boolean initialized;
    private long modXp;
    private int modLevel;
    private int availablePoints;
    private final EnumMap<ArkStat, Integer> allocatedPoints = new EnumMap<>(ArkStat.class);
    private double stamina;
    private double oxygen;
    private double food;
    private double water;
    private long nextEntryId = 1L;
    private final List<BackpackSlot> backpackSlots = new ArrayList<>();
    private final HotbarBinding[] hotbarBindings = new HotbarBinding[HOTBAR_SIZE];
    private int scrollOffset;
    private boolean dirty = true;

    public PlayerArkState() {
        for (ArkStat stat : ArkStat.values()) {
            this.allocatedPoints.put(stat, 0);
        }
        for (int i = 0; i < HOTBAR_SIZE; i++) {
            this.hotbarBindings[i] = new HotbarBinding();
        }
    }

    public void ensureInitialized() {
        if (!this.initialized) {
            this.initialized = true;
            this.modLevel = 1;
            this.modXp = 0L;
            this.availablePoints = 0;
            this.stamina = maxFor(ArkStat.STAMINA);
            this.oxygen = maxFor(ArkStat.OXYGEN);
            this.food = maxFor(ArkStat.FOOD);
            this.water = maxFor(ArkStat.WATER);
            this.markDirty();
        }
        this.normalize();
        this.stamina = clampResource(this.stamina, ArkStat.STAMINA);
        this.oxygen = clampResource(this.oxygen, ArkStat.OXYGEN);
        this.food = clampResource(this.food, ArkStat.FOOD);
        this.water = clampResource(this.water, ArkStat.WATER);
    }

    public long getModXp() { return modXp; }
    public int getModLevel() { return modLevel; }
    public int getAvailablePoints() { return availablePoints; }
    public double getStamina() { return stamina; }
    public double getOxygen() { return oxygen; }
    public double getFood() { return food; }
    public double getWater() { return water; }
    public int getScrollOffset() { return scrollOffset; }
    public List<BackpackSlot> backpackSlots() { return backpackSlots; }

    public int getAllocatedPoints(ArkStat stat) {
        return this.allocatedPoints.getOrDefault(stat, 0);
    }

    public double maxFor(ArkStat stat) {
        return stat.valueFor(this);
    }

    public void setScrollOffset(int scrollOffset) {
        int clamped = Mth.clamp(scrollOffset, 0, Math.max(0, totalBackpackRows() - visibleBackpackRows()));
        if (this.scrollOffset != clamped) {
            this.scrollOffset = clamped;
            this.markDirty();
        }
    }

    public int totalBackpackRows() {
        return Math.max(TRAILING_FREE_ROWS, Mth.positiveCeilDiv(this.backpackSlots.size(), GRID_COLUMNS));
    }

    public int visibleBackpackRows() {
        return 6;
    }

    public int totalBackpackSlots() {
        return totalBackpackRows() * GRID_COLUMNS;
    }

    public HotbarBinding getHotbarBinding(int slot) {
        return slot >= 0 && slot < HOTBAR_SIZE ? this.hotbarBindings[slot] : null;
    }

    public boolean hasHotbarBinding(int slot) {
        HotbarBinding binding = getHotbarBinding(slot);
        return binding != null && binding.isBound();
    }

    public int reservedCountFor(long entryId) {
        int total = 0;
        for (HotbarBinding binding : this.hotbarBindings) {
            if (binding.entryId() == entryId) {
                total += binding.reservedCount();
            }
        }
        return total;
    }

    public int displayCountFor(long entryId) {
        BackpackEntry entry = getEntry(entryId);
        return (entry == null ? 0 : entry.storedCount()) + reservedCountFor(entryId);
    }

    public void bindHotbar(int slot, long entryId) {
        HotbarBinding binding = getHotbarBinding(slot);
        if (binding != null) {
            for (int i = 0; i < HOTBAR_SIZE; i++) {
                if (i != slot && this.hotbarBindings[i].entryId() == entryId) {
                    this.hotbarBindings[i].clear();
                    this.markDirty();
                }
            }
        }
        if (binding != null && (!binding.isBound() || binding.entryId() != entryId)) {
            binding.bind(entryId);
            this.markDirty();
        }
    }

    public void clearHotbarBinding(int slot) {
        HotbarBinding binding = getHotbarBinding(slot);
        if (binding != null && binding.isBound()) {
            binding.clear();
            this.markDirty();
        }
    }

    public boolean isEntryBound(long entryId) {
        for (HotbarBinding binding : this.hotbarBindings) {
            if (binding.entryId() == entryId) {
                return true;
            }
        }
        return false;
    }

    public int boundSlotFor(long entryId) {
        for (int i = 0; i < HOTBAR_SIZE; i++) {
            if (this.hotbarBindings[i].entryId() == entryId) {
                return i;
            }
        }
        return -1;
    }

    public boolean addExperience(int amount) {
        if (amount <= 0) return false;
        ensureInitialized();
        boolean leveledUp = false;
        this.modXp += amount;
        while (this.modXp >= ArkConfig.xpForNextLevel(this.modLevel)) {
            this.modXp -= ArkConfig.xpForNextLevel(this.modLevel);
            this.modLevel++;
            this.availablePoints++;
            leveledUp = true;
        }
        this.markDirty();
        return leveledUp;
    }

    public void setModXp(long amount) {
        this.modXp = Math.max(0L, amount);
        this.markDirty();
    }

    public void setModLevel(int level) {
        this.modLevel = Math.max(1, level);
        this.markDirty();
    }

    public void addAvailablePoints(int amount) {
        if (amount == 0) {
            return;
        }
        this.availablePoints = Math.max(0, this.availablePoints + amount);
        this.markDirty();
    }

    public void setAvailablePoints(int amount) {
        this.availablePoints = Math.max(0, amount);
        this.markDirty();
    }

    public boolean spendPoint(ArkStat stat) {
        ensureInitialized();
        if (!stat.enabled() || this.availablePoints <= 0) return false;
        this.availablePoints--;
        this.allocatedPoints.merge(stat, 1, Integer::sum);
        this.stamina = clampResource(this.stamina, ArkStat.STAMINA);
        this.oxygen = clampResource(this.oxygen, ArkStat.OXYGEN);
        this.food = clampResource(this.food, ArkStat.FOOD);
        this.water = clampResource(this.water, ArkStat.WATER);
        this.markDirty();
        return true;
    }

    public int resetAllocatedPoints() {
        int spent = 0;
        for (ArkStat stat : ArkStat.values()) {
            int points = this.allocatedPoints.getOrDefault(stat, 0);
            spent += points;
            this.allocatedPoints.put(stat, 0);
        }
        this.availablePoints += spent;
        this.stamina = clampResource(this.stamina, ArkStat.STAMINA);
        this.oxygen = clampResource(this.oxygen, ArkStat.OXYGEN);
        this.food = clampResource(this.food, ArkStat.FOOD);
        this.water = clampResource(this.water, ArkStat.WATER);
        this.markDirty();
        return spent;
    }

    public double addStamina(double delta) { return setResource(ArkStat.STAMINA, this.stamina + delta); }
    public double addOxygen(double delta) { return setResource(ArkStat.OXYGEN, this.oxygen + delta); }
    public double addFood(double delta) { return setResource(ArkStat.FOOD, this.food + delta); }
    public double addWater(double delta) { return setResource(ArkStat.WATER, this.water + delta); }

    public ItemStackResult storeStack(ItemStack stack) {
        ensureInitialized();
        if (stack.isEmpty()) return new ItemStackResult(0, stack, null);
        int original = stack.getCount();
        int remaining = original;
        BackpackEntry target = null;
        int forcedRows = 0;
        for (BackpackSlot slot : this.backpackSlots) {
            BackpackEntry entry = slot.entry();
            if (entry != null && entry.matches(stack) && remaining > 0) {
                ItemStack probe = stack.copyWithCount(remaining);
                int moved = entry.insert(probe);
                remaining -= moved;
                if (moved > 0 && target == null) {
                    target = entry;
                }
            }
        }
        while (remaining > 0) {
            BackpackEntry created = new BackpackEntry(claimEntryId(), stack, remaining);
            int targetSlot = firstEmptySlotIndex();
            forcedRows = Math.max(forcedRows, forcedRowsForPlacement(targetSlot >= 0 ? targetSlot : this.backpackSlots.size()));
            if (targetSlot >= 0) {
                ensureSlotCount(targetSlot + 1);
                this.backpackSlots.get(targetSlot).setEntry(created);
            } else {
                this.backpackSlots.add(new BackpackSlot(created));
            }
            if (target == null) {
                target = created;
            }
            remaining = 0;
        }
        this.normalize(forcedRows);
        this.markDirty();
        return new ItemStackResult(original, ItemStack.EMPTY, target);
    }

    public BackpackEntry removeEntry(long entryId) {
        int index = findSlotIndex(entryId);
        if (index < 0) return null;
        BackpackEntry entry = this.backpackSlots.get(index).removeEntry();
        clearBindingsFor(entryId);
        this.normalize(0);
        this.markDirty();
        return entry;
    }

    public BackpackEntry getEntry(long entryId) {
        int index = findSlotIndex(entryId);
        return index >= 0 ? this.backpackSlots.get(index).entry() : null;
    }

    public int findSlotIndex(long entryId) {
        for (int i = 0; i < this.backpackSlots.size(); i++) {
            BackpackEntry entry = this.backpackSlots.get(i).entry();
            if (entry != null && entry.entryId() == entryId) {
                return i;
            }
        }
        return -1;
    }

    public BackpackSlot getSlot(int slotIndex) {
        return slotIndex >= 0 && slotIndex < this.backpackSlots.size() ? this.backpackSlots.get(slotIndex) : null;
    }

    public boolean isSlotEmpty(int slotIndex) {
        BackpackSlot slot = getSlot(slotIndex);
        return slot == null || slot.isEmpty();
    }

    public int firstEmptySlotIndex() {
        for (int i = 0; i < this.backpackSlots.size(); i++) {
            if (this.backpackSlots.get(i).isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    public BackpackEntry removeEntryAt(int slotIndex) {
        BackpackSlot slot = getSlot(slotIndex);
        if (slot == null || slot.isEmpty()) {
            return null;
        }
        BackpackEntry removed = slot.removeEntry();
        clearBindingsFor(removed.entryId());
        this.normalize(0);
        this.markDirty();
        return removed;
    }

    public void setEntryAt(int slotIndex, BackpackEntry entry) {
        setEntryAt(slotIndex, entry, 0);
    }

    private void setEntryAt(int slotIndex, BackpackEntry entry, int forcedRows) {
        ensureSlotCount(slotIndex + 1);
        this.backpackSlots.get(slotIndex).setEntry(entry);
        this.normalize(forcedRows);
        this.markDirty();
    }

    public BackpackEntry createEntry(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        return new BackpackEntry(claimEntryId(), stack, stack.getCount());
    }

    public BackpackEntry placeStackAt(int slotIndex, ItemStack stack) {
        BackpackEntry entry = createEntry(stack);
        if (entry != null) {
            setEntryAt(slotIndex, entry, forcedRowsForPlacement(slotIndex));
        }
        return entry;
    }

    public void clearBackpack() {
        this.backpackSlots.clear();
        this.scrollOffset = 0;
        for (HotbarBinding binding : this.hotbarBindings) {
            binding.clear();
        }
        ensureMinimumSlots(0);
        this.markDirty();
    }

    public void copyPersistentFrom(PlayerArkState other, boolean includeBackpack) {
        this.initialized = other.initialized;
        this.modXp = other.modXp;
        this.modLevel = other.modLevel;
        this.availablePoints = other.availablePoints;
        this.allocatedPoints.clear();
        this.allocatedPoints.putAll(other.allocatedPoints);
        this.stamina = other.stamina;
        this.oxygen = other.oxygen;
        this.food = other.food;
        this.water = other.water;
        this.nextEntryId = other.nextEntryId;
        this.scrollOffset = other.scrollOffset;
        this.backpackSlots.clear();
        if (includeBackpack) {
            for (BackpackSlot slot : other.backpackSlots) {
                if (slot.isEmpty()) {
                    this.backpackSlots.add(new BackpackSlot());
                } else {
                    BackpackEntry entry = slot.entry();
                    this.backpackSlots.add(new BackpackSlot(new BackpackEntry(entry.entryId(), entry.template(), entry.storedCount())));
                }
            }
            for (int i = 0; i < HOTBAR_SIZE; i++) {
                this.hotbarBindings[i] = new HotbarBinding(other.hotbarBindings[i].entryId(), other.hotbarBindings[i].reservedCount());
            }
        } else {
            for (int i = 0; i < HOTBAR_SIZE; i++) {
                this.hotbarBindings[i] = new HotbarBinding();
            }
        }
        this.normalize(0);
        this.markDirty();
    }

    public boolean isDirty() { return dirty; }
    public void markDirty() { this.dirty = true; }
    public void clearDirty() { this.dirty = false; }

    private long claimEntryId() {
        return this.nextEntryId++;
    }

    private void clearBindingsFor(long entryId) {
        for (HotbarBinding binding : this.hotbarBindings) {
            if (binding.entryId() == entryId) {
                binding.clear();
            }
        }
    }

    private double clampResource(double value, ArkStat stat) {
        return Mth.clamp(value, 0.0D, maxFor(stat));
    }

    private double setResource(ArkStat stat, double value) {
        double clamped = clampResource(value, stat);
        double current = switch (stat) {
            case STAMINA -> this.stamina;
            case OXYGEN -> this.oxygen;
            case FOOD -> this.food;
            case WATER -> this.water;
            default -> clamped;
        };
        if (Math.abs(current - clamped) > 1.0E-6D) {
            switch (stat) {
                case STAMINA -> this.stamina = clamped;
                case OXYGEN -> this.oxygen = clamped;
                case FOOD -> this.food = clamped;
                case WATER -> this.water = clamped;
                default -> {
                }
            }
            this.markDirty();
        }
        return clamped;
    }

    private void normalize() {
        normalize(0);
    }

    private void normalize(int forcedRows) {
        for (int i = 0; i < this.backpackSlots.size(); i++) {
            BackpackSlot slot = this.backpackSlots.get(i);
            if (!slot.isEmpty() && slot.entry().template().isEmpty()) {
                slot.setEntry(null);
            }
        }
        long maxEntryId = 0L;
        for (BackpackSlot slot : this.backpackSlots) {
            if (!slot.isEmpty()) {
                maxEntryId = Math.max(maxEntryId, slot.entry().entryId());
            }
        }
        this.nextEntryId = Math.max(this.nextEntryId, maxEntryId + 1L);
        trimTrailingEmptySlots(forcedRows);
        ensureMinimumSlots(forcedRows);
        int maxOffset = Math.max(0, totalBackpackRows() - visibleBackpackRows());
        this.scrollOffset = Mth.clamp(this.scrollOffset, 0, maxOffset);
        for (HotbarBinding binding : this.hotbarBindings) {
            if (binding.isBound() && getEntry(binding.entryId()) == null) {
                binding.clear();
            }
        }
        for (int i = 0; i < this.backpackSlots.size(); i++) {
            BackpackSlot slot = this.backpackSlots.get(i);
            if (!slot.isEmpty() && slot.entry().storedCount() <= 0 && reservedCountFor(slot.entry().entryId()) <= 0) {
                slot.setEntry(null);
            }
        }
    }

    private void ensureMinimumSlots(int forcedRows) {
        ensureSlotCount(Math.max(TRAILING_FREE_ROWS, forcedRows) * GRID_COLUMNS);
    }

    private void ensureSlotCount(int size) {
        while (this.backpackSlots.size() < size) {
            this.backpackSlots.add(new BackpackSlot());
        }
    }

    private void trimTrailingEmptySlots(int forcedRows) {
        int lastFilled = -1;
        for (int i = 0; i < this.backpackSlots.size(); i++) {
            if (!this.backpackSlots.get(i).isEmpty()) {
                lastFilled = i;
            }
        }
        int keepRows = Math.max(Math.max(TRAILING_FREE_ROWS, forcedRows), rowFromSlot(lastFilled) + 1 + TRAILING_FREE_ROWS);
        int keep = keepRows * GRID_COLUMNS;
        while (this.backpackSlots.size() > keep && this.backpackSlots.get(this.backpackSlots.size() - 1).isEmpty()) {
            this.backpackSlots.remove(this.backpackSlots.size() - 1);
        }
    }

    private int forcedRowsForPlacement(int slotIndex) {
        int currentRows = totalBackpackRows();
        int currentTrailingStartRow = Math.max(0, currentRows - TRAILING_FREE_ROWS);
        return rowFromSlot(slotIndex) >= currentTrailingStartRow ? currentRows + TRAILING_FREE_ROWS : 0;
    }

    private static int rowFromSlot(int slotIndex) {
        return slotIndex < 0 ? -1 : slotIndex / GRID_COLUMNS;
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("initialized", this.initialized);
        tag.putLong("xp", this.modXp);
        tag.putInt("level", this.modLevel);
        tag.putInt("availablePoints", this.availablePoints);
        tag.putLong("nextEntryId", this.nextEntryId);
        CompoundTag statsTag = new CompoundTag();
        for (ArkStat stat : ArkStat.values()) {
            statsTag.putInt(stat.id(), this.getAllocatedPoints(stat));
        }
        tag.put("stats", statsTag);
        CompoundTag resourcesTag = new CompoundTag();
        resourcesTag.putDouble("stamina", this.stamina);
        resourcesTag.putDouble("oxygen", this.oxygen);
        resourcesTag.putDouble("food", this.food);
        resourcesTag.putDouble("water", this.water);
        tag.put("resources", resourcesTag);
        ListTag backpackTag = new ListTag();
        for (BackpackSlot slot : this.backpackSlots) {
            backpackTag.add(slot.save(provider));
        }
        tag.put("backpack", backpackTag);
        ListTag hotbarTag = new ListTag();
        for (HotbarBinding binding : this.hotbarBindings) {
            hotbarTag.add(binding.save(provider));
        }
        tag.put("hotbarBindings", hotbarTag);
        tag.putInt("scrollOffset", this.scrollOffset);
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        this.initialized = tag.getBoolean("initialized");
        this.modXp = tag.getLong("xp");
        this.modLevel = tag.getInt("level");
        this.availablePoints = tag.getInt("availablePoints");
        this.nextEntryId = Math.max(1L, tag.getLong("nextEntryId"));
        for (ArkStat stat : ArkStat.values()) {
            this.allocatedPoints.put(stat, 0);
        }
        CompoundTag statsTag = tag.getCompound("stats");
        for (ArkStat stat : ArkStat.values()) {
            if (statsTag.contains(stat.id())) {
                this.allocatedPoints.put(stat, statsTag.getInt(stat.id()));
            }
        }
        CompoundTag resourcesTag = tag.getCompound("resources");
        this.stamina = resourcesTag.getDouble("stamina");
        this.oxygen = resourcesTag.getDouble("oxygen");
        this.food = resourcesTag.getDouble("food");
        this.water = resourcesTag.getDouble("water");
        this.backpackSlots.clear();
        ListTag backpackTag = tag.getList("backpack", 10);
        for (int i = 0; i < backpackTag.size(); i++) {
            this.backpackSlots.add(BackpackSlot.load(provider, backpackTag.getCompound(i)));
        }
        for (int i = 0; i < HOTBAR_SIZE; i++) {
            this.hotbarBindings[i] = new HotbarBinding();
        }
        ListTag hotbarTag = tag.getList("hotbarBindings", 10);
        for (int i = 0; i < Math.min(hotbarTag.size(), HOTBAR_SIZE); i++) {
            this.hotbarBindings[i] = HotbarBinding.load(provider, hotbarTag.getCompound(i));
        }
        this.scrollOffset = tag.getInt("scrollOffset");
        this.normalize();
        this.dirty = false;
    }

    public record ItemStackResult(int inserted, ItemStack remainder, BackpackEntry targetEntry) {
    }
}
