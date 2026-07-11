package org.bsdevelopment.servermaster.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

public final class BuildToolsArtifacts {

    public record BuiltJar(Path path, String groupId, String artifactId, String version, String classifier, String fileName, Path pomFile) {
        public String deployKey() {
            return (classifier == null || classifier.isBlank()) ? artifactId : artifactId + ":" + classifier;
        }

        public String displayName() {
            String label = artifactId + ((classifier == null || classifier.isBlank()) ? "" : "  (" + classifier + ")");
            return label + "   ·   " + version;
        }
    }

    /**
     * Lists the deployable Spigot artifacts BuildTools installed into the local Maven
     * repository (~/.m2/repository/org/spigotmc) for the most recently built version.
     * Only jars that are actually present are returned, each paired with its .pom.
     */
    public static List<BuiltJar> scan() {
        Path repository = spigotRepository();
        if (repository == null || !Files.isDirectory(repository)) return List.of();

        String version = newestVersion(repository);
        if (version == null) return List.of();

        List<BuiltJar> out = new ArrayList<>();
        try (Stream<Path> artifactDirs = Files.list(repository)) {
            for (Path artifactDir : artifactDirs.filter(Files::isDirectory).toList()) {
                Path versionDir = artifactDir.resolve(version);
                if (Files.isDirectory(versionDir)) {
                    collect(versionDir, artifactDir.getFileName().toString(), version, out);
                }
            }
        } catch (IOException ignored) {
        }

        out.sort((a, b) -> a.fileName().compareToIgnoreCase(b.fileName()));
        return out;
    }

    private static void collect(Path versionDir, String artifactId, String version, List<BuiltJar> out) {
        String base = artifactId + "-" + version;
        Path pom = versionDir.resolve(base + ".pom");
        Path pomFile = Files.isRegularFile(pom) ? pom : null;

        try (Stream<Path> files = Files.list(versionDir)) {
            for (Path file : files.filter(Files::isRegularFile).toList()) {
                String name = file.getFileName().toString();
                String lower = name.toLowerCase(Locale.ROOT);
                if (!lower.endsWith(".jar") || lower.endsWith("-sources.jar") || lower.endsWith("-javadoc.jar")) continue;

                String withoutExtension = name.substring(0, name.length() - 4);
                if (!withoutExtension.startsWith(base)) continue;

                String classifier;
                if (withoutExtension.equals(base)) {
                    classifier = null;
                } else if (withoutExtension.startsWith(base + "-")) {
                    classifier = withoutExtension.substring(base.length() + 1);
                } else {
                    continue;
                }

                out.add(new BuiltJar(file, "org.spigotmc", artifactId, version, classifier, name, pomFile));
            }
        } catch (IOException ignored) {
        }
    }

    private static String newestVersion(Path repository) {
        String newest = null;
        long newestTime = Long.MIN_VALUE;

        try (Stream<Path> artifactDirs = Files.list(repository)) {
            for (Path artifactDir : artifactDirs.filter(Files::isDirectory).toList()) {
                String artifactId = artifactDir.getFileName().toString();
                try (Stream<Path> versionDirs = Files.list(artifactDir)) {
                    for (Path versionDir : versionDirs.filter(Files::isDirectory).toList()) {
                        if (!hasCanonicalJar(versionDir, artifactId)) continue;
                        long time = Files.getLastModifiedTime(versionDir).toMillis();
                        if (time > newestTime) {
                            newestTime = time;
                            newest = versionDir.getFileName().toString();
                        }
                    }
                } catch (IOException ignored) {
                }
            }
        } catch (IOException ignored) {
        }

        return newest;
    }

    private static boolean hasCanonicalJar(Path versionDir, String artifactId) {
        return Files.isRegularFile(versionDir.resolve(artifactId + "-" + versionDir.getFileName().toString() + ".jar"));
    }

    private static Path spigotRepository() {
        String home = System.getProperty("user.home");
        if (home == null || home.isBlank()) return null;
        return Path.of(home, ".m2", "repository", "org", "spigotmc");
    }
}
