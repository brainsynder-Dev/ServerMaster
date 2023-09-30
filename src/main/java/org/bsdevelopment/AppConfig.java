package org.bsdevelopment;

import com.formdev.flatlaf.FlatLaf;
import org.bsdevelopment.utils.Theme;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class AppConfig {
    private static File propertiesFile;
    private static Properties properties;

    public static int ram = 1024;
    public static int port = 25565;
    public static Theme theme = Theme.LIGHT;
    public static boolean acceptEULA = false;
    public static String serverPath = "";

    private static boolean appSpecificMessage = false;

    public static void init() {
        propertiesFile = new File(new File("."), "server-master.properties");
        properties = new Properties();

        try {
            if (propertiesFile.createNewFile()) {
                properties.setProperty("server-path", "");
                properties.setProperty("server-ram", "1024");
                properties.setProperty("server-port", "25565");
                properties.setProperty("theme", "Dracula");
                properties.setProperty("eula", "false");

                saveData();
            } else {
                reloadData();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void reloadData() {
        try {
            properties.load(new FileInputStream(propertiesFile));
        } catch (IOException ignored) {
        }

        serverPath = properties.getProperty("server-path", "");

        ram = Integer.parseInt(properties.getProperty("server-ram", "1024"));
        port = Integer.parseInt(properties.getProperty("server-port", "25565"));
        acceptEULA = Boolean.parseBoolean(properties.getProperty("eula", "false"));

        theme = Theme.valueOf(properties.getProperty("theme", "LIGHT"));
        theme.apply();
        FlatLaf.updateUI();
    }

    public static void saveData() {
        properties.setProperty("server-path", serverPath);
        properties.setProperty("server-ram", String.valueOf(ram));
        properties.setProperty("server-port", String.valueOf(port));
        properties.setProperty("theme", theme.name());
        properties.setProperty("eula", String.valueOf(acceptEULA));

        try {
            properties.store(new FileOutputStream(propertiesFile), null);

            reloadData();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static File getPropertiesFile() {
        return propertiesFile;
    }

    public static Properties getProperties() {
        return properties;
    }

    public static boolean isAppSpecificMessage() {
        return appSpecificMessage;
    }

    public static void sendAppMessage(Runnable runnable) {
        appSpecificMessage = true;
        runnable.run();
        appSpecificMessage = false;
    }
}
