package com.qing_ying.ark.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;

public final class BackpackSlot {
    private BackpackEntry entry;

    public BackpackSlot() {
        this(null);
    }

    public BackpackSlot(BackpackEntry entry) {
        this.entry = entry;
    }

    public boolean isEmpty() {
        return this.entry == null || this.entry.template().isEmpty();
    }

    public BackpackEntry entry() {
        return this.entry;
    }

    public void setEntry(BackpackEntry entry) {
        this.entry = entry;
    }

    public BackpackEntry removeEntry() {
        BackpackEntry removed = this.entry;
        this.entry = null;
        return removed;
    }

    public CompoundTag save(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("empty", this.isEmpty());
        if (!this.isEmpty()) {
            tag.put("entry", this.entry.save(provider));
        }
        return tag;
    }

    public static BackpackSlot load(HolderLookup.Provider provider, CompoundTag tag) {
        if (tag.getBoolean("empty")) {
            return new BackpackSlot();
        }
        if (tag.contains("entry", 10)) {
            return new BackpackSlot(BackpackEntry.load(provider, tag.getCompound("entry")));
        }
        if (tag.contains("stack", 10)) {
            return new BackpackSlot(BackpackEntry.load(provider, tag));
        }
        return new BackpackSlot();
    }

    public void write(RegistryFriendlyByteBuf buf) {
        buf.writeBoolean(!this.isEmpty());
        if (!this.isEmpty()) {
            this.entry.write(buf);
        }
    }

    public static BackpackSlot read(RegistryFriendlyByteBuf buf) {
        if (!buf.readBoolean()) {
            return new BackpackSlot();
        }
        return new BackpackSlot(BackpackEntry.read(buf));
    }
}
