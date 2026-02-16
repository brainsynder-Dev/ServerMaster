package org.bsdevelopment.servermaster.instance;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class InstanceCatalog {
    private static final Pattern PAPER_LIKE = Pattern.compile("(?i)^(paper|purpur|folia|leaves|pufferfish)-([0-9]+(?:\\.[0-9]+)+)-([0-9]+)\\.jar$");
    private static final Pattern SPIGOT = Pattern.compile("(?i)^spigot-([0-9]+(?:\\.[0-9]+)+)\\.jar$");
    private final Path serverRoot;

    public InstanceCatalog(Path serverRoot) {
        this.serverRoot = serverRoot;
        try {
            Files.createDirectories(instanceRoot());
        } catch (IOException e) {
            throw new RuntimeException("Failed to create 'instance' directory: " + instanceRoot(), e);
        }
    }

    public Path instanceRoot() {
        return serverRoot.resolve("instance");
    }

    private Path typeDir(String type) {
        return instanceRoot().resolve(type);
    }

    public List<String> listServerTypes() throws IOException {
        Path root = instanceRoot();
        if (!Files.isDirectory(root)) return List.of();

        try (Stream<Path> s = Files.list(root)) {
            return s.filter(Files::isDirectory)
                    .map(p -> p.getFileName().toString())
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .toList();
        }
    }

    public List<Entry> listEntries(String type) throws IOException {
        Path dir = typeDir(type);
        if (!Files.isDirectory(dir)) return List.of();

        List<Entry> out = new ArrayList<>();
        try (Stream<Path> s = Files.list(dir)) {
            for (Path path : s.filter(Files::isRegularFile).toList()) {
                String name = path.getFileName().toString();
                if (!name.toLowerCase(Locale.ROOT).endsWith(".jar")) continue;
                parseJarName(type, name, path).ifPresent(out::add);
            }
        }

        out.sort(Comparator
                .comparing(Entry::version, InstanceCatalog::compareVersionsLoose)
                .thenComparing(e -> e.build().orElse(Integer.MIN_VALUE))
                .thenComparing(e -> e.jarFileName().toLowerCase(Locale.ROOT)));

        return out;
    }

    public Path copyToRuntimeJar(String type, String version, Integer build) throws IOException {
        Entry entry = findJar(type, version, build)
                .orElseThrow(() -> new IOException("No matching jar found for: type=" + type + ", version=" + version + ", build=" + build));

        Path runtimeJar = serverRoot.resolve("server.jar");
        Files.createDirectories(serverRoot);
        Files.copy(entry.jarPath(), runtimeJar, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
        return runtimeJar;
    }

    public List<String> listVersions(String type) throws IOException {
        return listEntries(type).stream()
                .map(Entry::version)
                .distinct()
                .sorted((a, b) -> compareVersionsLoose(b, a))
                .toList();
    }

    public List<Integer> listBuilds(String type, String version) throws IOException {
        return listEntries(type).stream()
                .filter(e -> e.version().equals(version))
                .map(Entry::build)
                .flatMap(Optional::stream)
                .distinct()
                .sorted(Comparator.reverseOrder())
                .toList();
    }

    public Optional<Entry> findJar(String type, String version, Integer build) throws IOException {
        Path dir = typeDir(type);
        if (!Files.isDirectory(dir)) return Optional.empty();

        String typeLower = type.toLowerCase(Locale.ROOT);

        if (build != null) {
            Path candidate = dir.resolve(typeLower + "-" + version + "-" + build + ".jar");
            if (Files.isRegularFile(candidate)) {
                return Optional.of(new Entry(type, version, Optional.of(build), candidate.getFileName().toString(), candidate));
            }
            return Optional.empty();
        }

        Path spigotCandidate = dir.resolve("spigot-" + version + ".jar");
        if (Files.isRegularFile(spigotCandidate)) {
            return Optional.of(new Entry(type, version, Optional.empty(), spigotCandidate.getFileName().toString(), spigotCandidate));
        }

        Pattern expected = Pattern.compile("(?i)^" + Pattern.quote(typeLower) + "-" + Pattern.quote(version) + "-([0-9]+)\\.jar$");

        Entry best = null;
        int bestBuild = Integer.MIN_VALUE;

        try (Stream<Path> s = Files.list(dir)) {
            for (Path p : s.filter(Files::isRegularFile).toList()) {
                String fileName = p.getFileName().toString();
                Matcher m = expected.matcher(fileName);
                if (!m.matches()) continue;

                int b;
                try {
                    b = Integer.parseInt(m.group(1));
                } catch (NumberFormatException ignored) {
                    continue;
                }

                if (b > bestBuild) {
                    bestBuild = b;
                    best = new Entry(type, version, Optional.of(b), fileName, p);
                }
            }
        }

        return Optional.ofNullable(best);
    }

    private Optional<Entry> parseJarName(String folderType, String jarFileName, Path jarPath) {
        Matcher m1 = PAPER_LIKE.matcher(jarFileName);
        if (m1.matches()) {
            String version = m1.group(2);
            int build = Integer.parseInt(m1.group(3));
            return Optional.of(new Entry(folderType, version, Optional.of(build), jarFileName, jarPath));
        }

        Matcher m2 = SPIGOT.matcher(jarFileName);
        if (m2.matches()) {
            String version = m2.group(1);
            return Optional.of(new Entry(folderType, version, Optional.empty(), jarFileName, jarPath));
        }

        return Optional.empty();
    }

    public record Entry(String type, String version, Optional<Integer> build, String jarFileName, Path jarPath) {}

    private static int compareVersionsLoose(String a, String b) {
        int[] pa = parseVersionParts(a);
        int[] pb = parseVersionParts(b);
        int len = Math.max(pa.length, pb.length);
        for (int i = 0; i < len; i++) {
            int va = i < pa.length ? pa[i] : 0;
            int vb = i < pb.length ? pb[i] : 0;
            if (va != vb) return Integer.compare(va, vb);
        }
        return a.compareToIgnoreCase(b);
    }

    private static int[] parseVersionParts(String v) {
        String cleaned = v.trim().replaceAll("[^0-9.]", "");
        if (cleaned.isEmpty()) return new int[]{0};
        String[] parts = cleaned.split("\\.");
        int[] out = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            try {
                out[i] = Integer.parseInt(parts[i]);
            } catch (NumberFormatException e) {
                out[i] = 0;
            }
        }
        return out;
    }
}
