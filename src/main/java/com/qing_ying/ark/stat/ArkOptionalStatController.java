package com.qing_ying.ark.stat;

import java.util.EnumSet;
import java.util.Set;

public final class ArkOptionalStatController {
    private static final Set<ArkStat> ACTIVE_STATS = EnumSet.noneOf(ArkStat.class);

    private ArkOptionalStatController() {
    }

    public static boolean isActive(ArkStat stat) {
        return ACTIVE_STATS.contains(stat);
    }

    public static void activate(ArkStat stat) {
        ACTIVE_STATS.add(stat);
    }

    public static void deactivate(ArkStat stat) {
        ACTIVE_STATS.remove(stat);
    }
}
