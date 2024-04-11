package org.bsdevelopment.servermaster.utils.markdown;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextArea;
import com.vladsch.flexmark.ext.emoji.EmojiExtension;
import com.vladsch.flexmark.ext.emoji.EmojiImageType;
import com.vladsch.flexmark.ext.emoji.EmojiShortcutType;
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.gfm.tasklist.TaskListExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;

import java.util.Arrays;

@Tag("markdown-area")
public class MarkdownArea extends Composite<Div> {

    private final TextArea inputText = new TextArea();
    private final Div writeView = new Div(inputText);
    private final Div previewView = new Div();
    private final Tab writeTab = new Tab("Write");
    private final Tab previewTab = new Tab("Preview");
    private final Tabs tabs = new Tabs(writeTab, previewTab);
    
    MutableDataSet options = new MutableDataSet().set(Parser.EXTENSIONS, Arrays.asList(
                    // AutolinkExtension.create(),
                    EmojiExtension.create(),
                    StrikethroughExtension.create(),
                    TaskListExtension.create(),
                    TablesExtension.create()
            ))
            .set(TablesExtension.WITH_CAPTION, false)
            .set(TablesExtension.COLUMN_SPANS, false)
            .set(TablesExtension.MIN_HEADER_ROWS, 1)
            .set(TablesExtension.MAX_HEADER_ROWS, 1)
            .set(TablesExtension.APPEND_MISSING_COLUMNS, true)
            .set(TablesExtension.DISCARD_EXTRA_COLUMNS, true)
            .set(TablesExtension.HEADER_SEPARATOR_COLUMN_MATCH, true)

            .set(EmojiExtension.USE_SHORTCUT_TYPE, EmojiShortcutType.GITHUB)
            .set(EmojiExtension.USE_IMAGE_TYPE, EmojiImageType.IMAGE_ONLY);
    Parser parser = Parser.builder(options).build();
    HtmlRenderer renderer = HtmlRenderer.builder(options).build();

    public MarkdownArea() {
        init();
    }

    public MarkdownArea(String text) {
        if (MarkdownUtil.isNotBlank(text)) setValue(text);

        init();
    }

    public void setWidthFull() {
        getContent().setWidthFull();
        writeView.setWidthFull();
        inputText.setWidthFull();
        previewView.setWidthFull();
    }

    public void setHeightFull() {
        // getContent().setHeightFull();
        // writeView.setHeightFull();
        // inputText.setHeightFull();
        previewView.setHeightFull();
    }


    private void init() {
        inputText.setWidth("100%");
        previewView.setVisible(false);
        previewView.setClassName("markdown-body");
        getContent().add(tabs, writeView, previewView);
        tabs.addSelectedChangeListener(event -> {
            if (tabs.getSelectedTab().getLabel().equals("Preview")) {
                writeView.setVisible(false);
                previewView.setVisible(true);
                String text = getValue().isEmpty() ? "*Nothing to preview*" : getValue();
                addMarkdown(text);
            } else {
                writeView.setVisible(true);
                previewView.setVisible(false);
            }
        });
    }

    public void previewOnly () {
        tabs.setVisible(false);
        writeView.setVisible(false);

        previewView.setVisible(true);
        String text = getValue().isEmpty() ? "*Nothing to preview*" : getValue();
        addMarkdown(text);
    }

    private void addMarkdown(String value) {
        String html = ("<div style=\"width:90%; height:100%; padding:1rem; overflow-wrap: break-word;\">" + parseMarkdown(MarkdownUtil.getNullSafeString(value)) + "</div>");
        Html item = new Html(html);
        previewView.removeAll();
        previewView.add(item);
    }

    private String parseMarkdown(String value) {
        Node text = parser.parse(value);
        return renderer.render(text);
    }

    public void setValue(String value) {
        inputText.setValue(value);
    }

    public String getValue() {
        return inputText.getValue();
    }

    public TextArea getInputText() {
        return inputText;
    }

    public Div getPreviewView() {
        return previewView;
    }
}

