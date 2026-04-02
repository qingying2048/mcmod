package com.qing_ying.ark.menu;

import com.qing_ying.ark.Ark;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ArkMenus {
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, Ark.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<ArkInventoryMenu>> ARK_INVENTORY = MENUS.register(
            "ark_inventory",
            () -> IMenuTypeExtension.create((containerId, inventory, data) -> new ArkInventoryMenu(containerId, inventory))
    );

    private ArkMenus() {
    }

    public static void register(IEventBus modEventBus) {
        MENUS.register(modEventBus);
    }
}
