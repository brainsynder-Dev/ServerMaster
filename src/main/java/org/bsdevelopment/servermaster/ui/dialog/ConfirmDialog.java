package org.bsdevelopment.servermaster.ui.dialog;

import atlantafx.base.theme.Styles;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.bsdevelopment.servermaster.ui.window.WindowButtons;
import org.bsdevelopment.servermaster.ui.window.WindowSurface;
import org.bsdevelopment.servermaster.utils.FX;

public class ConfirmDialog {
    private final Stage stage;

    public ConfirmDialog(Stage owner, String title, String message, String confirmText, Runnable onConfirm) {
        stage = new Stage();
        stage.initOwner(owner);
        stage.initModality(Modality.WINDOW_MODAL);
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setTitle(title);

        var windowButtons = new WindowButtons(stage, false);
        windowButtons.setStyle("-fx-background-color: transparent;");

        var heading = new Label(title);
        heading.getStyleClass().addAll(Styles.TITLE_3, Styles.DANGER);

        var body = new Label(message);
        body.getStyleClass().addAll(Styles.TEXT_MUTED);
        body.setWrapText(true);
        body.setMaxWidth(Double.MAX_VALUE);

        var cancel = new Button("CANCEL");
        cancel.getStyleClass().addAll(Styles.BUTTON_OUTLINED);
        cancel.setOnAction(e -> stage.close());

        var confirm = new Button(confirmText);
        confirm.getStyleClass().addAll(Styles.BUTTON_OUTLINED, Styles.DANGER);
        confirm.setOnAction(e -> {
            stage.close();
            if (onConfirm != null) onConfirm.run();
        });

        var footer = new HBox(10, cancel, spacer(), confirm);
        footer.setAlignment(Pos.CENTER_LEFT);

        var content = new VBox(14, heading, body, footer);
        content.setPadding(new Insets(6, 18, 18, 18));

        var surface = new WindowSurface();
        surface.getStyleClass().add("dialog");
        surface.setTop(windowButtons);
        surface.setCenter(content);
        BorderPane.setMargin(windowButtons, new Insets(6, 6, 0, 6));

        FX.buildDialogScene(stage, surface, 440, 240);
    }

    public void show() {
        FX.showDialog(stage);
    }

    private static Region spacer() {
        var region = new Region();
        HBox.setHgrow(region, Priority.ALWAYS);
        return region;
    }
}
