package org.bsdevelopment.servermaster.utils;

import atlantafx.base.layout.InputGroup;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;

import java.util.function.Consumer;

public class FX {
    public static void addStyleSheet (Scene scene) {
        scene.getStylesheets().add("/css/console.css");
        scene.getStylesheets().add("/css/outline.css");
    }

    public static InputGroup inputGroup (Node... nodes) {
        var group = new InputGroup(nodes);
        group.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(group, Priority.ALWAYS);
        return group;
    }

    public static <T extends Pane> T vbox(double spacing, Consumer<VBox> init) {
        VBox box = new VBox(spacing);
        init.accept(box);
        return (T) box;
    }
    
    public static <T extends Pane> T hbox(double spacing, Consumer<HBox> init) {
        HBox box = new HBox(spacing);
        init.accept(box);
        return (T) box;
    }

    public static Label label(String text, Consumer<Label> init) {
        Label lbl = new Label(text);
        init.accept(lbl);
        return lbl;
    }

    public static Label createLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("header-label");
        label.setGraphicTextGap(2);
        return label;
    }

    public static Label createLabel(String text, Consumer<Label> labelConsumer) {
        Label label = new Label(text);
        label.getStyleClass().add("header-label");
        label.setGraphicTextGap(2);
        labelConsumer.accept(label);
        return label;
    }

    public static <T extends Node> T clipRounded(T node, double radius) {
        Rectangle clip = new Rectangle();
        clip.setArcWidth(radius * 2);
        clip.setArcHeight(radius * 2);
        node.setClip(clip);
        // keep clip size in sync with node
        node.layoutBoundsProperty().addListener((obs, oldB, newB) -> {
            clip.setWidth(newB.getWidth());
            clip.setHeight(newB.getHeight());
        });
        return node;
    }

    public static <T extends Node> T createTooltip (T node, String text) {
        Tooltip tooltip = new Tooltip(text);
        tooltip.setHideDelay(Duration.ZERO);
        tooltip.setShowDelay(Duration.ZERO);
        tooltip.setTextAlignment(TextAlignment.CENTER);
        Tooltip.install(node, tooltip);
        return node;
    }
}
