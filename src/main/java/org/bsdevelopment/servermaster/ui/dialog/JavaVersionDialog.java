package org.bsdevelopment.servermaster.ui.dialog;

import atlantafx.base.theme.Styles;
import com.jeff_media.javafinder.JavaInstallation;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.bsdevelopment.servermaster.Constants;
import org.bsdevelopment.servermaster.config.SettingsService;
import org.bsdevelopment.servermaster.ui.window.WindowButtons;
import org.bsdevelopment.servermaster.ui.window.WindowSurface;
import org.bsdevelopment.servermaster.utils.FX;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2MZ;

import java.util.ArrayList;
import java.util.Objects;

public final class JavaVersionDialog {

    private final Stage stage;

    public JavaVersionDialog(Stage owner, Runnable onSelect) {
        stage = new Stage();
        stage.initOwner(owner);
        stage.initModality(Modality.WINDOW_MODAL);
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setTitle("Select a Java Version");

        var windowButtons = new WindowButtons(stage, false);
        windowButtons.setStyle("-fx-background-color: transparent;");

        var title = new Label("Select a Java Version");
        title.getStyleClass().addAll(Styles.TITLE_3);

        var subtitle = new Label("Choose an installed Java runtime from your PC.");
        subtitle.getStyleClass().addAll(Styles.TEXT_MUTED);

        var table = createTable();
        table.setItems(FXCollections.observableArrayList(new ArrayList<>(Constants.JAVA_MANAGER.getInstallations())));

        var selectBtn = new Button("Select");
        selectBtn.getStyleClass().addAll(Styles.BUTTON_OUTLINED, Styles.ACCENT);
        selectBtn.setDisable(true);

        var closeBtn = new Button("Close");
        closeBtn.getStyleClass().addAll(Styles.BUTTON_OUTLINED, Styles.DANGER);
        closeBtn.setOnAction(e -> stage.close());

        table.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            selectBtn.setDisable(newV == null);
        });

        selectBtn.setOnAction(e -> {
            JavaInstallation selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) return;

            SettingsService.get().setJavaPath(selected.getJavaExecutable().toPath());
            SettingsService.save();

            onSelect.run();

            stage.close();
        });

        var footer = new HBox(10, closeBtn, spacer(), selectBtn);
        footer.setAlignment(Pos.CENTER_LEFT);

        var content = new VBox(10, title, subtitle, table, footer);
        content.setPadding(new Insets(10, 16, 16, 16));
        VBox.setVgrow(table, Priority.ALWAYS);

        var surface = new WindowSurface();
        surface.getStyleClass().add("dialog");
        surface.setTop(windowButtons);
        surface.setCenter(content);
        BorderPane.setMargin(windowButtons, new Insets(6, 6, 0, 6));

        var scene = new Scene(surface, 900, 520);
        scene.setFill(Color.TRANSPARENT);
        FX.addStyleSheet(scene);

        stage.setScene(scene);
    }

    public void show() {
        stage.show();
        stage.centerOnScreen();
    }

    private static TableView<JavaInstallation> createTable() {
        var table = new TableView<JavaInstallation>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.getSelectionModel().setCellSelectionEnabled(false);

        var primaryCol = new TableColumn<JavaInstallation, Boolean>("Primary Java");
        primaryCol.setMinWidth(90);
        primaryCol.setMaxWidth(110);
        primaryCol.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().isCurrentJavaVersion()));
        primaryCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                setAlignment(Pos.CENTER);
                if (item) {
                    setGraphic(new FontIcon(Material2MZ.STAR));
                    setTooltip(new Tooltip("Current java version used by your PC"));
                }
            }
        });

        var versionCol = new TableColumn<JavaInstallation, String>("Version");
        versionCol.setMinWidth(130);
        versionCol.setCellValueFactory(cell -> {
            JavaInstallation installation = cell.getValue();
            String type = Objects.toString(installation.getType(), "");
            int major = installation.getVersion() != null ? installation.getVersion().getMajor() : -1;
            return new ReadOnlyObjectWrapper<>(major > 0 ? (type + " " + major) : type);
        });

        var pathCol = new TableColumn<JavaInstallation, String>("Path");
        pathCol.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getJavaExecutable().getAbsolutePath()));

        table.getColumns().addAll(primaryCol, versionCol, pathCol);

        table.setRowFactory(tv -> {
            var row = new TableRow<JavaInstallation>();
            row.itemProperty().addListener((obs, oldV, newV) -> {
                if (newV == null) {
                    row.setTooltip(null);
                } else {
                    row.setTooltip(new Tooltip(newV.getJavaExecutable().getAbsolutePath()));
                }
            });
            return row;
        });

        return table;
    }

    private static Region spacer() {
        var r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        return r;
    }
}
