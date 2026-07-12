package org.bsdevelopment.servermaster.instance;

import org.bsdevelopment.servermaster.LogViewer;
import org.bsdevelopment.servermaster.instance.server.utils.Version;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.Properties;
import java.util.stream.Stream;

public final class WorldInstanceManager {
    public static final String WORLD_INFO_FILE = "world.info";
    private static final Version SPLIT_VERSION = Version.parse("26.1");
    private static final String[] DIMENSION_FOLDERS = {"world", "world_nether", "world_the_end"};

    /**
     * Returns the world_instances sub-folder name for the given server type + version.
     * <ul>
     *   <li>Below 26.1 : {@code World_{version}}               (format is shared)</li>
     *   <li>26.1+       : {@code World_{serverType}_{version}} (formats diverge)</li>
     * </ul>
     */
    public static String instanceFolderName(String serverType, String version) {
        Version v = Version.parse(version);
        if (v.compareTo(SPLIT_VERSION) >= 0) {
            return "World_" + serverType + "_" + version;
        }
        return "World_" + version;
    }

    /**
     * Swaps the active world dimensions in {@code serverRoot} for the saved instance
     * that matches {@code serverType}/{@code version}.
     *
     * <p>Instance layout inside {@code world_instances/{name}/}:
     * <pre>
     *   world/          ← overworld (contains world.info)
     *   world_nether/   ← nether   (present only if it was saved)
     *   world_the_end/  ← end      (present only if it was saved)
     * </pre>
     *
     * <ol>
     *   <li>Reads {@code serverRoot/world/world.info} to find the current instance name,
     *       then moves all existing dimension folders into
     *       {@code world_instances/{currentName}/}.</li>
     *   <li>If {@code world_instances/{newName}/} exists, moves its dimension folders
     *       back into {@code serverRoot}.</li>
     *   <li>Otherwise, creates a fresh {@code serverRoot/world/} with a {@code world.info}
     *       so the server can generate a new world on first start.</li>
     * </ol>
     *
     * <p>The nether is controlled by {@code allow-nether=false} in server.properties
     * (set separately in Server). This method still saves/restores {@code world_nether}
     * so that a world retains its nether data if the setting is ever changed.
     */
    public static void swapWorld(Path serverRoot, String serverType, String version) throws IOException {
        Path worldInstancesDir = serverRoot.resolve("world_instances");
        Path activeWorld = serverRoot.resolve("world");

        // --- 1. Save current world ---
        if (Files.isDirectory(activeWorld)) {
            Path infoFile = activeWorld.resolve(WORLD_INFO_FILE);
            if (Files.exists(infoFile)) {
                String currentInstanceName = readProperty(infoFile, "instance-name");
                if (currentInstanceName != null && !currentInstanceName.isBlank()) {
                    Path instanceDir = worldInstancesDir.resolve(currentInstanceName);
                    Files.createDirectories(instanceDir);
                    LogViewer.system("Saving world → world_instances/" + currentInstanceName);
                    for (String dim : DIMENSION_FOLDERS) {
                        Path src = serverRoot.resolve(dim);
                        if (Files.isDirectory(src)) {
                            moveDirectory(src, instanceDir.resolve(dim));
                        }
                    }
                } else {
                    LogViewer.system("world.info missing instance-name — skipping world save.");
                }
            } else {
                LogViewer.system("No world.info in active world folder — skipping world save.");
            }
        }

        // --- 2. Restore or create new world ---
        String newInstanceName = instanceFolderName(serverType, version);
        Path newInstanceDir = worldInstancesDir.resolve(newInstanceName);

        if (Files.isDirectory(newInstanceDir)) {
            LogViewer.system("Restoring world ← world_instances/" + newInstanceName);
            for (String dim : DIMENSION_FOLDERS) {
                Path src = newInstanceDir.resolve(dim);
                if (Files.isDirectory(src)) {
                    moveDirectory(src, serverRoot.resolve(dim));
                }
            }
            // Remove the now-empty instance wrapper.
            deleteDirectoryIfEmpty(newInstanceDir);
        } else {
            // No saved world for this type/version — create an empty overworld folder
            // so the server can generate a fresh world.
            LogViewer.system("No saved world for " + newInstanceName + " — fresh world will be generated.");
            Files.createDirectories(activeWorld);
            writeWorldInfo(activeWorld, serverType, version, newInstanceName);
        }
    }

    /**
     * Permanently deletes the saved world for {@code serverType}/{@code version} so the
     * server generates a fresh one on its next start. Removes the matching
     * {@code world_instances/{name}} folder, and — if that instance is the one currently
     * active in {@code serverRoot} — also clears the live {@code world*} dimension folders.
     */
    public static void regenerateWorld(Path serverRoot, String serverType, String version) throws IOException {
        String targetName = instanceFolderName(serverType, version);

        Path savedInstance = serverRoot.resolve("world_instances").resolve(targetName);
        if (Files.isDirectory(savedInstance)) {
            deleteDirectory(savedInstance);
            LogViewer.system("Deleted saved world → world_instances/" + targetName);
        }

        Path activeWorld = serverRoot.resolve("world");
        Path infoFile = activeWorld.resolve(WORLD_INFO_FILE);
        if (Files.exists(infoFile) && targetName.equals(readProperty(infoFile, "instance-name"))) {
            for (String dim : DIMENSION_FOLDERS) {
                Path dir = serverRoot.resolve(dim);
                if (Files.isDirectory(dir)) deleteDirectory(dir);
            }
            LogViewer.system("Cleared the active world for " + targetName + ".");
        }

        LogViewer.system("A fresh world for " + serverType + " " + version + " will be generated on next start.");
    }

    /**
     * Writes (or overwrites) the {@code world.info} file inside {@code worldDir}.
     * Called automatically by {@link #swapWorld} when creating a fresh world folder.
     * Exposed publicly so callers can stamp an existing folder that lacks world.info.
     */
    public static void writeWorldInfo(Path worldDir, String serverType, String version, String instanceName) throws IOException {
        Path infoFile = worldDir.resolve(WORLD_INFO_FILE);
        try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(infoFile))) {
            pw.println("server-type=" + serverType);
            pw.println("server-version=" + version);
            pw.println("instance-name=" + instanceName);
        }
    }

    private static String readProperty(Path infoFile, String key) throws IOException {
        Properties props = new Properties();
        try (var reader = Files.newBufferedReader(infoFile)) {
            props.load(reader);
        }
        return props.getProperty(key);
    }

    private static void moveDirectory(Path source, Path target) throws IOException {
        if (Files.exists(target)) {
            deleteDirectory(target);
        }

        // Fast path: same-filesystem rename (instant, no data copying).
        try {
            Files.move(source, target);
            return;
        } catch (IOException ignored) {
            // Cross-filesystem — fall through to copy+delete.
        }

        // Slow path: copy tree then remove source.
        try (Stream<Path> walk = Files.walk(source)) {
            for (Path src : walk.toList()) {
                Path dst = target.resolve(source.relativize(src));
                if (Files.isDirectory(src)) {
                    Files.createDirectories(dst);
                } else {
                    Files.copy(src, dst, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
                }
            }
        }
        deleteDirectory(source);
    }

    private static void deleteDirectory(Path dir) throws IOException {
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.delete(p);
                } catch (IOException ignored) {}
            });
        }
    }

    private static void deleteDirectoryIfEmpty(Path dir) {
        try (Stream<Path> s = Files.list(dir)) {
            if (s.findAny().isEmpty()) Files.delete(dir);
        } catch (IOException ignored) {}
    }
}
