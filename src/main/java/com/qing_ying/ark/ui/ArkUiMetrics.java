package com.qing_ying.ark.ui;

import net.minecraft.util.Mth;

public final class ArkUiMetrics {
    private final double scale;
    private final int frameLeft;
    private final int frameTop;
    private final int scaledWidth;
    private final int scaledHeight;

    private ArkUiMetrics(double scale, int frameLeft, int frameTop, int scaledWidth, int scaledHeight) {
        this.scale = scale;
        this.frameLeft = frameLeft;
        this.frameTop = frameTop;
        this.scaledWidth = scaledWidth;
        this.scaledHeight = scaledHeight;
    }

    public static ArkUiMetrics create(int windowWidth, int windowHeight) {
        double availableWidth = Math.max(1.0D, windowWidth - ArkUiLayout.WINDOW_MARGIN * 2.0D);
        double availableHeight = Math.max(1.0D, windowHeight - ArkUiLayout.WINDOW_MARGIN * 2.0D);
        double scale = Math.min(1.0D, Math.min(availableWidth / ArkUiLayout.FRAME_WIDTH, availableHeight / ArkUiLayout.FRAME_HEIGHT));
        int scaledWidth = Math.max(1, Mth.floor(ArkUiLayout.FRAME_WIDTH * scale));
        int scaledHeight = Math.max(1, Mth.floor(ArkUiLayout.FRAME_HEIGHT * scale));
        int frameLeft = (windowWidth - scaledWidth) / 2;
        int frameTop = (windowHeight - scaledHeight) / 2;
        return new ArkUiMetrics(scale, frameLeft, frameTop, scaledWidth, scaledHeight);
    }

    public double scale() {
        return this.scale;
    }

    public float scaleF() {
        return (float) this.scale;
    }

    public int frameLeft() {
        return this.frameLeft;
    }

    public int frameTop() {
        return this.frameTop;
    }

    public int scaledWidth() {
        return this.scaledWidth;
    }

    public int scaledHeight() {
        return this.scaledHeight;
    }

    public double toLocalX(double mouseX) {
        return (mouseX - this.frameLeft) / this.scale;
    }

    public double toLocalY(double mouseY) {
        return (mouseY - this.frameTop) / this.scale;
    }

    public boolean contains(double mouseX, double mouseY) {
        return mouseX >= this.frameLeft
                && mouseY >= this.frameTop
                && mouseX < this.frameLeft + this.scaledWidth
                && mouseY < this.frameTop + this.scaledHeight;
    }
}
