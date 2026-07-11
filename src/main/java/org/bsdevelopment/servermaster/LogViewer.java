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
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class LogViewer extends AnchorPane {

    private static volatile LogViewer ACTIVE_INSTANCE;
    private static final Pattern TIMESTAMP = Pattern.compile("\\[\\d{2}:\\d{2}:\\d{2}\\]");
    private static final Pattern ANSI_ESCAPE = Pattern.compile("\\u001B\\[[0-9;]*m");
    private static final Pattern FORMAT_CODE = Pattern.compile("[§&][0-9a-fk-orA-FK-OR]");
    private static final String FORMAT_CODE_CHARS = "0123456789abcdefklmnor";

    private String lastStyle = "log-default";
    private boolean autoScroll = true;
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

    public static void update(String message) {
        LogViewer viewer = ACTIVE_INSTANCE;
        if (viewer == null) return;

        Platform.runLater(() -> viewer.appendUpdateMessage(message));
    }

    /** Raw console line piping (BuildTools/server output). */
    public static void console(String line) {
        LogViewer viewer = ACTIVE_INSTANCE;
        if (viewer == null) return;

        Platform.runLater(() -> viewer.appendLine(line));
    }

    public void loadFile(Path path) throws IOException {
        List<String> lines = Files.readAllLines(path);
        codeArea.clear();
        lastStyle = "log-default";
        for (String line : lines) {
            appendLine(line);
        }
    }

    public void appendLine(String rawLine) {
        String line = ANSI_ESCAPE.matcher(rawLine).replaceAll("");

        if (FORMAT_CODE.matcher(line).find()) {
            appendFormattedLine(line);
        } else {
            appendPlainLine(line);
        }
    }

    private void appendPlainLine(String line) {
        int start = codeArea.getLength();
        codeArea.appendText(line + System.lineSeparator());
        int end = codeArea.getLength();

        String style = determineStyleClass(line);
        safeSetStyleClass(start, end, style);

        highlightTimestamps(line, start);

        if (autoScroll) codeArea.requestFollowCaret();
    }

    private void appendFormattedLine(String line) {
        String baseStyle = determineStyleClass(line);
        List<Segment> segments = parseFormatting(line, baseStyle);

        StringBuilder visible = new StringBuilder();
        for (Segment segment : segments) visible.append(segment.text());

        int start = codeArea.getLength();
        codeArea.appendText(visible + System.lineSeparator());

        int offset = start;
        for (Segment segment : segments) {
            int segmentEnd = offset + segment.text().length();
            safeSetStyleClasses(offset, segmentEnd, segment.styleClasses());
            offset = segmentEnd;
        }

        highlightTimestamps(visible.toString(), start);

        if (autoScroll) codeArea.requestFollowCaret();
    }

    private void highlightTimestamps(String text, int start) {
        var matcher = TIMESTAMP.matcher(text);
        while (matcher.find()) {
            safeSetStyleClass(start + matcher.start(), start + matcher.end(), "log-timestamp");
        }
    }

    private static List<Segment> parseFormatting(String line, String baseStyle) {
        List<Segment> segments = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        String color = baseStyle;
        boolean bold = false, italic = false, underline = false, strike = false;

        int length = line.length();
        for (int index = 0; index < length; index++) {
            char character = line.charAt(index);
            boolean isCode = (character == '§' || character == '&')
                    && index + 1 < length
                    && FORMAT_CODE_CHARS.indexOf(Character.toLowerCase(line.charAt(index + 1))) >= 0;

            if (!isCode) {
                current.append(character);
                continue;
            }

            if (current.length() > 0) {
                segments.add(new Segment(current.toString(), buildStyleClasses(color, bold, italic, underline, strike)));
                current.setLength(0);
            }

            char code = Character.toLowerCase(line.charAt(++index));
            if ((code >= '0' && code <= '9') || (code >= 'a' && code <= 'f')) {
                color = "mc-" + code;
                bold = italic = underline = strike = false;
            } else switch (code) {
                case 'l' -> bold = true;
                case 'm' -> strike = true;
                case 'n' -> underline = true;
                case 'o' -> italic = true;
                case 'r' -> {
                    color = baseStyle;
                    bold = italic = underline = strike = false;
                }
                default -> { /* 'k' (obfuscated) is intentionally ignored */ }
            }
        }

        if (current.length() > 0) {
            segments.add(new Segment(current.toString(), buildStyleClasses(color, bold, italic, underline, strike)));
        }

        return segments;
    }

    private static List<String> buildStyleClasses(String color, boolean bold, boolean italic, boolean underline, boolean strike) {
        List<String> classes = new ArrayList<>();
        if (color != null && !color.isBlank()) classes.add(color);
        if (bold) classes.add("mc-bold");
        if (italic) classes.add("mc-italic");
        if (underline) classes.add("mc-underline");
        if (strike) classes.add("mc-strike");
        return classes;
    }

    public void setAutoScroll(boolean autoScroll) {
        this.autoScroll = autoScroll;
        if (autoScroll) codeArea.requestFollowCaret();
    }

    public void clearConsole() {
        codeArea.clear();
    }

    public void appendSystemMessage(String message) {
        appendStyledLine("[ServerMaster] " + message, "log-system");
    }
    public void appendUpdateMessage(String message) {
        appendStyledLine("[ServerMaster Updater] " + message, "log-update");
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

    private void safeSetStyleClasses(int start, int end, List<String> styleClasses) {
        if (styleClasses.isEmpty()) return;

        int docLen = codeArea.getLength();
        int safeStart = Math.max(0, Math.min(start, docLen));
        int safeEnd = Math.max(safeStart, Math.min(end, docLen));

        if (safeStart < safeEnd) {
            codeArea.setStyle(safeStart, safeEnd, styleClasses);
        }
    }

    private record Segment(String text, List<String> styleClasses) {
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
