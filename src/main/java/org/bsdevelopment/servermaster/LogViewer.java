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
import org.fxmisc.richtext.model.StyleSpansBuilder;
import org.fxmisc.richtext.util.UndoUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

public class LogViewer extends AnchorPane {

    private static volatile LogViewer ACTIVE_INSTANCE;
    private static final Pattern TIMESTAMP = Pattern.compile("\\[\\d{2}:\\d{2}:\\d{2}\\]");
    private static final Pattern ANSI_ESCAPE = Pattern.compile("\\u001B\\[[0-9;]*m");
    private static final Pattern FORMAT_CODE = Pattern.compile("[§&][0-9a-fk-orA-FK-OR]");
    private static final String FORMAT_CODE_CHARS = "0123456789abcdefklmnor";
    private static final String LINE_SEPARATOR = System.lineSeparator();
    private static final List<String> TIMESTAMP_STYLE = List.of("log-timestamp");
    private static final int MAX_VISIBLE_LINES = 5000;
    private static final int TRIM_SLACK_LINES = 1000;

    private String lastStyle = "log-default";
    private boolean autoScroll = true;
    private final CodeArea codeArea = new CodeArea();
    private final Queue<PendingLine> pendingLines = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean flushScheduled = new AtomicBoolean(false);

    public LogViewer() {
        getStyleClass().add("log-viewer");
        setPadding(new Insets(2));

        codeArea.setWrapText(true);
        codeArea.setEditable(false);
        codeArea.setFocusTraversable(true);
        codeArea.getStyleClass().add("log-cell");
        codeArea.setStyle("-fx-font-family: 'Monospaced'; -fx-font-size: 12px;");
        codeArea.setUndoManager(UndoUtils.noOpUndoManager());

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

        viewer.appendSystemMessage(message);
    }

    public static void update(String message) {
        LogViewer viewer = ACTIVE_INSTANCE;
        if (viewer == null) return;

        viewer.appendUpdateMessage(message);
    }

    /** Raw console line piping (BuildTools/server output). */
    public static void console(String line) {
        LogViewer viewer = ACTIVE_INSTANCE;
        if (viewer == null) return;

        viewer.appendLine(line);
    }

    public void loadFile(Path path) throws IOException {
        List<String> lines = Files.readAllLines(path);
        clearConsole();
        for (String line : lines) {
            appendLine(line);
        }
    }

    public void appendLine(String rawLine) {
        queueLine(new PendingLine(rawLine, null));
    }

    public void appendSystemMessage(String message) {
        appendStyledLine("[ServerMaster] " + message, "log-system");
    }

    public void appendUpdateMessage(String message) {
        appendStyledLine("[ServerMaster Updater] " + message, "log-update");
    }

    public void appendStyledLine(String text, String styleClass) {
        queueLine(new PendingLine(text, styleClass));
    }

    public void setAutoScroll(boolean autoScroll) {
        this.autoScroll = autoScroll;
        if (autoScroll) codeArea.requestFollowCaret();
    }

    public void clearConsole() {
        pendingLines.clear();
        lastStyle = "log-default";
        if (Platform.isFxApplicationThread()) {
            codeArea.clear();
        } else {
            Platform.runLater(codeArea::clear);
        }
    }

    private void queueLine(PendingLine line) {
        pendingLines.add(line);
        if (flushScheduled.compareAndSet(false, true)) Platform.runLater(this::flushPendingLines);
    }

    private void flushPendingLines() {
        flushScheduled.set(false);
        if (pendingLines.isEmpty()) return;

        StringBuilder batchText = new StringBuilder();
        StyleSpansBuilder<Collection<String>> batchSpans = new StyleSpansBuilder<>();

        PendingLine pending;
        while ((pending = pendingLines.poll()) != null) {
            if (pending.forcedStyle() == null) {
                buildConsoleLine(pending.text(), batchText, batchSpans);
            } else {
                buildUniformLine(pending.text(), List.of(pending.forcedStyle()), batchText, batchSpans);
            }
        }

        if (batchText.length() == 0) return;

        int start = codeArea.getLength();
        codeArea.appendText(batchText.toString());
        codeArea.setStyleSpans(start, batchSpans.create());

        trimOverflowingLines();

        if (autoScroll) codeArea.requestFollowCaret();
    }

    private void buildConsoleLine(String rawLine, StringBuilder batchText, StyleSpansBuilder<Collection<String>> batchSpans) {
        String line = ANSI_ESCAPE.matcher(rawLine).replaceAll("");
        List<String> baseClasses = List.of(determineStyleClass(line));

        String visible = line;
        List<Segment> segments;
        if (FORMAT_CODE.matcher(line).find()) {
            segments = parseFormatting(line, baseClasses.get(0));

            StringBuilder stripped = new StringBuilder();
            for (Segment segment : segments) stripped.append(segment.text());
            visible = stripped.toString();
        } else {
            segments = List.of(new Segment(line, baseClasses));
        }

        batchText.append(visible).append(LINE_SEPARATOR);
        addSegmentSpans(segments, visible, batchSpans);
        addSpan(batchSpans, baseClasses, LINE_SEPARATOR.length());
    }

    private void buildUniformLine(String text, List<String> styleClasses, StringBuilder batchText, StyleSpansBuilder<Collection<String>> batchSpans) {
        batchText.append(text).append(LINE_SEPARATOR);
        addSpan(batchSpans, styleClasses, text.length() + LINE_SEPARATOR.length());
    }

    private void trimOverflowingLines() {
        int paragraphs = codeArea.getParagraphs().size();
        if (paragraphs <= MAX_VISIBLE_LINES + TRIM_SLACK_LINES) return;

        int removeUntil = codeArea.getAbsolutePosition(paragraphs - MAX_VISIBLE_LINES, 0);
        if (removeUntil > 0) codeArea.deleteText(0, removeUntil);
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

    private static void addSegmentSpans(List<Segment> segments, String visible, StyleSpansBuilder<Collection<String>> batchSpans) {
        List<int[]> timestampRanges = findTimestampRanges(visible);

        int position = 0;
        for (Segment segment : segments) {
            int segmentEnd = position + segment.text().length();
            int cursor = position;

            for (int[] range : timestampRanges) {
                if (range[1] <= cursor || range[0] >= segmentEnd) continue;

                int overlapStart = Math.max(cursor, range[0]);
                int overlapEnd = Math.min(segmentEnd, range[1]);
                addSpan(batchSpans, segment.styleClasses(), overlapStart - cursor);
                addSpan(batchSpans, TIMESTAMP_STYLE, overlapEnd - overlapStart);
                cursor = overlapEnd;
            }

            addSpan(batchSpans, segment.styleClasses(), segmentEnd - cursor);
            position = segmentEnd;
        }
    }

    private static void addSpan(StyleSpansBuilder<Collection<String>> batchSpans, List<String> styleClasses, int length) {
        if (length > 0) batchSpans.add(styleClasses, length);
    }

    private static List<int[]> findTimestampRanges(String text) {
        var matcher = TIMESTAMP.matcher(text);
        if (!matcher.find()) return List.of();

        List<int[]> ranges = new ArrayList<>();
        do {
            ranges.add(new int[]{matcher.start(), matcher.end()});
        } while (matcher.find());
        return ranges;
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

    private record PendingLine(String text, String forcedStyle) {
    }

    private record Segment(String text, List<String> styleClasses) {
    }
}
