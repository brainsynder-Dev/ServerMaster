package org.bsdevelopment.servermaster;

import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.theme.Theme;
import org.bsdevelopment.servermaster.server.ServerWrapper;
import org.bsdevelopment.servermaster.server.jar.ServerJarManager;
import org.bsdevelopment.servermaster.swing.LoadingWindow;
import org.bsdevelopment.servermaster.swing.Swing;
import org.bsdevelopment.servermaster.swing.Window;
import org.bsdevelopment.servermaster.swing.WindowUtils;
import org.bsdevelopment.servermaster.utils.AppUtilities;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.ConfigurableApplicationContext;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

/**
 * The entry point of the Spring Boot application.
 * <p>
 * Use the @PWA annotation make the application installable on phones, tablets
 * and some desktop browsers.
 */
@SpringBootApplication
@Push
@Theme(value = "mytodo")
@PWA(name = "Server Master", shortName = "servermaster", offlineResources = {})
@NpmPackage(value = "line-awesome", version = "1.3.0")
public class App extends SpringBootServletInitializer implements AppShellConfigurator {
    public static final String name = "Server Master";
    public static ConfigurableApplicationContext context;
    public static Window siteDisplay;
    public static int sitePort;

    public static final File workingDir = new File(".");
    public static final File tempDir = new File(System.getProperty("java.io.tmpdir") + "/" + name);
    public static final File userDir = new File(System.getProperty("user.home") + "/" + name);
    public static final File LOG_FILE = new File(workingDir + "/latest.log");

    private static AppConfig CONFIG;
    private static ServerJarManager jarManager;
    private static int MAX_RAM;

    public static void main(String[] args) throws IOException {
        if (!LOG_FILE.exists()) {
            try {
                LOG_FILE.createNewFile();
            }catch (Exception e) {
                AppUtilities.delayedLogMessage("[ERROR]: Failed to create 'latest.log' in the "+LOG_FILE.getParentFile().getName()+" folder (Missing WRITE Permissions)");
            }
        }

        CONFIG = new AppConfig(new File(App.workingDir, "config.json"));
        try {
            CONFIG.load();
        } catch (IOException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        MAX_RAM = (int) (Runtime.getRuntime().freeMemory() / 125000);
        jarManager = new ServerJarManager(new File(AppConfig.serverPath));

        WindowUtils.LOADING_WINDOW.setVisible(true);
        WindowUtils.LOADING_WINDOW.setAlwaysOnTop(true);
        SpringApplication springApp = new SpringApplication(App.class);

        Properties props = new Properties();
        sitePort = new ServerSocket(0).getLocalPort(); // get random free port
        props.put("server.port", sitePort);
        props.put("security.require-ssl", "false");
        springApp.setDefaultProperties(props);

        context = springApp.run(args);
        System.setProperty("java.awt.headless", "false");
        siteDisplay = new Window(WindowUtils.LOADING_WINDOW);
        siteDisplay.setVisible(true);
        siteDisplay.setSize(5,50);
        Swing.center(siteDisplay);

        new ServerWrapper(jarManager);
    }

    public static void showApplication () {
        siteDisplay.setState(Frame.NORMAL);

        siteDisplay.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        Dimension dimension = new Dimension(1200, 745);

        siteDisplay.setSize(dimension);
        siteDisplay.setExtendedState(siteDisplay.getExtendedState() | JFrame.MAXIMIZED_BOTH);
        siteDisplay.setMinimumSize(dimension);

        WindowUtils.WINDOW_LIST.forEach(LoadingWindow::close);
    }

    public static Image getIcon() throws IOException {
        return getResourceImage("/images/icon.png");
    }

    /**
     * @param path expected to be child of /META-INF/resources. Example: icon.png or /icon.png
     */
    public static Image getResourceImage(String path) throws IOException {
        File img = new File(App.workingDir + path);
        if (!img.exists()) {
            img.getParentFile().mkdirs();
            img.createNewFile();
            InputStream link = (App.class.getResourceAsStream(path));
            Files.copy(link, img.toPath(), StandardCopyOption.REPLACE_EXISTING);
            link.close();
        }
        return Toolkit.getDefaultToolkit().getImage(img.getAbsolutePath());
    }

    public static void saveConfig () {
        try {
            CONFIG.saveNow();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static ServerJarManager getJarManager() {
        return jarManager;
    }
}
