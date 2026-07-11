package org.bsdevelopment.servermaster.utils;

import atlantafx.base.layout.InputGroup;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.TextAlignment;
import javafx.scene.transform.Scale;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;

import java.util.function.Consumer;

public class FX {
    private static final String DIALOG_SURFACE = "dialog.surface";
    private static final String DIALOG_WIDTH = "dialog.width";
    private static final String DIALOG_HEIGHT = "dialog.height";

    public static void addStyleSheet (Scene scene) {
        scene.getStylesheets().add("/css/console.css");
        scene.getStylesheets().add("/css/outline.css");
        scene.getStylesheets().add("/css/theme.css");
    }

    public static Scene buildDialogScene(Stage stage, Region surface, double designWidth, double designHeight) {
        surface.setMinSize(designWidth, designHeight);
        surface.setPrefSize(designWidth, designHeight);
        surface.setMaxSize(designWidth, designHeight);

        var scaler = new Group(surface);
        var scene = new Scene(scaler, designWidth, designHeight);
        scene.setFill(Color.TRANSPARENT);
        addStyleSheet(scene);

        stage.setScene(scene);
        stage.getProperties().put(DIALOG_SURFACE, surface);
        stage.getProperties().put(DIALOG_WIDTH, designWidth);
        stage.getProperties().put(DIALOG_HEIGHT, designHeight);
        return scene;
    }

    public static void showDialog(Stage stage) {
        Object storedSurface = stage.getProperties().get(DIALOG_SURFACE);
        double designWidth = (double) stage.getProperties().getOrDefault(DIALOG_WIDTH, 0.0);
        double designHeight = (double) stage.getProperties().getOrDefault(DIALOG_HEIGHT, 0.0);

        if (storedSurface instanceof Region surface && designWidth > 0 && designHeight > 0) {
            Rectangle2D area = ownerArea(stage);

            double margin = 32;
            double availableWidth = Math.max(200, area.getWidth() - margin);
            double availableHeight = Math.max(160, area.getHeight() - margin);

            double scale = Math.min(1.0, Math.min(availableWidth / designWidth, availableHeight / designHeight));

            surface.getTransforms().removeIf(transform -> transform instanceof Scale);
            if (scale < 1.0) surface.getTransforms().add(new Scale(scale, scale, 0, 0));

            double width = designWidth * scale;
            double height = designHeight * scale;

            stage.setWidth(width);
            stage.setHeight(height);
            stage.setX(area.getMinX() + (area.getWidth() - width) / 2);
            stage.setY(area.getMinY() + (area.getHeight() - height) / 2);
        }

        stage.show();
    }

    private static Rectangle2D ownerArea(Stage stage) {
        Window owner = stage.getOwner();
        if (owner != null && owner.getWidth() > 0 && owner.getHeight() > 0
                && !Double.isNaN(owner.getX()) && !Double.isNaN(owner.getY())) {
            return new Rectangle2D(owner.getX(), owner.getY(), owner.getWidth(), owner.getHeight());
        }
        return Screen.getPrimary().getVisualBounds();
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
