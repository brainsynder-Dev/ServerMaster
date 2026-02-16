package org.bsdevelopment.servermaster;

import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.Arrays;

public class Launcher {
    public static void main(String[] args) {
        var logFile = Constants.WORKING_PATH.resolve("startup.log");
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

        ServerMasterApp.main(args);
    }
}
