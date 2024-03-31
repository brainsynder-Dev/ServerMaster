package org.bsdevelopment.servermaster.views;

import com.vaadin.flow.component.html.Span;
import org.bsdevelopment.servermaster.views.data.JavaInstallationManager;
import org.bsdevelopment.servermaster.views.serverconsole.AppSettingsDialog;
import org.bsdevelopment.servermaster.views.serverconsole.InstallerDialog;
import org.bsdevelopment.servermaster.views.serverconsole.JavaVersionDialog;

public class ViewHandler {

    public static AppSettingsDialog APP_SETTINGS;
    public static InstallerDialog INSTALLER;
    public static JavaVersionDialog JAVA_VERSION;

    public static Span DEV_MODE = new Span();

    public static final JavaInstallationManager JAVA_MANAGER = new JavaInstallationManager();
}
