package com.qing_ying.ark.stat;

import com.qing_ying.ark.config.ArkConfig;

public enum ArkStat implements StatDefinition {
    HEALTH("health", "stat.ark.health", true, false),
    STAMINA("stamina", "stat.ark.stamina", true, true),
    OXYGEN("oxygen", "stat.ark.oxygen", true, true),
    FOOD("food", "stat.ark.food", true, true),
    WATER("water", "stat.ark.water", false, true),
    WEIGHT("weight", "stat.ark.weight", true, false),
    MELEE_DAMAGE("melee_damage", "stat.ark.melee_damage", true, false),
    MOVE_SPEED("move_speed", "stat.ark.move_speed", true, false),
    RESISTANCE("resistance", "stat.ark.resistance", false, false);

    private final String id;
    private final String translationKey;
    private final boolean enabled;
    private final boolean dynamicResource;

    ArkStat(String id, String translationKey, boolean enabled, boolean dynamicResource) {
        this.id = id;
        this.translationKey = translationKey;
        this.enabled = enabled;
        this.dynamicResource = dynamicResource;
    }

    public String id() {
        return id;
    }

    public static ArkStat byId(String id) {
        for (ArkStat stat : values()) {
            if (stat.id.equals(id)) {
                return stat;
            }
        }
        throw new IllegalArgumentException("Unknown Ark stat id: " + id);
    }

    @Override
    public ArkStat stat() {
        return this;
    }

    @Override
    public String translationKey() {
        return translationKey;
    }

    @Override
    public boolean enabled() {
        return enabled || ArkOptionalStatController.isActive(this);
    }

    @Override
    public boolean dynamicResource() {
        return dynamicResource;
    }

    @Override
    public double baseValue() {
        return switch (this) {
            case HEALTH -> ArkConfig.healthBase();
            case STAMINA -> ArkConfig.staminaBase();
            case OXYGEN -> ArkConfig.oxygenBase();
            case FOOD -> ArkConfig.foodBase();
            case WATER -> ArkConfig.waterBase();
            case WEIGHT -> ArkConfig.weightBase();
            case MELEE_DAMAGE, MOVE_SPEED, RESISTANCE -> 0.0D;
        };
    }

    @Override
    public double perPointValue() {
        return switch (this) {
            case HEALTH -> ArkConfig.healthPerPoint();
            case STAMINA -> ArkConfig.staminaPerPoint();
            case OXYGEN -> ArkConfig.oxygenPerPoint();
            case FOOD -> ArkConfig.foodPerPoint();
            case WATER -> ArkConfig.waterPerPoint();
            case WEIGHT -> ArkConfig.weightPerPoint();
            case MELEE_DAMAGE -> ArkConfig.meleePerPoint();
            case MOVE_SPEED -> ArkConfig.speedPerPoint();
            case RESISTANCE -> 0.0D;
        };
    }
}
