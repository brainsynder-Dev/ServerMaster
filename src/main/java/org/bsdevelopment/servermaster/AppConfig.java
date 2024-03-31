package org.bsdevelopment.servermaster;

import com.osiris.jlib.json.JsonFile;

import java.io.File;

public class AppConfig extends JsonFile {
    public static boolean devMode = false;
    public static boolean lightTheme = true;
    public static boolean auto_scroll = true;

    public static String serverPath = "";
    public static String javaPath = "";

    public static String serverType = "";
    public static String serverVersion = "";
    public static String serverBuild = "";

    public static int ram = 1024;
    public static int port = 25565;
    public static boolean auto_accept_eula = true;

    public AppConfig(File file) {
        super(file);
    }
}