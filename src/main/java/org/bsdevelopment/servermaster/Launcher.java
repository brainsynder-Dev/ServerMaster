package org.bsdevelopment.servermaster;

import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.Arrays;

public class Launcher {
    public static void main(String[] args) {
        if (!Constants.DEV_MODE) {
            var logFile = resolveLogPath();
            logFile.toFile().delete();

            try {
                Files.writeString(
                        logFile,
                        "START " + Instant.now() + System.lineSeparator()
                                + "cwd=" + Paths.get("").toAbsolutePath() + System.lineSeparator()
                                + "java=" + System.getProperty("java.version") + System.lineSeparator()
                                + "args=" + Arrays.toString(args) + System.lineSeparator(),
                        StandardOpenOption.CREATE,
                        StandardOpenOption.APPEND
                );

                var out = new PrintStream(Files.newOutputStream(
                        logFile,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.APPEND
                ), true);
                System.setOut(out);
                System.setErr(out);

                Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
                    e.printStackTrace();
                    try {
                        Files.writeString(
                                logFile,
                                "UNCAUGHT on " + t.getName() + " @ " + Instant.now() + System.lineSeparator(),
                                StandardOpenOption.CREATE,
                                StandardOpenOption.APPEND
                        );
                    } catch (Exception ignored) {
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        ServerMasterApp.main(args);
    }

    private static Path resolveLogPath() {
        boolean windows = System.getProperty("os.name", "").toLowerCase().contains("win");
        String home = System.getProperty("user.home");

        Path dir;
        if (windows) {
            String appData = System.getenv("APPDATA");
            dir = Path.of(appData != null ? appData : home).resolve("ServerMaster");
        } else {
            String xdgData = System.getenv("XDG_DATA_HOME");
            dir = Path.of(xdgData != null ? xdgData : home + "/.local/share").resolve("servermaster");
        }

        try {
            Files.createDirectories(dir);
        } catch (Exception ignored) {
            return Constants.WORKING_PATH.resolve("startup.log");
        }

        return dir.resolve("startup.log");
    }
}
