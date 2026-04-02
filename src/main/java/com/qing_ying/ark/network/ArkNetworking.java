package com.qing_ying.ark.network;

import com.qing_ying.ark.Ark;
import com.qing_ying.ark.event.ArkCommonEvents;
import com.qing_ying.ark.stat.ArkStat;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ArkNetworking {
    private static final String VERSION = "4";

    private ArkNetworking() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(ArkNetworking::registerPayloads);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(VERSION);
        registrar.playToServer(SpendStatPayload.TYPE, SpendStatPayload.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> {
            ArkCommonEvents.handleSpendPoint((ServerPlayer) context.player(), payload.stat());
        }));
        registrar.playToServer(BackpackGridClickPayload.TYPE, BackpackGridClickPayload.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> {
            ArkCommonEvents.handleBackpackGridClick((ServerPlayer) context.player(), payload.slotIndex(), payload.mouseButton());
        }));
        registrar.playToServer(BackpackScrollPayload.TYPE, BackpackScrollPayload.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> {
            ArkCommonEvents.handleBackpackScroll((ServerPlayer) context.player(), payload.delta());
        }));
        registrar.playToServer(StashCarriedPayload.TYPE, StashCarriedPayload.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> {
            ArkCommonEvents.stashCarried((ServerPlayer) context.player());
        }));
        registrar.playToServer(OpenArkInventoryPayload.TYPE, OpenArkInventoryPayload.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> {
            ArkCommonEvents.handleOpenArkInventory((ServerPlayer) context.player());
        }));
        registrar.playToServer(ClearHotbarBindingPayload.TYPE, ClearHotbarBindingPayload.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> {
            ArkCommonEvents.clearHotbarBinding((ServerPlayer) context.player(), payload.slot());
        }));
        registrar.playToServer(DropCarriedPayload.TYPE, DropCarriedPayload.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> {
            ArkCommonEvents.dropCarried((ServerPlayer) context.player(), payload.single());
        }));
        registrar.playToServer(DropBackpackSlotPayload.TYPE, DropBackpackSlotPayload.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> {
            ArkCommonEvents.dropBackpackSlot((ServerPlayer) context.player(), payload.slotIndex(), payload.amountMode());
        }));
        registrar.playToServer(DropHotbarBindingPayload.TYPE, DropHotbarBindingPayload.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> {
            ArkCommonEvents.dropHotbarBinding((ServerPlayer) context.player(), payload.slot(), payload.amountMode());
        }));
        registrar.playToServer(QuickMoveToBackpackPayload.TYPE, QuickMoveToBackpackPayload.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> {
            ArkCommonEvents.quickMoveToBackpack((ServerPlayer) context.player(), payload.slotIndex());
        }));
        registrar.playToServer(BindHotbarPayload.TYPE, BindHotbarPayload.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> {
            ArkCommonEvents.bindHotbarFromBackpack((ServerPlayer) context.player(), payload.backpackSlotIndex(), payload.hotbarSlot());
        }));
    }

    public record SpendStatPayload(ArkStat stat) implements CustomPacketPayload {
        public static final Type<SpendStatPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Ark.MOD_ID, "spend_stat"));
        public static final StreamCodec<RegistryFriendlyByteBuf, SpendStatPayload> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public SpendStatPayload decode(RegistryFriendlyByteBuf buf) {
                return new SpendStatPayload(ArkStat.values()[buf.readVarInt()]);
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buf, SpendStatPayload payload) {
                buf.writeVarInt(payload.stat.ordinal());
            }
        };

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record BackpackGridClickPayload(int slotIndex, int mouseButton) implements CustomPacketPayload {
        public static final Type<BackpackGridClickPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Ark.MOD_ID, "backpack_grid_click"));
        public static final StreamCodec<RegistryFriendlyByteBuf, BackpackGridClickPayload> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public BackpackGridClickPayload decode(RegistryFriendlyByteBuf buf) {
                return new BackpackGridClickPayload(buf.readVarInt(), buf.readVarInt());
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buf, BackpackGridClickPayload payload) {
                buf.writeVarInt(payload.slotIndex);
                buf.writeVarInt(payload.mouseButton);
            }
        };

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record BackpackScrollPayload(int delta) implements CustomPacketPayload {
        public static final Type<BackpackScrollPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Ark.MOD_ID, "backpack_scroll"));
        public static final StreamCodec<RegistryFriendlyByteBuf, BackpackScrollPayload> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public BackpackScrollPayload decode(RegistryFriendlyByteBuf buf) {
                return new BackpackScrollPayload(buf.readVarInt());
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buf, BackpackScrollPayload payload) {
                buf.writeVarInt(payload.delta);
            }
        };

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record StashCarriedPayload() implements CustomPacketPayload {
        public static final Type<StashCarriedPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Ark.MOD_ID, "stash_carried"));
        public static final StreamCodec<RegistryFriendlyByteBuf, StashCarriedPayload> STREAM_CODEC = StreamCodec.unit(new StashCarriedPayload());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record OpenArkInventoryPayload() implements CustomPacketPayload {
        public static final Type<OpenArkInventoryPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Ark.MOD_ID, "open_inventory"));
        public static final StreamCodec<RegistryFriendlyByteBuf, OpenArkInventoryPayload> STREAM_CODEC = StreamCodec.unit(new OpenArkInventoryPayload());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record ClearHotbarBindingPayload(int slot) implements CustomPacketPayload {
        public static final Type<ClearHotbarBindingPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Ark.MOD_ID, "clear_hotbar_binding"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ClearHotbarBindingPayload> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public ClearHotbarBindingPayload decode(RegistryFriendlyByteBuf buf) {
                return new ClearHotbarBindingPayload(buf.readVarInt());
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buf, ClearHotbarBindingPayload payload) {
                buf.writeVarInt(payload.slot);
            }
        };

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record DropCarriedPayload(boolean single) implements CustomPacketPayload {
        public static final Type<DropCarriedPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Ark.MOD_ID, "drop_carried"));
        public static final StreamCodec<RegistryFriendlyByteBuf, DropCarriedPayload> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public DropCarriedPayload decode(RegistryFriendlyByteBuf buf) {
                return new DropCarriedPayload(buf.readBoolean());
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buf, DropCarriedPayload payload) {
                buf.writeBoolean(payload.single);
            }
        };

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record DropBackpackSlotPayload(int slotIndex, int amountMode) implements CustomPacketPayload {
        public static final Type<DropBackpackSlotPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Ark.MOD_ID, "drop_backpack_slot"));
        public static final StreamCodec<RegistryFriendlyByteBuf, DropBackpackSlotPayload> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public DropBackpackSlotPayload decode(RegistryFriendlyByteBuf buf) {
                return new DropBackpackSlotPayload(buf.readVarInt(), buf.readVarInt());
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buf, DropBackpackSlotPayload payload) {
                buf.writeVarInt(payload.slotIndex);
                buf.writeVarInt(payload.amountMode);
            }
        };

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record DropHotbarBindingPayload(int slot, int amountMode) implements CustomPacketPayload {
        public static final Type<DropHotbarBindingPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Ark.MOD_ID, "drop_hotbar_binding"));
        public static final StreamCodec<RegistryFriendlyByteBuf, DropHotbarBindingPayload> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public DropHotbarBindingPayload decode(RegistryFriendlyByteBuf buf) {
                return new DropHotbarBindingPayload(buf.readVarInt(), buf.readVarInt());
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buf, DropHotbarBindingPayload payload) {
                buf.writeVarInt(payload.slot);
                buf.writeVarInt(payload.amountMode);
            }
        };

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record QuickMoveToBackpackPayload(int slotIndex) implements CustomPacketPayload {
        public static final Type<QuickMoveToBackpackPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Ark.MOD_ID, "quick_move_to_backpack"));
        public static final StreamCodec<RegistryFriendlyByteBuf, QuickMoveToBackpackPayload> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public QuickMoveToBackpackPayload decode(RegistryFriendlyByteBuf buf) {
                return new QuickMoveToBackpackPayload(buf.readVarInt());
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buf, QuickMoveToBackpackPayload payload) {
                buf.writeVarInt(payload.slotIndex);
            }
        };

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record BindHotbarPayload(int backpackSlotIndex, int hotbarSlot) implements CustomPacketPayload {
        public static final Type<BindHotbarPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Ark.MOD_ID, "bind_hotbar"));
        public static final StreamCodec<RegistryFriendlyByteBuf, BindHotbarPayload> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public BindHotbarPayload decode(RegistryFriendlyByteBuf buf) {
                return new BindHotbarPayload(buf.readVarInt(), buf.readVarInt());
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buf, BindHotbarPayload payload) {
                buf.writeVarInt(payload.backpackSlotIndex);
                buf.writeVarInt(payload.hotbarSlot);
            }
        };

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
