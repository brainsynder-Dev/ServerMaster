package org.bsdevelopment.servermaster.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

public class AdvString {
    public static String getPaddedString(String text, char paddingChar, int maxPadding, AlignText alignText) {
        if (text == null) {
            throw new NullPointerException("Can not add padding in null String!");
        }

        int length = text.length();
        int padding = (maxPadding - length) / 2;//decide left and right padding
        if (padding <= 0) {
            return text;// return actual String if padding is less than or equal to 0
        }

        String empty = "", hash = "#";//hash is used as a place holder

        // extra character in case of String with even length
        int extra = (length % 2 == 0) ? 1 : 0;

        String leftPadding = "%" + padding + "s";
        String rightPadding = "%" + (padding - extra) + "s";

        // Will align the text to the selected side
        switch (alignText) {
            case LEFT:
                leftPadding = "%s";
                rightPadding = "%" + (padding + (padding - extra)) + "s";
                break;
            case RIGHT:
                rightPadding = "%s";
                leftPadding = "%" + (padding + (padding - extra)) + "s";
                break;
        }

        String strFormat = leftPadding + "%s" + rightPadding;
        String formattedString = String.format(strFormat, empty, hash, empty);

        //Replace space with * and hash with provided String
        String paddedString = formattedString.replace(' ', paddingChar).replace(hash, text);
        return paddedString;
    }

    /**
     * Replaces the last known {@param target} text in the {@param haystack} with the {@param replacement}
     *
     * @param target      -- The text being targeted for replacement
     * @param replacement -- The replacement of the targeted text
     * @param haystack    -- The text being scanned
     * @return -- Returns the text after any replacement that has happened
     * Will return itself if it could not find the targeted text
     */
    public static String replaceLast(String target, String replacement, String haystack) {
        int pos = haystack.lastIndexOf(target);
        if (pos > -1) {
            return haystack.substring(0, pos)
                    + replacement
                    + haystack.substring(pos + target.length());
        } else {
            return haystack;
        }
    }

    /**
     * Checks if the {@param string} contains the {@param character} X amount of times (the count)
     *
     * @param string    The text being checked
     * @param character the character you are checking for
     * @param count     How many times should that character be in the string
     *                  If count is set to -1 it will use the default String.contains method
     * @return true if it contains the character X amount of times, else false
     */
    public static boolean contains(String string, char character, int count) {
        if (count == -1) return string.contains(String.valueOf(character));

        int i = 0;
        for (char c : string.toCharArray()) {
            if (c == character) i++;
        }

        return (i == count);
    }

    /**
     * Retrieves the text that comes after the first instance of 'needle'
     *
     * @param needle
     * @param haystack -- the text that is being scanned
     * @return -- Returns the text that is after 'needle'
     */
    public static String after(String needle, String haystack) {
        return haystack.substring((haystack.indexOf(needle) + needle.length()));
    }

    /**
     * Retrieves the text that comes after the last instance of 'needle'
     *
     * @param needle
     * @param haystack
     * @return -- Returns the text that is after the last 'needle'
     */
    public static String afterLast(String needle, String haystack) {
        return haystack.substring(reversePos(needle, haystack) + needle.length());
    }

    /**
     * Retrieves the text that comes before the first instance of 'needle'
     *
     * @param needle
     * @param haystack -- the text that is being scanned
     * @return -- Returns the text that is before 'needle'
     */
    public static String before(String needle, String haystack) {
        return haystack.substring(0, haystack.indexOf(needle));
    }

    /**
     * Retrieves the text that comes after the last instance of 'needle'
     *
     * @param needle
     * @param haystack -- the text that is being scanned
     * @return -- Returns the text that is before the last instance of 'needle'
     */
    public static String beforeLast(String needle, String haystack) {
        return haystack.substring(0, reversePos(needle, haystack));
    }

    /**
     * Retrieves the text that is between the first instance of 'first' and 'last'
     *
     * @param first
     * @param last
     * @param haystack -- the text that is being scanned
     * @return -- Returns the text that is between the instance of 'first' and 'last'
     */
    public static String between(String first, String last, String haystack) {
        return before(last, after(first, haystack));
    }

    /**
     * Retrieves the text that is between the last instance of 'first' and 'last'
     *
     * @param first
     * @param last
     * @param haystack -- the text that is being scanned
     * @return -- Returns the text that is between the last instance of 'first' and 'last'
     */
    public static String betweenLast(String first, String last, String haystack) {
        return afterLast(first, beforeLast(last, haystack));
    }


    public static int reversePos(String needle, String haystack) {
        int pos = reverse(haystack).indexOf(reverse(needle));
        return haystack.length() - pos - needle.length();
    }

    /**
     * Reverses the 'input' text
     *
     * @param input
     * @return -- Returns the 'input' in reverse
     */
    public static String reverse(String input) {
        char[] chars = input.toCharArray();
        List<Character> characters = new ArrayList<>();

        for (char c : chars)
            characters.add(c);

        Collections.reverse(characters);
        ListIterator iterator = characters.listIterator();
        StringBuilder builder = new StringBuilder();

        while (iterator.hasNext())
            builder.append(iterator.next());
        return builder.toString();
    }

    /**
     * Scrambles the 'input' text in random configurations
     *
     * @param input
     * @return -- Returns the 'input' randomly scambled
     */
    public static String scramble(String input) {
        StringBuilder out = new StringBuilder();
        for (String part : input.split(" ")) {
            List<Character> characters = new ArrayList<>();
            for (char c : part.toCharArray()) {
                characters.add(c);
            }
            StringBuilder output = new StringBuilder(part.length());
            while (characters.size() != 0) {
                int rndm = (int) (Math.random() * characters.size());
                output.append(characters.remove(rndm));
            }
            out.append(output).append(' ');
        }
        return out.toString().trim();
    }

    public enum AlignText {
        LEFT, RIGHT, CENTER
    }
}
