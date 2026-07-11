package org.bsdevelopment.servermaster.components;

import atlantafx.base.theme.Styles;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import org.bsdevelopment.servermaster.ui.window.WindowButtons;

public final class TopBar extends HBox {

    private double dragOffsetX;
    private double dragOffsetY;

    public TopBar(Stage stage, Node tabs, Runnable onClose) {
        setMinHeight(48);
        setPrefHeight(48);
        setMaxHeight(48);
        setAlignment(Pos.CENTER_LEFT);
        getStyleClass().add("top-bar");

        var title = new Label("ServerMaster");
        title.getStyleClass().add("app-title");

        String rawVersion = TopBar.class.getPackage().getImplementationVersion();
        var version = new Label(rawVersion != null ? "v" + rawVersion : "Development Build");
        version.getStyleClass().addAll(Styles.TEXT_MUTED, Styles.TEXT_SMALL);

        var branding = new HBox(8, title, version);
        branding.setAlignment(Pos.CENTER_LEFT);
        branding.setPadding(new Insets(0, 4, 0, 16));

        var left = new HBox(18, branding, tabs);
        left.setAlignment(Pos.CENTER_LEFT);

        var spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        installDrag(stage, branding);
        installDrag(stage, spacer);

        getChildren().addAll(left, spacer, new WindowButtons(stage, true, onClose));
    }

    private void installDrag(Stage stage, Node handle) {
        handle.setOnMousePressed(e -> {
            dragOffsetX = e.getSceneX();
            dragOffsetY = e.getSceneY();
        });
        handle.setOnMouseDragged(e -> {
            stage.setX(e.getScreenX() - dragOffsetX);
            stage.setY(e.getScreenY() - dragOffsetY);
        });
    }
}
