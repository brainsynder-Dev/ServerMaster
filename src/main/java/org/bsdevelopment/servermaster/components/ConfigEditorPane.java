package org.bsdevelopment.servermaster.components;

import atlantafx.base.theme.Styles;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.bsdevelopment.servermaster.LogViewer;
import org.bsdevelopment.servermaster.config.SettingsService;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public final class ConfigEditorPane extends VBox {
    private static final List<String> KNOWN_FILES = List.of(
            "server.properties",
            "bukkit.yml",
            "spigot.yml",
            "commands.yml",
            "paper.yml",
            "config/paper-global.yml",
            "config/paper-world-defaults.yml",
            "purpur.yml",
            "config/purpur.yml",
            "pufferfish.yml"
    );

    private final ComboBox<String> filePicker;
    private final ScrollPane formScroll;
    private final Label statusLabel;
    private final Button saveButton;

    private Path currentFile;
    private List<String> propertyLines;
    private Map<String, Supplier<String>> propertyReaders;
    private Supplier<Object> yamlReader;

    public ConfigEditorPane() {
        setSpacing(12);
        setPadding(new Insets(14));
        setMinHeight(0);

        var title = new Label("Configuration");
        title.getStyleClass().add("settings-section");

        filePicker = new ComboBox<>();
        filePicker.setPromptText("Select a config file");
        filePicker.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(filePicker, Priority.ALWAYS);
        filePicker.setOnAction(e -> openSelectedFile());

        var reload = new Button("Reload");
        reload.getStyleClass().add(Styles.BUTTON_OUTLINED);
        reload.setOnAction(e -> {
            refreshFileList();
            openSelectedFile();
        });

        saveButton = new Button("Save");
        saveButton.getStyleClass().addAll(Styles.ACCENT);
        saveButton.setDisable(true);
        saveButton.setOnAction(e -> saveCurrentFile());

        var toolbar = new HBox(10, filePicker, reload, saveButton);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        statusLabel = new Label();
        statusLabel.getStyleClass().addAll(Styles.TEXT_MUTED, Styles.TEXT_SMALL);
        statusLabel.setWrapText(true);

        formScroll = new ScrollPane();
        formScroll.setFitToWidth(true);
        formScroll.getStyleClass().add("edge-to-edge");
        formScroll.getStyleClass().add("config-form");
        VBox.setVgrow(formScroll, Priority.ALWAYS);

        getChildren().addAll(title, toolbar, statusLabel, formScroll);

        refreshFileList();
        if (!filePicker.getItems().isEmpty()) {
            filePicker.getSelectionModel().selectFirst();
            openSelectedFile();
        }
    }

    public void refresh() {
        String previous = filePicker.getValue();
        refreshFileList();
        if (filePicker.getValue() == null && !filePicker.getItems().isEmpty()) {
            filePicker.getSelectionModel().selectFirst();
            openSelectedFile();
        } else if (previous == null && filePicker.getValue() != null) {
            openSelectedFile();
        }
    }

    private void refreshFileList() {
        Path serverRoot = SettingsService.get().getServerPath();
        var found = new ArrayList<String>();
        if (serverRoot != null) {
            for (String candidate : KNOWN_FILES) {
                if (Files.isRegularFile(serverRoot.resolve(candidate))) found.add(candidate);
            }
        }

        String previous = filePicker.getValue();
        filePicker.getItems().setAll(found);
        if (previous != null && found.contains(previous)) {
            filePicker.setValue(previous);
        } else if (found.isEmpty()) {
            showMessage("No config files found. Start a server once to generate them.");
        }
    }

    private void openSelectedFile() {
        String selected = filePicker.getValue();
        if (selected == null) return;

        Path serverRoot = SettingsService.get().getServerPath();
        currentFile = serverRoot.resolve(selected);
        propertyReaders = null;
        yamlReader = null;

        try {
            if (selected.endsWith(".properties")) {
                loadProperties(currentFile);
            } else {
                loadYaml(currentFile);
            }
            saveButton.setDisable(false);
        } catch (Exception e) {
            saveButton.setDisable(true);
            showMessage("Could not open " + selected + ": " + e.getMessage());
        }
    }

    private void saveCurrentFile() {
        if (currentFile == null) return;

        try {
            if (propertyReaders != null) {
                saveProperties();
            } else if (yamlReader != null) {
                saveYaml();
            } else {
                return;
            }
            statusLabel.setText("Saved " + currentFile.getFileName() + ". Restart the server to apply changes.");
            LogViewer.system("Saved config file: " + currentFile.getFileName());
        } catch (IOException e) {
            statusLabel.setText("Failed to save: " + e.getMessage());
        }
    }

    // --- server.properties (line-preserving) --------------------------------
    private void loadProperties(Path file) throws IOException {
        List<String> lines = Files.readAllLines(file);
        var readers = new LinkedHashMap<String, Supplier<String>>();
        var form = new VBox(6);

        for (String line : lines) {
            String trimmed = line.strip();
            int equals = line.indexOf('=');
            if (trimmed.isEmpty() || trimmed.startsWith("#") || equals < 0) continue;

            String key = line.substring(0, equals).trim();
            String value = line.substring(equals + 1);

            if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
                var checkBox = new CheckBox();
                checkBox.setSelected(Boolean.parseBoolean(value.trim()));
                readers.put(key, () -> String.valueOf(checkBox.isSelected()));
                form.getChildren().add(leafRow(key, checkBox));
            } else {
                var field = new TextField(value);
                readers.put(key, field::getText);
                form.getChildren().add(leafRow(key, field));
            }
        }

        propertyLines = lines;
        propertyReaders = readers;
        statusLabel.setText(readers.size() + " properties loaded.");
        formScroll.setContent(padded(form));
    }

    private void saveProperties() throws IOException {
        var out = new ArrayList<String>(propertyLines.size());
        for (String line : propertyLines) {
            String trimmed = line.strip();
            int equals = line.indexOf('=');
            if (!trimmed.isEmpty() && !trimmed.startsWith("#") && equals >= 0) {
                String key = line.substring(0, equals).trim();
                Supplier<String> reader = propertyReaders.get(key);
                if (reader != null) {
                    out.add(key + "=" + reader.get());
                    continue;
                }
            }
            out.add(line);
        }
        Files.write(currentFile, out);
    }

    // --- YAML configs -------------------------------------------------------
    private void loadYaml(Path file) throws IOException {
        Object root;
        try (var in = Files.newInputStream(file)) {
            root = new Yaml().load(in);
        }

        if (!(root instanceof Map<?, ?>)) {
            yamlReader = null;
            saveButton.setDisable(true);
            showMessage(file.getFileName() + " is not a key/value config file.");
            return;
        }

        FormField field = buildYamlField(null, root);
        yamlReader = field.reader();
        statusLabel.setText("Loaded " + file.getFileName() + ". Note: comments are removed when this file is saved.");
        formScroll.setContent(padded((Region) field.node()));
    }

    private void saveYaml() throws IOException {
        var options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        options.setIndent(2);

        var yaml = new Yaml(options);
        try (Writer writer = Files.newBufferedWriter(currentFile)) {
            yaml.dump(yamlReader.get(), writer);
        }
    }

    private FormField buildYamlField(String key, Object value) {
        if (value instanceof Map<?, ?> map) {
            var box = new VBox(6);
            box.setPadding(new Insets(4, 0, 4, key == null ? 0 : 14));

            var keys = new ArrayList<String>();
            var readers = new ArrayList<Supplier<Object>>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String childKey = String.valueOf(entry.getKey());
                FormField child = buildYamlField(childKey, entry.getValue());
                box.getChildren().add(child.node());
                keys.add(childKey);
                readers.add(child.reader());
            }

            Supplier<Object> reader = () -> {
                var result = new LinkedHashMap<String, Object>();
                for (int i = 0; i < keys.size(); i++) result.put(keys.get(i), readers.get(i).get());
                return result;
            };

            if (key == null) return new FormField(box, reader);

            var pane = new TitledPane(key, box);
            pane.setExpanded(false);
            return new FormField(pane, reader);
        }

        if (value instanceof Boolean bool) {
            var checkBox = new CheckBox();
            checkBox.setSelected(bool);
            return new FormField(leafRow(key, checkBox), checkBox::isSelected);
        }

        if (value instanceof Number number) {
            var field = new TextField(String.valueOf(number));
            return new FormField(leafRow(key, field), () -> parseNumber(field.getText(), number));
        }

        if (value instanceof List<?> list) {
            var area = new TextArea();
            area.setWrapText(true);
            area.setPrefRowCount(Math.min(6, Math.max(2, list.size())));
            var joined = new StringBuilder();
            for (Object item : list) joined.append(item == null ? "" : item.toString()).append('\n');
            area.setText(joined.toString().stripTrailing());
            return new FormField(leafRow(key, area), () -> parseList(area.getText()));
        }

        boolean wasNull = value == null;
        var field = new TextField(wasNull ? "" : value.toString());
        return new FormField(leafRow(key, field), () -> {
            String text = field.getText();
            return (wasNull && (text == null || text.isEmpty())) ? null : text;
        });
    }

    private Region leafRow(String key, Control control) {
        var label = new Label(key);
        label.getStyleClass().add(Styles.TEXT_MUTED);
        label.setWrapText(true);
        label.setMinWidth(150);
        label.setPrefWidth(240);

        control.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(control, Priority.ALWAYS);

        var row = new HBox(12, label, control);
        row.setAlignment(control instanceof TextArea ? Pos.TOP_LEFT : Pos.CENTER_LEFT);
        return row;
    }

    private void showMessage(String message) {
        var label = new Label(message);
        label.getStyleClass().addAll(Styles.TEXT_MUTED);
        label.setWrapText(true);
        statusLabel.setText("");
        formScroll.setContent(padded(label));
    }

    private static Region padded(Region content) {
        var box = new VBox(content);
        box.setPadding(new Insets(2, 4, 8, 2));
        return box;
    }

    private static Object parseNumber(String text, Number original) {
        String value = text == null ? "" : text.trim();
        try {
            if (original instanceof Integer) return Integer.parseInt(value);
            if (original instanceof Long) return Long.parseLong(value);
            if (original instanceof Double) return Double.parseDouble(value);
            if (original instanceof Float) return Float.parseFloat(value);
            if (value.contains(".")) return Double.parseDouble(value);
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return value;
        }
    }

    private static List<Object> parseList(String text) {
        var out = new ArrayList<Object>();
        if (text == null) return out;
        for (String line : text.split("\n", -1)) {
            String item = line.strip();
            if (item.isEmpty()) continue;
            out.add(inferScalar(item));
        }
        return out;
    }

    private static Object inferScalar(String text) {
        if (text.equalsIgnoreCase("true") || text.equalsIgnoreCase("false")) return Boolean.parseBoolean(text);
        try {
            if (text.contains(".")) return Double.parseDouble(text);
            return Long.parseLong(text);
        } catch (NumberFormatException e) {
            return text;
        }
    }

    private record FormField(Node node, Supplier<Object> reader) {
    }
}
