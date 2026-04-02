package com.qing_ying.ark.data;

import com.qing_ying.ark.Ark;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ArkAttachments {
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Ark.MOD_ID);

    public static final java.util.function.Supplier<AttachmentType<PlayerArkState>> PLAYER_STATE = ATTACHMENTS.register(
            "player_state",
            () -> AttachmentType.serializable(PlayerArkState::new).sync(PlayerArkState.STREAM_CODEC).build()
    );

    private ArkAttachments() {
    }

    public static void register(IEventBus modEventBus) {
        ATTACHMENTS.register(modEventBus);
    }

    public static PlayerArkState get(Player player) {
        PlayerArkState state = player.getData(PLAYER_STATE);
        state.ensureInitialized();
        return state;
    }

    public static void sync(ServerPlayer player) {
        get(player).ensureInitialized();
        player.syncData(PLAYER_STATE);
        get(player).clearDirty();
    }
}