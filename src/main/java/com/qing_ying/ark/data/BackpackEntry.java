package com.qing_ying.ark.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

public final class BackpackEntry {
    private final long entryId;
    private ItemStack template;
    private int storedCount;

    public BackpackEntry(long entryId, ItemStack template, int storedCount) {
        this.entryId = entryId;
        this.template = template.copyWithCount(1);
        this.storedCount = Math.max(0, storedCount);
    }

    public long entryId() {
        return entryId;
    }

    public ItemStack template() {
        return template;
    }

    public void setTemplate(ItemStack template) {
        if (!template.isEmpty()) {
            this.template = template.copyWithCount(1);
        }
    }

    public int storedCount() {
        return storedCount;
    }

    public void setStoredCount(int storedCount) {
        this.storedCount = Math.max(0, storedCount);
    }

    public boolean isEmpty() {
        return this.template.isEmpty();
    }

    public boolean matches(ItemStack stack) {
        return !stack.isEmpty() && ItemStack.isSameItemSameComponents(this.template, stack);
    }

    public boolean sameItem(ItemStack stack) {
        return !stack.isEmpty() && this.template.is(stack.getItem());
    }

    public int insert(ItemStack stack) {
        if (!matches(stack) || this.storedCount >= Integer.MAX_VALUE) {
            return 0;
        }
        int moved = Math.min(Integer.MAX_VALUE - this.storedCount, stack.getCount());
        this.storedCount += moved;
        return moved;
    }

    public ItemStack extract(int amount) {
        int extracted = Math.min(Math.max(0, amount), this.storedCount);
        this.storedCount -= extracted;
        return extracted <= 0 ? ItemStack.EMPTY : this.template.copyWithCount(extracted);
    }

    public ItemStack asDisplayStack() {
        if (this.template.isEmpty()) {
            return ItemStack.EMPTY;
        }
        int visibleCount = Math.max(1, this.storedCount);
        return this.template.copyWithCount(Math.min(Math.max(1, this.template.getMaxStackSize()), visibleCount));
    }

    public ItemStack asCarriedStack() {
        return this.template.copyWithCount(this.storedCount);
    }

    public CompoundTag save(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putLong("id", this.entryId);
        tag.put("stack", this.template.save(provider));
        tag.putInt("count", this.storedCount);
        return tag;
    }

    public static BackpackEntry load(HolderLookup.Provider provider, CompoundTag tag) {
        ItemStack stack = ItemStack.parseOptional(provider, tag.getCompound("stack"));
        return new BackpackEntry(tag.getLong("id"), stack, tag.getInt("count"));
    }

    public void write(RegistryFriendlyByteBuf buf) {
        buf.writeVarLong(this.entryId);
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, this.template);
        buf.writeVarInt(this.storedCount);
    }

    public static BackpackEntry read(RegistryFriendlyByteBuf buf) {
        long entryId = buf.readVarLong();
        ItemStack stack = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
        int count = buf.readVarInt();
        return new BackpackEntry(entryId, stack, count);
    }
}
