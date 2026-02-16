package org.bsdevelopment.servermaster.utils;

import com.jeff_media.javafinder.JavaFinder;
import com.jeff_media.javafinder.JavaInstallation;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class JavaInstallationManager {
    @Getter private final List<JavaInstallation> installations;
    @Getter private final JavaInstallation primaryInstallation;
    @Getter private final JavaInstallation highestInstallation;
    @Getter @Setter private JavaInstallation selectedInstallation;
    @Getter private final List<String> jvmArguments;

    public JavaInstallationManager() {
        this.installations = JavaFinder.builder().build().findInstallations();
        this.primaryInstallation = findPrimaryInstallation(this.installations);
        this.highestInstallation = findHighestInstallation(this.installations).orElse(this.primaryInstallation);

        this.selectedInstallation = this.highestInstallation;

        this.jvmArguments = new ArrayList<>();
        this.jvmArguments.add("-Xms512M");
    }

    public void setJvmArguments(final String arguments) {
        this.jvmArguments.clear();
        if (arguments == null || arguments.isBlank()) return;

        this.jvmArguments.addAll(Arrays.asList(arguments.trim().split("\\s+")));
    }

    private static JavaInstallation findPrimaryInstallation(final List<JavaInstallation> installations) {
        if (installations == null || installations.isEmpty()) return null;

        for (JavaInstallation installation : installations) {
            if (installation != null && installation.isCurrentJavaVersion()) {
                return installation;
            }
        }

        return null;
    }

    private static Optional<JavaInstallation> findHighestInstallation(final List<JavaInstallation> installations) {
        if (installations == null || installations.isEmpty()) return Optional.empty();

        return installations.stream().filter(Objects::nonNull).filter(JavaInstallationManager::hasAnyVersionInfo)
                .max(Comparator
                        .comparingInt(JavaInstallationManager::majorVersionSafe)
                        .thenComparing(JavaInstallationManager::versionStringSafe));
    }

    private static boolean hasAnyVersionInfo(final JavaInstallation installation) {
        return majorVersionSafe(installation) > 0 || !versionStringSafe(installation).isEmpty();
    }

    private static int majorVersionSafe(final JavaInstallation installation) {
        try {
            return Math.max(0, installation.getVersion().getMajor());
        } catch (Throwable ignored) {
            return 0;
        }
    }

    private static String versionStringSafe(final JavaInstallation installation) {
        try {
            String v = installation.getVersion().getShortVersion();
            return v == null ? "" : v;
        } catch (Throwable ignored) {
            return "";
        }
    }
}
