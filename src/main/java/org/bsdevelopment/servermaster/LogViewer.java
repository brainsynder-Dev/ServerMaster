package org.bsdevelopment.servermaster;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.AnchorPane;
import org.bsdevelopment.servermaster.utils.AnchorUtil;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

public class LogViewer extends AnchorPane {

    private static volatile LogViewer ACTIVE_INSTANCE;
    private static final Pattern TIMESTAMP = Pattern.compile("\\[\\d{2}:\\d{2}:\\d{2}\\]");

    private String lastStyle = "log-default";
    private final CodeArea codeArea = new CodeArea();

    public LogViewer() {
        getStyleClass().add("log-viewer");
        setPadding(new Insets(2));

        codeArea.setWrapText(true);
        codeArea.setEditable(false);
        codeArea.setFocusTraversable(true);
        codeArea.getStyleClass().add("log-cell");
        codeArea.setStyle("-fx-font-family: 'Monospaced'; -fx-font-size: 12px;");

        var scrollPane = new VirtualizedScrollPane<>(codeArea);
        getChildren().add(AnchorUtil.setAnchors(scrollPane, 0.0, 0.0, 0.0, 0.0));

        ContextMenu ctx = new ContextMenu();
        MenuItem copy = new MenuItem("Copy");
        copy.setOnAction(evt -> {
            String selected = codeArea.getSelectedText();
            if (!selected.isEmpty()) {
                ClipboardContent content = new ClipboardContent();
                content.putString(selected);
                Clipboard.getSystemClipboard().setContent(content);
            }
        });
        ctx.getItems().add(copy);
        codeArea.setContextMenu(ctx);
    }

    public static void registerActive(LogViewer viewer) {
        ACTIVE_INSTANCE = viewer;
    }

    public static void system(String message) {
        LogViewer viewer = ACTIVE_INSTANCE;
        if (viewer == null) return;

        Platform.runLater(() -> viewer.appendSystemMessage(message));
    }

    public void loadFile(Path path) throws IOException {
        List<String> lines = Files.readAllLines(path);
        codeArea.clear();
        lastStyle = "log-default";
        for (String line : lines) {
            appendLine(line);
        }
    }

    public void appendLine(String line) {
        int start = codeArea.getLength();
        codeArea.appendText(line + System.lineSeparator());
        int end = codeArea.getLength();

        String style = determineStyleClass(line);
        safeSetStyleClass(start, end, style);

        var matcher = TIMESTAMP.matcher(line);
        while (matcher.find()) {
            int absStart = start + matcher.start();
            int absEnd = start + matcher.end();
            safeSetStyleClass(absStart, absEnd, "log-timestamp");
        }

        codeArea.requestFollowCaret();
    }

    public void clearConsole() {
        codeArea.clear();
    }

    public void appendSystemMessage(String message) {
        appendStyledLine("[ServerMaster] " + message, "log-system");
    }

    public void appendStyledLine(String text, String styleClass) {
        int start = codeArea.getLength();
        codeArea.appendText(text + System.lineSeparator());
        int end = codeArea.getLength();

        safeSetStyleClass(start, end, styleClass);
    }

    private void safeSetStyleClass(int start, int end, String styleClass) {
        int docLen = codeArea.getLength();

        int safeStart = Math.max(0, Math.min(start, docLen));
        int safeEnd = Math.max(safeStart, Math.min(end, docLen));

        if (safeStart < safeEnd) {
            codeArea.setStyleClass(safeStart, safeEnd, styleClass);
        }
    }

    private String determineStyleClass(String line) {
        if (line.contains(" INFO]: Done (")) return lastStyle = "log-success";
        if (line.contains("Server empty for 60 seconds, pausing")) return lastStyle = "log-paused";
        if (line.contains("FATAL")) return lastStyle = "log-fatal";
        if (line.contains("ERROR") || line.contains("[STDERR]:")) return lastStyle = "log-error";
        if (line.contains("WARN")) return lastStyle = "log-warn";
        if (line.contains("DEBUG")) return lastStyle = "log-debug";
        if (line.contains("TRACE")) return lastStyle = "log-trace";
        if (line.contains("INFO")) return lastStyle = "log-info";
        return lastStyle;
    }
}
