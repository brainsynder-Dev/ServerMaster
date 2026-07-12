package org.bsdevelopment.servermaster.ui.dialog;

import atlantafx.base.theme.Styles;
import com.eclipsesource.json.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.bsdevelopment.servermaster.LogViewer;
import org.bsdevelopment.servermaster.ServerMasterApp;
import org.bsdevelopment.servermaster.instance.server.gamerule.GameRuleCatalog;
import org.bsdevelopment.servermaster.ui.window.WindowButtons;
import org.bsdevelopment.servermaster.ui.window.WindowSurface;
import org.bsdevelopment.servermaster.utils.FX;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class GameRulesDialog {

    private final Stage stage;
    private final Path gameruleFile;
    private final List<RuleRow> rows = new ArrayList<>();
    private final VBox ruleBox = new VBox(6);

    public GameRulesDialog(Stage owner) {
        gameruleFile = ServerMasterApp.resolveDataFile("gamerules.json");

        stage = new Stage();
        stage.initOwner(owner);
        stage.initModality(Modality.WINDOW_MODAL);
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setTitle("Game Rules");

        var windowButtons = new WindowButtons(stage, false);
        windowButtons.setStyle("-fx-background-color: transparent;");

        var title = new Label("Game Rules");
        title.getStyleClass().add("app-title");

        var subtitle = new Label("Enable a rule and set its value. Enabled rules are applied automatically when the server finishes starting.");
        subtitle.getStyleClass().addAll(Styles.TEXT_MUTED, Styles.TEXT_SMALL);
        subtitle.setWrapText(true);
        subtitle.setMaxWidth(Double.MAX_VALUE);

        var search = new TextField();
        search.setPromptText("Search game rules…");
        search.textProperty().addListener((obs, old, text) -> renderRows(text));

        Map<String, StoredRule> stored = loadStoredRules();
        buildRows(stored);
        renderRows("");

        ruleBox.setPadding(new Insets(2, 18, 8, 18));

        var scroll = new ScrollPane(ruleBox);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("edge-to-edge");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        var save = new Button("SAVE GAME RULES");
        save.getStyleClass().addAll(Styles.ACCENT, "hero-button");
        save.setMaxWidth(Double.MAX_VALUE);
        save.setOnAction(e -> {
            if (saveRules()) stage.close();
        });

        var header = new VBox(6, title, subtitle, search);
        header.setPadding(new Insets(4, 18, 8, 18));

        var saveBar = new VBox(save);
        saveBar.setPadding(new Insets(8, 18, 16, 18));

        var content = new VBox(header, scroll, saveBar);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        var surface = new WindowSurface();
        surface.getStyleClass().add("dialog");
        surface.setTop(windowButtons);
        surface.setCenter(content);
        BorderPane.setMargin(windowButtons, new Insets(6, 6, 0, 6));

        FX.buildDialogScene(stage, surface, 640, 700);
    }

    public void show() {
        FX.showDialog(stage);
    }

    private void buildRows(Map<String, StoredRule> stored) {
        for (String name : stored.keySet()) {
            if (GameRuleCatalog.find(name) == null) {
                StoredRule custom = stored.get(name);
                var rule = new GameRuleCatalog.GameRule(name, guessType(custom.value()), custom.value(),
                        "", "Custom rule — not part of the built-in catalog.", null, null, toAliases(custom.aliases()));
                rows.add(new RuleRow(rule, stored.get(name)));
            }
        }

        for (GameRuleCatalog.GameRule rule : GameRuleCatalog.all()) {
            rows.add(new RuleRow(rule, stored.get(rule.name())));
        }
    }

    private void renderRows(String filter) {
        String needle = filter == null ? "" : filter.trim().toLowerCase(Locale.ROOT);
        ruleBox.getChildren().clear();

        for (RuleRow row : rows) {
            if (needle.isEmpty() || row.matches(needle)) {
                ruleBox.getChildren().add(row.node());
            }
        }
    }

    private boolean saveRules() {
        var rulesArray = new JsonArray();

        for (RuleRow row : rows) {
            if (!row.isEnabled()) continue;

            String value = row.value();
            if (value == null || value.isBlank()) continue;

            var object = Json.object().add("name", row.name()).add("value", value);

            List<String> aliases = row.aliases();
            if (aliases != null && !aliases.isEmpty()) {
                var aliasArray = new JsonArray();
                aliases.forEach(aliasArray::add);
                object.add("aliases", aliasArray);
            }

            rulesArray.add(object);
        }

        var root = Json.object().add("rules", rulesArray);
        try {
            Files.writeString(gameruleFile, root.toString(WriterConfig.PRETTY_PRINT), StandardCharsets.UTF_8);
            LogViewer.system("Saved " + rulesArray.size() + " game rule(s) to gamerules.json");
            return true;
        } catch (IOException e) {
            LogViewer.system("Failed to save gamerules.json: " + e.getMessage());
            return false;
        }
    }

    private Map<String, StoredRule> loadStoredRules() {
        var result = new LinkedHashMap<String, StoredRule>();
        if (!Files.exists(gameruleFile)) return result;

        try {
            String raw = Files.readString(gameruleFile, StandardCharsets.UTF_8);
            JsonValue parsed = Json.parse(raw);
            if (!parsed.isObject()) return result;

            JsonValue rules = parsed.asObject().get("rules");
            if (rules == null || !rules.isArray()) return result;

            for (JsonValue entry : rules.asArray()) {
                if (entry == null || !entry.isObject()) continue;
                JsonObject object = entry.asObject();

                String name = stringOrNull(object.get("name"));
                String value = stringOrNull(object.get("value"));
                if (name == null || name.isBlank() || value == null || value.isBlank()) continue;

                result.put(name, new StoredRule(value, readAliases(object.get("aliases"))));
            }
        } catch (IOException | RuntimeException e) {
            LogViewer.system("Could not read existing gamerules.json: " + e.getMessage());
        }
        return result;
    }

    private static List<String> readAliases(JsonValue value) {
        if (value == null || !value.isArray()) return List.of();

        var list = new ArrayList<String>();
        for (JsonValue item : value.asArray()) {
            if (item != null && item.isString() && !item.asString().isBlank()) {
                list.add(item.asString());
            }
        }
        return List.copyOf(list);
    }

    private static String stringOrNull(JsonValue value) {
        return value != null && value.isString() ? value.asString() : null;
    }

    private static GameRuleCatalog.Type guessType(String value) {
        return "true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)
                ? GameRuleCatalog.Type.BOOLEAN
                : GameRuleCatalog.Type.INTEGER;
    }

    private static List<GameRuleCatalog.Alias> toAliases(List<String> names) {
        var list = new ArrayList<GameRuleCatalog.Alias>();
        for (String name : names) {
            if (name != null && !name.isBlank()) list.add(new GameRuleCatalog.Alias(name, false));
        }
        return List.copyOf(list);
    }

    private record StoredRule(String value, List<String> aliases) {
    }

    private static class RuleRow {
        private final GameRuleCatalog.GameRule rule;
        private final CheckBox enabled;
        private final ComboBox<String> booleanValue;
        private final TextField integerValue;
        private final TextField aliasField;
        private final VBox container;

        private RuleRow(GameRuleCatalog.GameRule rule, StoredRule stored) {
            this.rule = rule;

            enabled = new CheckBox();
            enabled.setSelected(stored != null);

            String initial = stored != null ? stored.value() : rule.defaultValue();

            if (rule.isBoolean()) {
                booleanValue = new ComboBox<>();
                booleanValue.getItems().addAll("true", "false");
                booleanValue.setValue(Boolean.toString(!"false".equalsIgnoreCase(initial)));
                booleanValue.setPrefWidth(96);
                booleanValue.disableProperty().bind(enabled.selectedProperty().not());
                integerValue = null;
            } else {
                integerValue = new TextField(initial);
                integerValue.setPrefColumnCount(6);
                integerValue.setTextFormatter(new TextFormatter<>(change ->
                        change.getControlNewText().matches("-?\\d*") ? change : null));
                integerValue.disableProperty().bind(enabled.selectedProperty().not());
                booleanValue = null;
            }

            var name = new Label(rule.name());
            name.getStyleClass().addAll(Styles.TEXT_BOLD);

            var meta = new Label(rule.since().isBlank() ? typeLabel() : "Since " + rule.since() + "  ·  " + typeLabel());
            meta.getStyleClass().addAll(Styles.TEXT_MUTED, Styles.TEXT_SMALL);

            var description = new Label(rule.description());
            description.getStyleClass().addAll(Styles.TEXT_MUTED, Styles.TEXT_SMALL);
            description.setWrapText(true);
            description.setMaxWidth(Double.MAX_VALUE);

            var text = new VBox(2, new HBox(8, name, meta), description);
            HBox.setHgrow(text, Priority.ALWAYS);

            var valueControl = rule.isBoolean() ? booleanValue : integerValue;

            var mainRow = new HBox(12, enabled, text, valueControl);
            mainRow.setAlignment(Pos.CENTER_LEFT);

            List<String> initialAliases = stored != null ? stored.aliases() : List.of();
            aliasField = new TextField(String.join(", ", initialAliases));
            aliasField.setPromptText("extra names to also try (comma separated) — known version renames are applied automatically");
            aliasField.disableProperty().bind(enabled.selectedProperty().not());

            container = new VBox(6, mainRow, aliasField);
            container.getStyleClass().add("settings-card");
        }

        private Region node() {
            return container;
        }

        private boolean isEnabled() {
            return enabled.isSelected();
        }

        private String name() {
            return rule.name();
        }

        private List<String> aliases() {
            String text = aliasField.getText();
            if (text == null || text.isBlank()) return List.of();

            var list = new ArrayList<String>();
            for (String part : text.split(",")) {
                String trimmed = part.strip();
                if (!trimmed.isEmpty() && !trimmed.equalsIgnoreCase(rule.name())) list.add(trimmed);
            }
            return list;
        }

        private String value() {
            if (rule.isBoolean()) return booleanValue.getValue();
            String text = integerValue.getText();
            if (text == null || text.isBlank() || text.equals("-")) return rule.defaultValue();
            return clampInteger(text);
        }

        private String clampInteger(String text) {
            try {
                long parsed = Long.parseLong(text);
                if (rule.minimum() != null && parsed < rule.minimum()) parsed = rule.minimum();
                if (rule.maximum() != null && parsed > rule.maximum()) parsed = rule.maximum();
                return Long.toString(parsed);
            } catch (NumberFormatException e) {
                return rule.defaultValue();
            }
        }

        private boolean matches(String needle) {
            if (rule.name().toLowerCase(Locale.ROOT).contains(needle)
                    || rule.description().toLowerCase(Locale.ROOT).contains(needle)) {
                return true;
            }
            for (GameRuleCatalog.Alias alias : rule.aliases()) {
                if (alias.name().toLowerCase(Locale.ROOT).contains(needle)) return true;
            }
            for (String alias : aliases()) {
                if (alias.toLowerCase(Locale.ROOT).contains(needle)) return true;
            }
            return false;
        }

        private String typeLabel() {
            return rule.isBoolean() ? "boolean" : "integer";
        }
    }
}
