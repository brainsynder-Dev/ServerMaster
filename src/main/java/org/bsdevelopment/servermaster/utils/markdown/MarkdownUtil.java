package org.bsdevelopment.servermaster.utils.markdown;

import com.vladsch.flexmark.ext.abbreviation.AbbreviationExtension;
import com.vladsch.flexmark.ext.anchorlink.AnchorLinkExtension;
import com.vladsch.flexmark.ext.autolink.AutolinkExtension;
import com.vladsch.flexmark.ext.definition.DefinitionExtension;
import com.vladsch.flexmark.ext.emoji.EmojiExtension;
import com.vladsch.flexmark.ext.escaped.character.EscapedCharacterExtension;
import com.vladsch.flexmark.ext.footnotes.FootnoteExtension;
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.gfm.tasklist.TaskListExtension;
import com.vladsch.flexmark.ext.jekyll.front.matter.JekyllFrontMatterExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.ext.toc.SimTocExtension;
import com.vladsch.flexmark.ext.toc.TocExtension;
import com.vladsch.flexmark.ext.toc.internal.TocOptions;
import com.vladsch.flexmark.ext.typographic.TypographicExtension;
import com.vladsch.flexmark.ext.wikilink.WikiLinkExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.KeepType;
import com.vladsch.flexmark.util.data.MutableDataHolder;
import com.vladsch.flexmark.util.data.MutableDataSet;
import com.vladsch.flexmark.util.misc.Extension;

import java.util.ArrayList;

class MarkdownUtil {
    public enum ForUseBy {
        PARSER,
        JAVAFX,
        SWING,
        HTML
    }

    static class Options {
        public boolean abbreviations = false;
        public boolean autoLinks = true;
        public boolean anchorLinks = true;
        public boolean definitions = false;
        public boolean fencedCode = true;
        public boolean hardWraps = false;
        public boolean atxHeadingSpace = true;
        public boolean typographicQuotes = false;
        public boolean typographicSmarts = false;
        public boolean relaxedThematicBreak = true;
        public boolean strikeThrough = true;
        public boolean tables = true;
        public boolean renderTablesGFM = true;
        public boolean taskListItems = true;
        public boolean wikiLinks = false;
        public boolean wikiLinkGfmSyntax = true;
        public boolean footnotes = false;
        public boolean tableOfContents = true;
        public boolean jekyllFrontMatter = false;
        public boolean emojiShortcuts = false;
        public String emojiImageDirectory = "";
    }

    static MutableDataHolder options(ForUseBy purpose, Options options) {
        MutableDataSet dataSet = new MutableDataSet();
        ArrayList<Extension> extensions = new ArrayList<>();

        dataSet.set(Parser.PARSE_INNER_HTML_COMMENTS, true);
        dataSet.set(Parser.INDENTED_CODE_NO_TRAILING_BLANK_LINES, true);
        dataSet.set(HtmlRenderer.SUPPRESS_HTML_BLOCKS, false);
        dataSet.set(HtmlRenderer.SUPPRESS_INLINE_HTML, false);

        // add default extensions in pegdown
        extensions.add(EscapedCharacterExtension.create());

        // Setup Block Quote Options
        dataSet.set(Parser.BLOCK_QUOTE_EXTEND_TO_BLANK_LINE, true);

        // Setup List Options for GitHub profile
        dataSet.set(Parser.LISTS_AUTO_LOOSE, false);
        dataSet.set(Parser.LISTS_AUTO_LOOSE, false);
        dataSet.set(Parser.LISTS_ITEM_TYPE_MISMATCH_TO_NEW_LIST, false);
        dataSet.set(Parser.LISTS_ITEM_TYPE_MISMATCH_TO_SUB_LIST, false);
        dataSet.set(Parser.LISTS_END_ON_DOUBLE_BLANK, false);
        dataSet.set(Parser.LISTS_ITEM_INDENT, 4);
        dataSet.set(Parser.LISTS_BULLET_ITEM_INTERRUPTS_PARAGRAPH, false);
        dataSet.set(Parser.LISTS_BULLET_ITEM_INTERRUPTS_ITEM_PARAGRAPH, true);
        dataSet.set(Parser.LISTS_ORDERED_ITEM_DOT_ONLY, true);
        dataSet.set(Parser.LISTS_ORDERED_ITEM_INTERRUPTS_PARAGRAPH, false);
        dataSet.set(Parser.LISTS_ORDERED_ITEM_INTERRUPTS_ITEM_PARAGRAPH, true);
        dataSet.set(Parser.LISTS_ORDERED_NON_ONE_ITEM_INTERRUPTS_PARAGRAPH, false);
        dataSet.set(Parser.LISTS_ORDERED_NON_ONE_ITEM_INTERRUPTS_ITEM_PARAGRAPH, true);
        dataSet.set(Parser.LISTS_ORDERED_LIST_MANUAL_START, false);

        if (options.abbreviations) {
            extensions.add(AbbreviationExtension.create());
            dataSet.set(AbbreviationExtension.ABBREVIATIONS_KEEP, KeepType.LAST);
        }

        if (options.anchorLinks) {
            extensions.add(AnchorLinkExtension.create());
            dataSet.set(AnchorLinkExtension.ANCHORLINKS_WRAP_TEXT, true);
        }

        if (options.autoLinks) {
            extensions.add(AutolinkExtension.create());
        }

        if (options.definitions) {
            // not implemented yet, but have placeholder
            extensions.add(DefinitionExtension.create());
        }

        if (options.fencedCode) {
            // disable fenced code blocks
            dataSet.set(Parser.MATCH_CLOSING_FENCE_CHARACTERS, false);
        } else {
            dataSet.set(Parser.FENCED_CODE_BLOCK_PARSER, false);
        }

        if (options.hardWraps) {
            dataSet.set(HtmlRenderer.SOFT_BREAK, "<br />\n");
            dataSet.set(HtmlRenderer.HARD_BREAK, "<br />\n<br />\n");
        }

        if (!options.atxHeadingSpace) {
            dataSet.set(Parser.HEADING_NO_ATX_SPACE, true);
        }
        dataSet.set(Parser.HEADING_NO_LEAD_SPACE, true);

        if (purpose == ForUseBy.PARSER) {
            // 3 for pegdown compatibility, 1 for commonmark, something else for GFM which will take 1 without trailing spaces if in a list, outside a list 1 or 2+ with spaces even if in a list
            dataSet.set(Parser.HEADING_SETEXT_MARKER_LENGTH, 3);
        }

        if (options.typographicQuotes || options.typographicSmarts) {
            // not implemented yet, have placeholder
            extensions.add(TypographicExtension.create());
            dataSet.set(TypographicExtension.ENABLE_SMARTS, options.typographicSmarts);
            dataSet.set(TypographicExtension.ENABLE_QUOTES, options.typographicQuotes);
        }

        dataSet.set(Parser.THEMATIC_BREAK_RELAXED_START, options.relaxedThematicBreak);

        if (options.strikeThrough) {
            extensions.add(StrikethroughExtension.create());
        }

        if (options.tables) {
            extensions.add(TablesExtension.create());
            dataSet.set(TablesExtension.TRIM_CELL_WHITESPACE, false);
            dataSet.set(TablesExtension.HEADER_SEPARATOR_COLUMN_MATCH, false);
        }

        if (options.taskListItems) {
            extensions.add(TaskListExtension.create());
        }

        if (options.wikiLinks) {
            extensions.add(WikiLinkExtension.create());
            dataSet.set(WikiLinkExtension.LINK_FIRST_SYNTAX, !options.wikiLinkGfmSyntax);
        }

        if (options.footnotes) {
            extensions.add(FootnoteExtension.create());
            dataSet.set(FootnoteExtension.FOOTNOTES_KEEP, KeepType.LAST);
        }

        // References compatibility
        dataSet.set(Parser.REFERENCES_KEEP, KeepType.LAST);

        if (options.tableOfContents) {
            extensions.add(SimTocExtension.create());
            dataSet.set(SimTocExtension.BLANK_LINE_SPACER, true);

            extensions.add(TocExtension.create());
            dataSet.set(TocExtension.LEVELS, TocOptions.getLevels(2, 3));
        }

        if (options.jekyllFrontMatter) {
            extensions.add(JekyllFrontMatterExtension.create());
        }

        if (options.emojiShortcuts) {
            // requires copying the emoji images to some directory and setting it here
            extensions.add(EmojiExtension.create());
            if (!options.emojiImageDirectory.isEmpty()) {
                dataSet.set(EmojiExtension.ROOT_IMAGE_PATH, options.emojiImageDirectory);
            }
        }

        if (purpose == ForUseBy.JAVAFX) {
            // set rendering options for JavaFX
            // set to true if java fx, else false
            dataSet.set(HtmlRenderer.INDENT_SIZE, 2);
            dataSet.set(Parser.LISTS_LOOSE_WHEN_PREV_HAS_TRAILING_BLANK_LINE, true);

            if (options.tables && options.renderTablesGFM) {
                dataSet.set(TablesExtension.COLUMN_SPANS, false)
                        .set(TablesExtension.MIN_HEADER_ROWS, 1)
                        .set(TablesExtension.MAX_HEADER_ROWS, 1)
                        .set(TablesExtension.APPEND_MISSING_COLUMNS, true)
                        .set(TablesExtension.DISCARD_EXTRA_COLUMNS, true)
                        .set(TablesExtension.HEADER_SEPARATOR_COLUMN_MATCH, true);
            }

            if (options.fencedCode) {
                dataSet.set(HtmlRenderer.FENCED_CODE_LANGUAGE_CLASS_PREFIX, "");
            }

            if (options.anchorLinks) {
                dataSet.set(AnchorLinkExtension.ANCHORLINKS_SET_ID, true);
                dataSet.set(AnchorLinkExtension.ANCHORLINKS_ANCHOR_CLASS, "anchor");
                dataSet.set(AnchorLinkExtension.ANCHORLINKS_SET_NAME, true);
                dataSet.set(AnchorLinkExtension.ANCHORLINKS_TEXT_PREFIX, "<span class=\"octicon octicon-link\"></span>");
            }

            if (options.taskListItems) {
                dataSet.set(TaskListExtension.ITEM_DONE_MARKER, "<span class=\"taskitem\">X</span>");
                dataSet.set(TaskListExtension.ITEM_NOT_DONE_MARKER, "<span class=\"taskitem\">O</span>");
            }

            dataSet.set(HtmlRenderer.RENDER_HEADER_ID, true);

            if (!options.wikiLinks) {
                dataSet.set(WikiLinkExtension.DISABLE_RENDERING, true);
            }
        } else if (purpose == ForUseBy.SWING) {
            // set rendering options for Swing
            dataSet.set(HtmlRenderer.INDENT_SIZE, 2);
            // dataSet.set(Parser.LISTS_LOOSE_ON_PREV_LOOSE_ITEM, true);

            if (options.tables && options.renderTablesGFM) {
                dataSet.set(TablesExtension.COLUMN_SPANS, false)
                        .set(TablesExtension.MIN_HEADER_ROWS, 1)
                        .set(TablesExtension.MAX_HEADER_ROWS, 1)
                        .set(TablesExtension.APPEND_MISSING_COLUMNS, true)
                        .set(TablesExtension.DISCARD_EXTRA_COLUMNS, true)
                        .set(TablesExtension.HEADER_SEPARATOR_COLUMN_MATCH, true);
            }

            if (options.fencedCode) {
                dataSet.set(HtmlRenderer.FENCED_CODE_LANGUAGE_CLASS_PREFIX, "");
            }

            if (options.anchorLinks) {
                dataSet.set(AnchorLinkExtension.ANCHORLINKS_SET_ID, false);
                dataSet.set(AnchorLinkExtension.ANCHORLINKS_ANCHOR_CLASS, "");
                dataSet.set(AnchorLinkExtension.ANCHORLINKS_SET_NAME, true);
                dataSet.set(AnchorLinkExtension.ANCHORLINKS_TEXT_PREFIX, "");
            }

            if (options.taskListItems) {
                dataSet.set(TaskListExtension.ITEM_DONE_MARKER, "");
                dataSet.set(TaskListExtension.ITEM_NOT_DONE_MARKER, "");
            }

            dataSet.set(HtmlRenderer.RENDER_HEADER_ID, true);

            if (!options.wikiLinks) {
                dataSet.set(WikiLinkExtension.DISABLE_RENDERING, true);
            }
        } else if (purpose == ForUseBy.HTML) {
            // set rendering options for Swing
            dataSet.set(HtmlRenderer.INDENT_SIZE, 2);
            // dataSet.set(Parser.LISTS_LOOSE_ON_PREV_LOOSE_ITEM, false);

            if (options.fencedCode) {
                dataSet.set(HtmlRenderer.FENCED_CODE_LANGUAGE_CLASS_PREFIX, "");
            }

            if (options.tables && options.renderTablesGFM) {
                dataSet.set(TablesExtension.COLUMN_SPANS, false)
                        .set(TablesExtension.MIN_HEADER_ROWS, 1)
                        .set(TablesExtension.MAX_HEADER_ROWS, 1)
                        .set(TablesExtension.APPEND_MISSING_COLUMNS, true)
                        .set(TablesExtension.DISCARD_EXTRA_COLUMNS, true)
                        .set(TablesExtension.HEADER_SEPARATOR_COLUMN_MATCH, true);
            }

            dataSet.set(HtmlRenderer.RENDER_HEADER_ID, false);
            dataSet.set(HtmlRenderer.GENERATE_HEADER_ID, true);
        }

        dataSet.set(Parser.EXTENSIONS, extensions);

        return dataSet;
    }

    /**
     * Checks if the string is not blank.
     * 
     * @param str
     *            the string to check.
     * @return {@code true} if the string is not blank.
     * @see #isBlank(String)
     */
    public static boolean isNotBlank(String str) {
        return !isBlank(str);
    }

    /**
     * Checks if the string is blank
     * 
     * @param string
     *            the string to check.
     *
     * @return {@code true} if the string is blank.
     * @see #isEmpty(String)
     */
    public static boolean isBlank(String string) {
        if (string == null || string.length() == 0) {
            return true;
        }
        int stringLength = string.length();
        for (int i = 0; i < stringLength; i++) {
            if ((!Character.isWhitespace(string.charAt(i)))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns a null-safe string.
     * 
     * @param value
     *            the value.
     * @see #getNullSafeString(String,String)
     * @see #notEmpty(String)
     *
     * @return the value, or an empty string if value is {@code null}. This is same as <code>getNullSafeString(value, "")</code>
     */
    public static String getNullSafeString(String value) {
        return getNullSafeString(value,"");
    }

    /**
     * Returns a null-safe string.
     *
     * @param value
     *            the value.
     * @param defaultValue
     *            value to be returned if is null
     * @see #getNullSafeString(String)
     * @see #notEmpty(String,String)
     *
     * @return the value, or defaultValue string if value is {@code null}.
     */
    public static String getNullSafeString(String value, String defaultValue) {
        return value == null ? defaultValue : value;
    }

    /**
     * Returns a empty-safe string.
     *
     * @param value
     *            the value.
     *
     * @return true if value is null or contains just blanks.
     */
    public static boolean isEmpty(String value) {
        return value ==  null || value.trim().length() == 0;
    }

    /**
     * Returns a empty-safe string.
     *
     * @param value
     *            the value.
     * @param defaultValue
     *            value to be returned if is empty
     * @see #isEmpty(String)
     *
     * @return the value, or defaultValue string if value is {{@link #isEmpty(String)}}.
     */
    public static String notEmpty(String value, String defaultValue) {
        return isEmpty(value) ? defaultValue  : value;
    }

    /**
     * Returns a empty-safe string.
     *
     * @param value
     *            the value.
     * @see #isEmpty(String)
     *
     * @return the value, or an empty string if value is {{@link #isEmpty(String)}}.
     */
    public static String notEmpty(String value) {
        return notEmpty(value, "");
    }

    public static String toValid3ByteUTF8String(String s) {
        final int length = s.length();
        StringBuilder b = new StringBuilder(length);
        for (int offset = 0; offset < length; ) {
            final int codepoint = s.codePointAt(offset);

            // do something with the codepoint
            if (codepoint > "\uFFFF".codePointAt(0)) {
                b.append("\uFFFD");
            } else {
                if (Character.isValidCodePoint(codepoint)) {
                    b.appendCodePoint(codepoint);
                } else {
                    b.append("\uFFFD");
                }
            }
            offset += Character.charCount(codepoint);
        }
        return b.toString();
    }

}