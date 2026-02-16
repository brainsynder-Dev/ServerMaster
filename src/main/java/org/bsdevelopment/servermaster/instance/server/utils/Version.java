package org.bsdevelopment.servermaster.instance.server.utils;

import java.util.ArrayList;
import java.util.List;

public final class Version implements Comparable<Version> {
    private final String version;
    private final List<Object> sequence;
    private final List<Object> pre;
    private final List<Object> build;

    private static int takeNumber(String s, int i, List<Object> acc) {
        char c = s.charAt(i);
        int d = (c - '0');
        int n = s.length();
        while (++i < n) {
            c = s.charAt(i);
            if (c >= '0' && c <= '9') {
                d = d * 10 + (c - '0');
                continue;
            }
            break;
        }
        acc.add(d);
        return i;
    }

    private static int takeString(String s, int i, List<Object> acc) {
        int b = i;
        int n = s.length();
        while (++i < n) {
            char c = s.charAt(i);
            if (c != '.' && c != '-' && c != '+' && !(c >= '0' && c <= '9')) continue;
            break;
        }
        acc.add(s.substring(b, i));
        return i;
    }

    private Version(String v) {
        if (v == null) throw new IllegalArgumentException("Null version string");

        int n = v.length();
        if (n == 0) throw new IllegalArgumentException("Empty version string");

        int i = 0;
        char c = v.charAt(i);
        if (!(c >= '0' && c <= '9')) throw new IllegalArgumentException(v + ": Version string does not start" + " with a number");

        List<Object> sequence = new ArrayList<>(4);
        List<Object> pre = new ArrayList<>(2);
        List<Object> build = new ArrayList<>(2);

        i = takeNumber(v, i, sequence);

        while (i < n) {
            c = v.charAt(i);
            if (c == '.') {
                i++;
                continue;
            }

            if (c == '-' || c == '+') {
                i++;
                break;
            }

            if (c >= '0' && c <= '9') i = takeNumber(v, i, sequence);
            else i = takeString(v, i, sequence);
        }

        if (c == '-' && i >= n) throw new IllegalArgumentException(v + ": Empty pre-release");

        while (i < n) {
            c = v.charAt(i);
            if (c >= '0' && c <= '9') i = takeNumber(v, i, pre);
            else i = takeString(v, i, pre);

            if (i >= n) break;

            c = v.charAt(i);
            if (c == '.' || c == '-') {
                i++;
                continue;
            }

            if (c == '+') {
                i++;
                break;
            }
        }

        if (c == '+' && i >= n) throw new IllegalArgumentException(v + ": Empty pre-release");

        while (i < n) {
            c = v.charAt(i);
            if (c >= '0' && c <= '9') i = takeNumber(v, i, build);
            else i = takeString(v, i, build);

            if (i >= n) break;

            c = v.charAt(i);
            if (c == '.' || c == '-' || c == '+') {
                i++;
            }
        }

        this.version = v;
        this.sequence = sequence;
        this.pre = pre;
        this.build = build;
    }

    public static Version parse(String v) {
        return new Version(v);
    }

    private int cmp(Object o1, Object o2) {
        return ((Comparable) o1).compareTo(o2);
    }

    private int compareTokens(List<Object> ts1, List<Object> ts2) {
        int n = Math.min(ts1.size(), ts2.size());
        for (int i = 0; i < n; i++) {
            Object o1 = ts1.get(i);
            Object o2 = ts2.get(i);
            if ((o1 instanceof Integer && o2 instanceof Integer) || (o1 instanceof String && o2 instanceof String)) {
                int c = cmp(o1, o2);
                if (c == 0) continue;

                return c;
            }
            int c = o1.toString().compareTo(o2.toString());
            if (c == 0) continue;
            return c;
        }
        List<Object> rest = ts1.size() > ts2.size() ? ts1 : ts2;
        int e = rest.size();
        for (int i = n; i < e; i++) {
            Object o = rest.get(i);
            if (o instanceof Integer && ((Integer) o) == 0) continue;
            return ts1.size() - ts2.size();
        }
        return 0;
    }

    @Override
    public int compareTo(Version that) {
        int c = compareTokens(this.sequence, that.sequence);
        if (c != 0) return c;
        if (this.pre.isEmpty()) {
            if (!that.pre.isEmpty()) return +1;
        } else {
            if (that.pre.isEmpty()) return -1;
        }
        c = compareTokens(this.pre, that.pre);
        if (c != 0) return c;
        return compareTokens(this.build, that.build);
    }

    @Override
    public boolean equals(Object ob) {
        if (!(ob instanceof Version)) return false;
        return compareTo((Version) ob) == 0;
    }

    @Override
    public int hashCode() {
        return version.hashCode();
    }

    @Override
    public String toString() {
        return version;
    }

}