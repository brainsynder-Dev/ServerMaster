package org.bsdevelopment.servermaster.ui;

import atlantafx.base.theme.Styles;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.bsdevelopment.servermaster.ui.window.WindowButtons;
import org.bsdevelopment.servermaster.ui.window.WindowSurface;
import org.bsdevelopment.servermaster.utils.FX;
import org.bsdevelopment.servermaster.utils.Outline;

public final class UpdateFoundWindow {
    private final Stage stage;

    public UpdateFoundWindow(String newVersion, Runnable onUpdate, Runnable onContinue) {
        stage = new Stage();
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setTitle("ServerMaster");

        var windowButtons = new WindowButtons(stage, false);
        windowButtons.setStyle("-fx-background-color: transparent;");

        var title = new Label("ServerMaster");
        title.getStyleClass().addAll(Styles.TITLE_1, Styles.DANGER);
        title.setAlignment(Pos.TOP_CENTER);

        var subtitle = new Label("UPDATE DETECTED");
        subtitle.getStyleClass().addAll(Styles.TEXT_BOLD, Styles.DANGER);

        var versionLabel = new Label("Version " + newVersion + " is available");
        versionLabel.getStyleClass().add(Styles.TEXT_MUTED);

        var updateBtn = new Button("UPDATE");
        updateBtn.getStyleClass().addAll(Styles.BUTTON_OUTLINED, Styles.ACCENT);
        updateBtn.setMaxWidth(Double.MAX_VALUE);
        updateBtn.setOnAction(e -> {
            stage.close();
            onUpdate.run();
        });

        var contBtn = new Button("CONTINUE");
        contBtn.getStyleClass().addAll(Styles.BUTTON_OUTLINED);
        contBtn.setMaxWidth(Double.MAX_VALUE);
        contBtn.setOnAction(e -> {
            stage.close();
            onContinue.run();
        });

        var titleBox = new VBox(0, title, subtitle);
        titleBox.setAlignment(Pos.TOP_CENTER);

        var content = new VBox(12, titleBox, versionLabel, updateBtn, contBtn);
        content.setPadding(new Insets(0, 16, 16, 16));
        content.setAlignment(Pos.CENTER);

        var surface = new WindowSurface();
        surface.setTop(windowButtons);
        surface.setCenter(content);
        surface.getStyleClass().add(Outline.DANGER.getCssClass());
        BorderPane.setMargin(windowButtons, new Insets(6, 6, 0, 6));

        FX.buildDialogScene(stage, surface, 420, 280);
    }

    public void show() {
        FX.showDialog(stage);
    }
}
