package com.qing_ying.ark.stat;

import com.qing_ying.ark.data.PlayerArkState;
import net.minecraft.network.chat.Component;

public interface StatDefinition {
    ArkStat stat();

    String translationKey();

    boolean enabled();

    boolean dynamicResource();

    double baseValue();

    double perPointValue();

    default Component displayName() {
        return Component.translatable(translationKey());
    }

    default double valueFor(PlayerArkState state) {
        return baseValue() + state.getAllocatedPoints(stat()) * perPointValue();
    }
}