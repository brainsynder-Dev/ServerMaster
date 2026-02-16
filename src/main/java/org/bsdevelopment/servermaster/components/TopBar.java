package org.bsdevelopment.servermaster.components;

import atlantafx.base.theme.Styles;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.bsdevelopment.servermaster.ui.window.WindowButtons;

public final class TopBar extends HBox {

    private final Region leftFill;

    public TopBar(Stage stage, double sidebarWidth, Runnable onClose) {
        setMinHeight(44);
        setPrefHeight(44);
        setMaxHeight(44);

        leftFill = new Region();
        leftFill.setPrefWidth(sidebarWidth);
        leftFill.setMinWidth(sidebarWidth);
        leftFill.setMaxWidth(sidebarWidth);
        leftFill.getStyleClass().addAll(Styles.BG_SUBTLE);

        var stackPane = new StackPane(new WindowButtons(stage, true, onClose));
        stackPane.setAlignment(Pos.TOP_RIGHT);
        stackPane.setPadding(new Insets(6, 6, 0, 6));
        HBox.setHgrow(stackPane, Priority.ALWAYS);

        getChildren().addAll(leftFill, stackPane);
    }

    public void setSidebarWidth(double width) {
        leftFill.setPrefWidth(width);
        leftFill.setMinWidth(width);
        leftFill.setMaxWidth(width);
    }
}
