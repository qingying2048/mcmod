package com.qing_ying.ark.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;

public final class HotbarBinding {
    private long entryId;
    private int reservedCount;

    public HotbarBinding() {
        this(0L, 0);
    }

    public HotbarBinding(long entryId, int reservedCount) {
        this.entryId = Math.max(0L, entryId);
        this.reservedCount = Math.max(0, reservedCount);
    }

    public long entryId() {
        return entryId;
    }

    public int reservedCount() {
        return reservedCount;
    }

    public boolean isBound() {
        return this.entryId > 0L;
    }

    public void bind(long entryId) {
        this.entryId = Math.max(0L, entryId);
        this.reservedCount = 0;
    }

    public void setReservedCount(int reservedCount) {
        this.reservedCount = Math.max(0, reservedCount);
    }

    public void clear() {
        this.entryId = 0L;
        this.reservedCount = 0;
    }

    public CompoundTag save(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putLong("entryId", this.entryId);
        tag.putInt("reservedCount", this.reservedCount);
        return tag;
    }

    public static HotbarBinding load(HolderLookup.Provider provider, CompoundTag tag) {
        int reservedCount = tag.contains("reservedCount") ? tag.getInt("reservedCount") : tag.getInt("syncedCount");
        return new HotbarBinding(tag.getLong("entryId"), reservedCount);
    }

    public void write(RegistryFriendlyByteBuf buf) {
        buf.writeVarLong(this.entryId);
        buf.writeVarInt(this.reservedCount);
    }

    public static HotbarBinding read(RegistryFriendlyByteBuf buf) {
        return new HotbarBinding(buf.readVarLong(), buf.readVarInt());
    }
}
