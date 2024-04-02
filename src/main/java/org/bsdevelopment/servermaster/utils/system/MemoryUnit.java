package org.bsdevelopment.servermaster.utils.system;

import java.text.DecimalFormat;

public interface MemoryUnit {
    MemoryUnit KILOBYTE = new MemoryBuilder("kB", 1024);
    MemoryUnit MEGABYTE = new MemoryBuilder("MB", (KILOBYTE.getUnitSize() * KILOBYTE.getUnitSize()));
    MemoryUnit GIGABYTE = new MemoryBuilder("GB", (MEGABYTE.getUnitSize() * KILOBYTE.getUnitSize()));
    MemoryUnit TERABYTE = new MemoryBuilder("TB", (GIGABYTE.getUnitSize() * KILOBYTE.getUnitSize()));
    MemoryUnit PETABYTE = new MemoryBuilder("PB", (TERABYTE.getUnitSize() * KILOBYTE.getUnitSize()));
    MemoryUnit EXABYTE  = new MemoryBuilder("EB", (PETABYTE.getUnitSize() * KILOBYTE.getUnitSize()));


    long getUnitSize();
    String getSuffix();
    String format(long size);
    long convert(long size);

    static long convertTo(long size, MemoryUnit unit) {
        return (size / unit.getUnitSize());
    }

    class MemoryBuilder implements MemoryUnit {
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
        public String format(long size) {
            return format(size, unitSize, suffix);
        }

        @Override
        public long convert(long size) {
            return (size / getUnitSize());
        }

        String format(long size, long base, String unit) {
            return new DecimalFormat("#,##0.#").format(Long.valueOf(size).doubleValue() / Long.valueOf(base).doubleValue()) + unit;
        }
    }
}