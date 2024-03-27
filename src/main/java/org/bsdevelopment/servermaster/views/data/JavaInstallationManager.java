package org.bsdevelopment.servermaster.views.data;

import com.jeff_media.javafinder.JavaFinder;
import com.jeff_media.javafinder.JavaInstallation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class JavaInstallationManager {

    private final List<JavaInstallation> installations;
    private final JavaInstallation primaryInstallation;
    private JavaInstallation selectedInstallation;
    private final List<String> jvmArguments;

    public JavaInstallationManager() {
        this.installations = JavaFinder.builder().build().findInstallations();
        this.primaryInstallation = fetchPrimaryInstallation(this.installations);
        this.selectedInstallation = this.primaryInstallation;
        this.jvmArguments = new ArrayList<>();
        this.jvmArguments.add("-Xms512M"); // absolute minimum ram required
    }

    public JavaInstallation getPrimaryInstallation() {
        return this.primaryInstallation;
    }

    public JavaInstallation getSelectedInstallation() {
        return this.selectedInstallation;
    }

    public List<JavaInstallation> getInstallations() {
        return this.installations;
    }

    public List<String> getJvmArguments() {
        return this.jvmArguments;
    }

    public void setSelectedInstallation(JavaInstallation javaInstallation) {
        this.selectedInstallation = javaInstallation;
    }

    public void setJvmArguments(final String arguments) {
        this.jvmArguments.clear();
        jvmArguments.addAll(Arrays.asList(arguments.split(" ")));
    }

    private static JavaInstallation fetchPrimaryInstallation(final List<JavaInstallation> installations) {
        for (JavaInstallation installation : installations) {
            if (installation.isCurrentJavaVersion()) {
                return installation;
            }
        }

        return null;
    }
}