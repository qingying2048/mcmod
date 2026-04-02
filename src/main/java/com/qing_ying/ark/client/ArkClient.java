package com.qing_ying.ark.client;

import com.qing_ying.ark.Ark;
import com.qing_ying.ark.client.screen.ArkInventoryScreen;
import com.qing_ying.ark.client.screen.ArkWrappedContainerScreen;
import com.qing_ying.ark.data.ArkAttachments;
import com.qing_ying.ark.data.PlayerArkState;
import com.qing_ying.ark.event.ArkCommonEvents;
import com.qing_ying.ark.menu.ArkMenus;
import com.qing_ying.ark.network.ArkNetworking;
import com.qing_ying.ark.stat.ArkOptionalStatController;
import com.qing_ying.ark.stat.ArkStat;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.HorseInventoryMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;

public final class ArkClient {
    private static final ResourceLocation HUD_LAYER = ResourceLocation.fromNamespaceAndPath(Ark.MOD_ID, "hud");
    private static final ResourceLocation SPEED_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(Ark.MOD_ID, "speed_bonus");

    private ArkClient() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(ArkClient::onRegisterGuiLayers);
        modEventBus.addListener(ArkClient::onRegisterMenuScreens);
        NeoForge.EVENT_BUS.addListener(ArkClient::onGuiLayerPre);
        NeoForge.EVENT_BUS.addListener(ArkClient::onScreenOpening);
        NeoForge.EVENT_BUS.addListener(ArkClient::onScreenClosing);
        NeoForge.EVENT_BUS.addListener(ArkClient::onClientTick);
        NeoForge.EVENT_BUS.addListener(ArkClient::onComputeFovModifier);
    }

    private static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, HUD_LAYER, (guiGraphics, partialTick) -> renderHud(guiGraphics));
    }

    private static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ArkMenus.ARK_INVENTORY.get(), ArkInventoryScreen::new);
    }

    private static void onGuiLayerPre(RenderGuiLayerEvent.Pre event) {
        ResourceLocation name = event.getName();
        if (VanillaGuiLayers.PLAYER_HEALTH.equals(name) || VanillaGuiLayers.FOOD_LEVEL.equals(name) || VanillaGuiLayers.AIR_LEVEL.equals(name)) {
            event.setCanceled(true);
        }
    }

    private static void onClientTick(ClientTickEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null) {
            return;
        }
        while (minecraft.options.keyInventory.consumeClick()) {
            if (minecraft.gameMode != null && minecraft.gameMode.hasInfiniteItems()) {
                minecraft.setScreen(new CreativeModeInventoryScreen(minecraft.player, minecraft.player.connection.enabledFeatures(), minecraft.options.operatorItemsTab().get()));
            } else {
                PacketDistributor.sendToServer(new ArkNetworking.OpenArkInventoryPayload());
            }
        }
        if (Screen.hasShiftDown()) {
            while (minecraft.options.keyDrop.consumeClick()) {
                PlayerArkState state = ArkAttachments.get(minecraft.player);
                int selected = minecraft.player.getInventory().selected;
                if (state.hasHotbarBinding(selected)) {
                    PacketDistributor.sendToServer(new ArkNetworking.DropHotbarBindingPayload(selected, ArkCommonEvents.DROP_TEN));
                }
            }
        }
    }

    private static void onComputeFovModifier(ComputeFovModifierEvent event) {
        Player player = event.getPlayer();
        if (player == null) {
            return;
        }
        AttributeInstance movement = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movement == null) {
            return;
        }
        AttributeModifier arkSpeed = movement.getModifier(SPEED_MODIFIER_ID);
        if (arkSpeed == null) {
            return;
        }
        double base = movement.getBaseValue();
        double addValue = 0.0D;
        double addMultBase = 0.0D;
        double addMultTotal = 1.0D;
        for (AttributeModifier modifier : movement.getModifiers()) {
            switch (modifier.operation()) {
                case ADD_VALUE -> addValue += modifier.amount();
                case ADD_MULTIPLIED_BASE -> addMultBase += modifier.amount();
                case ADD_MULTIPLIED_TOTAL -> addMultTotal *= 1.0D + modifier.amount();
            }
        }
        double walkingSpeed = player.getAbilities().getWalkingSpeed();
        if (walkingSpeed <= 0.0D) {
            return;
        }
        double currentSpeed = movement.getValue();
        double speedWithoutArk = (base + addValue) * (1.0D + (addMultBase - arkSpeed.amount())) * addMultTotal;
        if (currentSpeed <= 0.0D || speedWithoutArk <= 0.0D) {
            return;
        }
        double speedFactor = (currentSpeed / walkingSpeed + 1.0D) * 0.5D;
        double speedFactorNoArk = (speedWithoutArk / walkingSpeed + 1.0D) * 0.5D;
        if (speedFactor <= 0.0D) {
            return;
        }
        float baseModifier = event.getFovModifier();
        float adjusted = (float) (baseModifier * (speedFactorNoArk / speedFactor));
        float scale = Minecraft.getInstance().options.fovEffectScale().get().floatValue();
        event.setNewFovModifier((float) Mth.lerp(scale, 1.0F, adjusted));
    }

    private static void onScreenOpening(ScreenEvent.Opening event) {
        Screen newScreen = event.getNewScreen();
        if (!(newScreen instanceof AbstractContainerScreen<?> screen)
                || screen instanceof CreativeModeInventoryScreen
                || screen instanceof ArkInventoryScreen
                || screen instanceof ArkWrappedContainerScreen<?>) {
            return;
        }
        if (!(screen.getMenu() instanceof AbstractContainerMenu menu) || menu instanceof InventoryMenu || menu instanceof HorseInventoryMenu || menu instanceof MerchantMenu) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        event.setNewScreen(new ArkWrappedContainerScreen<>(menu, minecraft.player.getInventory(), screen.getTitle()));
    }

    private static void onScreenClosing(ScreenEvent.Closing event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen) || screen instanceof CreativeModeInventoryScreen) {
            return;
        }
        if (Minecraft.getInstance().getConnection() == null) {
            return;
        }
        if (!screen.getMenu().getCarried().isEmpty()) {
            PacketDistributor.sendToServer(new ArkNetworking.StashCarriedPayload());
        }
    }

    private static void renderHud(GuiGraphics guiGraphics) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui || minecraft.player == null) {
            return;
        }
        Player player = minecraft.player;
        PlayerArkState state = ArkAttachments.get(player);
        int x = 8;
        int y = 8;
        int width = 182;
        int height = 88;

        guiGraphics.fill(RenderType.guiOverlay(), x, y, x + width, y + height, 0xA0141B1E);
        guiGraphics.fill(RenderType.guiOverlay(), x, y, x + width, y + 2, 0xFF7FC4A6);
        guiGraphics.drawString(minecraft.font, Component.literal("ARK"), x + 8, y + 8, 0xFFF2F5E9, false);
        guiGraphics.drawString(minecraft.font, Component.translatable("screen.ark.level", state.getModLevel()), x + 8, y + 20, 0xFFBBD6C8, false);

        long neededXp = Math.max(1L, com.qing_ying.ark.config.ArkConfig.xpForNextLevel(state.getModLevel()));
        int barX = x + 8;
        int barY = y + 34;
        int barW = width - 16;
        guiGraphics.fill(RenderType.guiOverlay(), barX, barY, barX + barW, barY + 8, 0x661C2825);
        guiGraphics.fill(RenderType.guiOverlay(), barX, barY, barX + (int) (barW * (state.getModXp() / (double) neededXp)), barY + 8, 0xFF7FC4A6);
        guiGraphics.drawString(minecraft.font, state.getModXp() + "/" + neededXp, barX + 2, barY + 10, 0xFFD5E7DE, false);

        int infoY = y + 52;
        guiGraphics.drawString(minecraft.font, Component.translatable("hud.ark.health", shortNumber(player.getHealth()), shortNumber(player.getMaxHealth())), x + 8, infoY, 0xFFF6C3B9, false);
        guiGraphics.drawString(minecraft.font, Component.translatable("hud.ark.resource", ArkStat.STAMINA.displayName(), shortNumber(state.getStamina()), shortNumber(state.maxFor(ArkStat.STAMINA))), x + 8, infoY + 10, 0xFFE8E3B8, false);
        guiGraphics.drawString(minecraft.font, Component.translatable("hud.ark.resource", ArkStat.OXYGEN.displayName(), shortNumber(state.getOxygen()), shortNumber(state.maxFor(ArkStat.OXYGEN))), x + 8, infoY + 20, 0xFFB8E2F2, false);
        guiGraphics.drawString(minecraft.font, Component.translatable("hud.ark.resource", ArkStat.FOOD.displayName(), shortNumber(state.getFood()), shortNumber(state.maxFor(ArkStat.FOOD))), x + 8, infoY + 30, 0xFFF0D8A8, false);
        if (ArkOptionalStatController.isActive(ArkStat.WATER)) {
            guiGraphics.drawString(minecraft.font, Component.translatable("hud.ark.resource", ArkStat.WATER.displayName(), shortNumber(state.getWater()), shortNumber(state.maxFor(ArkStat.WATER))), x + 92, infoY + 10, 0xFF8ED4F8, false);
        }
        guiGraphics.drawString(minecraft.font, Component.translatable("hud.ark.weight", shortNumber(ArkCommonEvents.calculateTotalWeight(player, state)), shortNumber(ArkCommonEvents.weightCapacity(state))), x + 92, infoY + 20, 0xFFCECEC8, false);
        guiGraphics.drawString(minecraft.font, Component.translatable("hud.ark.points", state.getAvailablePoints()), x + 92, infoY + 30, 0xFF9EE39A, false);
    }

    static String shortNumber(double value) {
        if (value >= 1_000_000) return String.format(Locale.ROOT, "%.1fm", value / 1_000_000.0D);
        if (value >= 10_000) return String.format(Locale.ROOT, "%.1fk", value / 1_000.0D);
        if (Math.abs(value - Math.rint(value)) < 0.01D) return Integer.toString((int) Math.rint(value));
        return String.format(Locale.ROOT, "%.1f", value);
    }
}
