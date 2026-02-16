package org.bsdevelopment.servermaster.ui.window;

import javafx.beans.value.ChangeListener;
import javafx.scene.layout.BorderPane;
import javafx.scene.shape.Rectangle;

public final class WindowSurface extends BorderPane {
    private static final double RADIUS = 12;

    public WindowSurface() {
        getStyleClass().add("window-surface");

        setStyle("""
            -fx-background-radius: 12;
            -fx-border-radius: 12;
        """);

        var clip = new Rectangle();
        clip.setArcWidth(RADIUS * 2);
        clip.setArcHeight(RADIUS * 2);
        setClip(clip);

        ChangeListener<Number> resizer = (obs, oldV, newV) -> {
            clip.setWidth(getWidth());
            clip.setHeight(getHeight());
        };
        widthProperty().addListener(resizer);
        heightProperty().addListener(resizer);
    }
}
