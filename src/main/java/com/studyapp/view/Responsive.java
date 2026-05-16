package com.studyapp.view;

import javafx.geometry.Insets;
import javafx.geometry.Rectangle2D;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Screen;

final class Responsive {
    private static final double BASE_WIDTH = 1920.0;
    private static final double BASE_HEIGHT = 1080.0;
    private static final double MIN_SCALE = 0.85;
    private static final double MAX_SCALE = 1.25;

    private Responsive() {
    }

    static double scale() {
        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        double widthScale = bounds.getWidth() / BASE_WIDTH;
        double heightScale = bounds.getHeight() / BASE_HEIGHT;
        double scale = Math.min(widthScale, heightScale);
        return Math.max(MIN_SCALE, Math.min(MAX_SCALE, scale));
    }

    static double size(double value) {
        return Math.round(value * scale());
    }

    static Font font(String family, double size) {
        return Font.font(family, size(size));
    }

    static Font font(String family, FontWeight weight, double size) {
        return Font.font(family, weight, size(size));
    }

    static Insets insets(double all) {
        return new Insets(size(all));
    }

    static Insets insets(double top, double right, double bottom, double left) {
        return new Insets(size(top), size(right), size(bottom), size(left));
    }
}
