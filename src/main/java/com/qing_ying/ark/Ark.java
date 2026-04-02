package com.qing_ying.ark;

import com.mojang.logging.LogUtils;
import com.qing_ying.ark.client.ArkClient;
import com.qing_ying.ark.config.ArkConfig;
import com.qing_ying.ark.data.ArkAttachments;
import com.qing_ying.ark.event.ArkCommonEvents;
import com.qing_ying.ark.menu.ArkMenus;
import com.qing_ying.ark.network.ArkNetworking;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import org.slf4j.Logger;

@Mod(Ark.MOD_ID)
public final class Ark {
    public static final String MOD_ID = "ark";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Ark(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, ArkConfig.SPEC);
        ArkAttachments.register(modEventBus);
        ArkMenus.register(modEventBus);
        ArkNetworking.register(modEventBus);
        ArkCommonEvents.register();
        if (FMLEnvironment.dist.isClient()) {
            ArkClient.register(modEventBus);
        }
        LOGGER.info("Ark initialized");
    }
}
