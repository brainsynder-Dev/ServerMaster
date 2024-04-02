package org.bsdevelopment.servermaster.utils.system;

import oshi.SystemInfo;

import java.text.DecimalFormat;

public class SystemUtilities {
    public static final long KILOBYTE = 1024;
    public static final long MEGABYTE = KILOBYTE * KILOBYTE;
    public static final long GIGABYTE = MEGABYTE * KILOBYTE;
    public static final long TERABYTE = GIGABYTE * KILOBYTE;

    private static final SystemInfo SYSTEM_INFO;

    static {
        SYSTEM_INFO = new SystemInfo();
    }

    public static SystemInfo getSystemInfo() {
        return SYSTEM_INFO;
    }

    public static String convertToStringRepresentation(final long value) {
        final long[] dividers = new long[]{TERABYTE, GIGABYTE, MEGABYTE, KILOBYTE, 1};
        final String[] units = new String[]{"TB", "GB", "MB", "KB", "B"};
        if (value < 1) throw new IllegalArgumentException("Invalid file size: " + value);

        String result = null;
        for (int i = 0; i < dividers.length; i++) {
            final long divider = dividers[i];
            if (value >= divider) {
                result = format(value, divider, units[i]);
                break;
            }
        }
        return result;
    }

    private static String format(long value, long divider, String unit) {
        final double result = divider > 1 ? (double) value / (double) divider : (double) value;
        return new DecimalFormat("#,##0.#").format(result) + " " + unit;
    }
}
