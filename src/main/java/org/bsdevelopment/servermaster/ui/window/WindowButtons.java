package org.bsdevelopment.servermaster.ui.window;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.bsdevelopment.servermaster.ServerMasterApp;
import org.bsdevelopment.servermaster.instance.server.ServerHandlerAPI;
import org.bsdevelopment.servermaster.utils.FX;

public final class WindowButtons extends HBox {
    private double dragOffsetX;
    private double dragOffsetY;
    private boolean isMaximized = false;
    private boolean dragStartedMaximized = false;
    private double preMaxX, preMaxY, preMaxW, preMaxH;

    public WindowButtons(Stage stage, boolean showMinimize){
        this(stage, showMinimize, () -> {});
    }
    public WindowButtons(Stage stage, boolean showMinimize, Runnable onClose) {
        setAlignment(Pos.CENTER_RIGHT);
        setSpacing(8);
        setPadding(new Insets(8, 10, 8, 10));
        setMinHeight(34);

        var spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        var minimize = circle(Color.web("#febc2e"));
        minimize.setOnMouseClicked(e -> stage.setIconified(true));
        FX.createTooltip(minimize, "Minimize Window");

        var maximize = circle(Color.web("#77D84B"));
        maximize.setOnMouseClicked(e -> {
            if (isMaximized) {
                stage.setX(preMaxX);
                stage.setY(preMaxY);
                stage.setWidth(preMaxW);
                stage.setHeight(preMaxH);
                isMaximized = false;
            } else {
                preMaxX = stage.getX();
                preMaxY = stage.getY();
                preMaxW = stage.getWidth();
                preMaxH = stage.getHeight();

                var screens = Screen.getScreensForRectangle(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight());
                Rectangle2D bounds = (screens.isEmpty() ? Screen.getPrimary() : screens.get(0)).getVisualBounds();

                stage.setX(bounds.getMinX());
                stage.setY(bounds.getMinY());
                stage.setWidth(bounds.getWidth());
                stage.setHeight(bounds.getHeight());
                isMaximized = true;
            }
        });
        FX.createTooltip(maximize, "Maximize Window");

        var close = circle(Color.web("#ff5f57"));
        close.setOnMouseClicked(e -> {
            try {
                if (ServerMasterApp.serverWrapper().isServerRunning()) ServerHandlerAPI.killServer();
                ServerMasterApp.stopBuildToolsIfRunning();
                onClose.run();
            }catch (Exception ignored) {}

            stage.close();
        });
        FX.createTooltip(close, "Close Window");

        getChildren().add(spacer);
        if (showMinimize) getChildren().addAll(minimize, maximize);
        getChildren().add(close);

        setCursor(Cursor.OPEN_HAND);

        setOnMousePressed(e -> {
            dragOffsetX = e.getSceneX();
            dragOffsetY = e.getSceneY();
            dragStartedMaximized = isMaximized;
            setCursor(Cursor.CLOSED_HAND);
        });

        setOnMouseReleased(e -> {
            setCursor(Cursor.OPEN_HAND);
            if (dragStartedMaximized) {
                // Re-maximize on whichever screen the window was dropped onto
                var screens = Screen.getScreensForRectangle(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight());
                Rectangle2D bounds = (screens.isEmpty() ? Screen.getPrimary() : screens.get(0)).getVisualBounds();
                preMaxX = stage.getX();
                preMaxY = stage.getY();
                stage.setX(bounds.getMinX());
                stage.setY(bounds.getMinY());
                stage.setWidth(bounds.getWidth());
                stage.setHeight(bounds.getHeight());
                isMaximized = true;
                dragStartedMaximized = false;
            }
        });

        setOnMouseDragged(e -> {
            if (dragStartedMaximized && isMaximized) {
                // Restore to pre-maximize size, keeping cursor proportionally positioned
                double ratio = e.getSceneX() / stage.getWidth();
                isMaximized = false;
                stage.setWidth(preMaxW);
                stage.setHeight(preMaxH);
                dragOffsetX = preMaxW * ratio;
                dragOffsetY = e.getSceneY();
            }
            stage.setX(e.getScreenX() - dragOffsetX);
            stage.setY(e.getScreenY() - dragOffsetY);
        });
    }

    private static Circle circle(Color color) {
        var c = new Circle(8, color);
        c.setStroke(Color.rgb(0, 0, 0, 0.35));
        c.setCursor(Cursor.HAND);
        return c;
    }
}
