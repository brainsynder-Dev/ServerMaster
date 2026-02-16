/*
 * Copyright © 2025
 * BSDevelopment <https://bsdevelopment.org>
 */
package org.bsdevelopment.servermaster.utils;

import java.text.DecimalFormat;

public interface MemoryUnit {
    MemoryUnit BYTE     = new MemoryBuilder("B", 1L);
    MemoryUnit KILOBYTE = new MemoryBuilder("KB", 1L << 10); // 1024
    MemoryUnit MEGABYTE = new MemoryBuilder("MB", 1L << 20); // 1024^2
    MemoryUnit GIGABYTE = new MemoryBuilder("GB", 1L << 30); // 1024^3
    MemoryUnit TERABYTE = new MemoryBuilder("TB", 1L << 40); // 1024^4
    MemoryUnit PETABYTE = new MemoryBuilder("PB", 1L << 50); // 1024^5
    MemoryUnit EXABYTE  = new MemoryBuilder("EB", 1L << 60); // 1024^6

    long getUnitSize();
    String getSuffix();

    String format(long sizeInBytes);

    long convert(long sizeInBytes);

    static long convertTo(long sizeInBytes, MemoryUnit unit) {
        return unit.convert(sizeInBytes);
    }

    static MemoryUnit bestFit(long sizeInBytes) {
        long abs = Math.abs(sizeInBytes);

        if (abs >= EXABYTE.getUnitSize())  return EXABYTE;
        if (abs >= PETABYTE.getUnitSize()) return PETABYTE;
        if (abs >= TERABYTE.getUnitSize()) return TERABYTE;
        if (abs >= GIGABYTE.getUnitSize()) return GIGABYTE;
        if (abs >= MEGABYTE.getUnitSize()) return MEGABYTE;
        if (abs >= KILOBYTE.getUnitSize()) return KILOBYTE;
        return BYTE;
    }

    static String formatBest(long sizeInBytes) {
        return bestFit(sizeInBytes).format(sizeInBytes);
    }

    class MemoryBuilder implements MemoryUnit {
        private static final DecimalFormat ONE_DECIMAL = new DecimalFormat("0.#");

        private final String suffix;
        private final long unitSize;

        MemoryBuilder(String suffix, long unitSize) {
            this.suffix = suffix;
            this.unitSize = unitSize;
        }

        @Override
        public long getUnitSize() {
            return unitSize;
        }

        @Override
        public String getSuffix() {
            return suffix;
        }

        @Override
        public String format(long sizeInBytes) {
            if (unitSize <= 0) return sizeInBytes + "B";

            double value = (double) sizeInBytes / (double) unitSize;

            if (unitSize == 1L) return sizeInBytes + suffix;
            return ONE_DECIMAL.format(value) + suffix;
        }

        @Override
        public long convert(long sizeInBytes) {
            if (unitSize <= 0) return 0;
            return sizeInBytes / unitSize;
        }
    }
}
