package org.bsdevelopment.serverapp.utils;

public class WordUtils {
    /**
     * <p>Capitalizes all the whitespace separated words in a String.
     * Only the first letter of each word is changed. To convert the
     * rest of each word to lowercase at the same time,
     * use {@link #capitalizeFully(String)}.</p>
     *
     * <p>Whitespace is defined by {@link Character#isWhitespace(char)}.
     * A <code>null</code> input String returns <code>null</code>.
     * Capitalization uses the unicode title case, normally equivalent to
     * upper case.</p>
     *
     * <pre>
     * WordUtils.capitalize(null)        = null
     * WordUtils.capitalize("")          = ""
     * WordUtils.capitalize("i am FINE") = "I Am FINE"
     * </pre>
     *
     * @param input the String to capitalize, may be null
     * @return capitalized String, <code>null</code> if null String input
     * @see #uncapitalize(String)
     * @see #capitalizeFully(String)
     */
    public static String capitalize(String input) {
        return capitalize(input, null);
    }

    /**
     * <p>Capitalizes all the delimiter separated words in a String.
     * Only the first letter of each word is changed. To convert the
     * rest of each word to lowercase at the same time,
     * use {@link #capitalizeFully(String, char[])}.</p>
     *
     * <p>The delimiters represent a set of characters understood to separate words.
     * The first string character and the first non-delimiter character after a
     * delimiter will be capitalized. </p>
     *
     * <p>A <code>null</code> input String returns <code>null</code>.
     * Capitalization uses the unicode title case, normally equivalent to
     * upper case.</p>
     *
     * <pre>
     * WordUtils.capitalize(null, *)            = null
     * WordUtils.capitalize("", *)              = ""
     * WordUtils.capitalize(*, new char[0])     = *
     * WordUtils.capitalize("i am fine", null)  = "I Am Fine"
     * WordUtils.capitalize("i aM.fine", {'.'}) = "I aM.Fine"
     * </pre>
     *
     * @param input      the String to capitalize, may be null
     * @param delimiters set of characters to determine capitalization, null means whitespace
     * @return capitalized String, <code>null</code> if null String input
     * @see #uncapitalize(String)
     * @see #capitalizeFully(String)
     * @since 2.1
     */
    public static String capitalize(String input, char[] delimiters) {
        int delimLen = (delimiters == null ? -1 : delimiters.length);

        if (input == null || input.length() == 0 || delimLen == 0) return input;

        int strLen = input.length();
        StringBuilder builder = new StringBuilder(strLen);
        boolean capitalizeNext = true;
        for (int i = 0; i < strLen; i++) {
            char ch = input.charAt(i);

            if (isDelimiter(ch, delimiters)) {
                builder.append(ch);
                capitalizeNext = true;
            } else if (capitalizeNext) {
                builder.append(Character.toTitleCase(ch));
                capitalizeNext = false;
            } else {
                builder.append(ch);
            }
        }

        return builder.toString();
    }

    /**
     * <p>Converts all the whitespace separated words in a String into capitalized words,
     * that is each word is made up of a titlecase character and then a series of
     * lowercase characters.  </p>
     *
     * <p>Whitespace is defined by {@link Character#isWhitespace(char)}.
     * A <code>null</code> input String returns <code>null</code>.
     * Capitalization uses the unicode title case, normally equivalent to
     * upper case.</p>
     *
     * <pre>
     * WordUtils.capitalizeFully(null)        = null
     * WordUtils.capitalizeFully("")          = ""
     * WordUtils.capitalizeFully("i am FINE") = "I Am Fine"
     * </pre>
     *
     * @param input the String to capitalize, may be null
     * @return capitalized String, <code>null</code> if null String input
     */
    public static String capitalizeFully(String input) {
        return capitalizeFully(input, null);
    }

    /**
     * <p>Converts all the delimiter separated words in a String into capitalized words,
     * that is each word is made up of a titlecase character and then a series of
     * lowercase characters. </p>
     *
     * <p>The delimiters represent a set of characters understood to separate words.
     * The first string character and the first non-delimiter character after a
     * delimiter will be capitalized. </p>
     *
     * <p>A <code>null</code> input String returns <code>null</code>.
     * Capitalization uses the unicode title case, normally equivalent to
     * upper case.</p>
     *
     * <pre>
     * WordUtils.capitalizeFully(null, *)            = null
     * WordUtils.capitalizeFully("", *)              = ""
     * WordUtils.capitalizeFully(*, null)            = *
     * WordUtils.capitalizeFully(*, new char[0])     = *
     * WordUtils.capitalizeFully("i aM.fine", {'.'}) = "I am.Fine"
     * </pre>
     *
     * @param input      the String to capitalize, may be null
     * @param delimiters set of characters to determine capitalization, null means whitespace
     * @return capitalized String, <code>null</code> if null String input
     * @since 2.1
     */
    public static String capitalizeFully(String input, char[] delimiters) {
        int delimLen = (delimiters == null ? -1 : delimiters.length);

        if (input == null || input.length() == 0 || delimLen == 0) return input;

        input = input.toLowerCase();
        return capitalize(input, delimiters);
    }

    /**
     * <p>Uncapitalizes all the whitespace separated words in a String.
     * Only the first letter of each word is changed.</p>
     *
     * <p>Whitespace is defined by {@link Character#isWhitespace(char)}.
     * A <code>null</code> input String returns <code>null</code>.</p>
     *
     * <pre>
     * WordUtils.uncapitalize(null)        = null
     * WordUtils.uncapitalize("")          = ""
     * WordUtils.uncapitalize("I Am FINE") = "i am fINE"
     * </pre>
     *
     * @param input the String to uncapitalize, may be null
     * @return uncapitalized String, <code>null</code> if null String input
     * @see #capitalize(String)
     */
    public static String uncapitalize(String input) {
        return uncapitalize(input, null);
    }

    /**
     * <p>Uncapitalizes all the whitespace separated words in a String.
     * Only the first letter of each word is changed.</p>
     *
     * <p>The delimiters represent a set of characters understood to separate words.
     * The first string character and the first non-delimiter character after a
     * delimiter will be uncapitalized. </p>
     *
     * <p>Whitespace is defined by {@link Character#isWhitespace(char)}.
     * A <code>null</code> input String returns <code>null</code>.</p>
     *
     * <pre>
     * WordUtils.uncapitalize(null, *)            = null
     * WordUtils.uncapitalize("", *)              = ""
     * WordUtils.uncapitalize(*, null)            = *
     * WordUtils.uncapitalize(*, new char[0])     = *
     * WordUtils.uncapitalize("I AM.FINE", {'.'}) = "i AM.fINE"
     * </pre>
     *
     * @param input      the String to uncapitalize, may be null
     * @param delimiters set of characters to determine uncapitalization, null means whitespace
     * @return uncapitalized String, <code>null</code> if null String input
     * @see #capitalize(String)
     * @since 2.1
     */
    public static String uncapitalize(String input, char[] delimiters) {
        int delimLen = (delimiters == null ? -1 : delimiters.length);

        if (input == null || input.length() == 0 || delimLen == 0) return input;

        int strLen = input.length();
        StringBuilder buffer = new StringBuilder(strLen);
        boolean uncapitalizeNext = true;
        for (int i = 0; i < strLen; i++) {
            char ch = input.charAt(i);

            if (isDelimiter(ch, delimiters)) {
                buffer.append(ch);
                uncapitalizeNext = true;
            } else if (uncapitalizeNext) {
                buffer.append(Character.toLowerCase(ch));
                uncapitalizeNext = false;
            } else {
                buffer.append(ch);
            }
        }
        return buffer.toString();
    }

    /**
     * <p>Swaps the case of a String using a word based algorithm.</p>
     *
     * <ul>
     *  <li>Upper case character converts to Lower case</li>
     *  <li>Title case character converts to Lower case</li>
     *  <li>Lower case character after Whitespace or at start converts to Title case</li>
     *  <li>Other Lower case character converts to Upper case</li>
     * </ul>
     *
     * <p>Whitespace is defined by {@link Character#isWhitespace(char)}.
     * A <code>null</code> input String returns <code>null</code>.</p>
     *
     * <pre>
     * StringUtils.swapCase(null)                 = null
     * StringUtils.swapCase("")                   = ""
     * StringUtils.swapCase("The dog has a BONE") = "tHE DOG HAS A bone"
     * </pre>
     *
     * @param input the String to swap case, may be null
     * @return the changed String, <code>null</code> if null String input
     */
    public static String swapCase(String input) {
        int length;
        if (input == null || (length = input.length()) == 0) return input;

        StringBuilder builder = new StringBuilder(length);

        boolean whitespace = true;
        char character = 0;
        char newCharacter = 0;

        for (int i = 0; i < length; i++) {
            character = input.charAt(i);
            if (Character.isUpperCase(character)) {
                newCharacter = Character.toLowerCase(character);
            } else if (Character.isTitleCase(character)) {
                newCharacter = Character.toLowerCase(character);
            } else if (Character.isLowerCase(character)) {
                if (whitespace) {
                    newCharacter = Character.toTitleCase(character);
                } else {
                    newCharacter = Character.toUpperCase(character);
                }
            } else {
                newCharacter = character;
            }
            builder.append(newCharacter);
            whitespace = Character.isWhitespace(character);
        }
        return builder.toString();
    }

    /**
     * <p>Extracts the initial letters from each word in the String.</p>
     *
     * <p>The first letter of the string and all first letters after
     * whitespace are returned as a new string.
     * Their case is not changed.</p>
     *
     * <p>Whitespace is defined by {@link Character#isWhitespace(char)}.
     * A <code>null</code> input String returns <code>null</code>.</p>
     *
     * <pre>
     * WordUtils.initials(null)             = null
     * WordUtils.initials("")               = ""
     * WordUtils.initials("Ben John Lee")   = "BJL"
     * WordUtils.initials("Ben J.Lee")      = "BJ"
     * </pre>
     *
     * @param input the String to get initials from, may be null
     * @return String of initial letters, <code>null</code> if null String input
     * @see #initials(String, char[])
     * @since 2.2
     */
    public static String initials(String input) {
        return initials(input, null);
    }

    /**
     * <p>Extracts the initial letters from each word in the String.</p>
     *
     * <p>The first letter of the string and all first letters after the
     * defined delimiters are returned as a new string.
     * Their case is not changed.</p>
     *
     * <p>If the delimiters array is null, then Whitespace is used.
     * Whitespace is defined by {@link Character#isWhitespace(char)}.
     * A <code>null</code> input String returns <code>null</code>.
     * An empty delimiter array returns an empty String.</p>
     *
     * <pre>
     * WordUtils.initials(null, *)                = null
     * WordUtils.initials("", *)                  = ""
     * WordUtils.initials("Ben John Lee", null)   = "BJL"
     * WordUtils.initials("Ben J.Lee", null)      = "BJ"
     * WordUtils.initials("Ben J.Lee", [' ','.']) = "BJL"
     * WordUtils.initials(*, new char[0])         = ""
     * </pre>
     *
     * @param input      the String to get initials from, may be null
     * @param delimiters set of characters to determine words, null means whitespace
     * @return String of initial letters, <code>null</code> if null String input
     * @see #initials(String)
     * @since 2.2
     */
    public static String initials(String input, char[] delimiters) {
        if (input == null || input.length() == 0) return input;
        if (delimiters != null && delimiters.length == 0) return "";

        int length = input.length();
        char[] buf = new char[length / 2 + 1];
        int count = 0;
        boolean lastWasGap = true;

        for (int i = 0; i < length; i++) {
            char ch = input.charAt(i);

            if (isDelimiter(ch, delimiters)) {
                lastWasGap = true;
            } else if (lastWasGap) {
                buf[count++] = ch;
                lastWasGap = false;
            }
        }

        return new String(buf, 0, count);
    }

    /**
     * Is the character a delimiter.
     *
     * @param character  the character to check
     * @param delimiters the delimiters
     * @return true if it is a delimiter
     */
    private static boolean isDelimiter(char character, char[] delimiters) {
        if (delimiters == null) return Character.isWhitespace(character);

        for (char delimiter : delimiters) {
            if (character == delimiter) return true;
        }
        return false;
    }
}