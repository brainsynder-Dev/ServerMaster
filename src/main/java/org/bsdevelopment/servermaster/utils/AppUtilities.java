package org.bsdevelopment.servermaster.utils;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.dom.ThemeList;
import com.vaadin.flow.theme.lumo.Lumo;
import org.bsdevelopment.servermaster.App;
import org.bsdevelopment.servermaster.AppConfig;
import org.bsdevelopment.servermaster.server.utils.Version;
import org.bsdevelopment.servermaster.utils.records.UpdateInfo;

import java.awt.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;

public class AppUtilities {
    public static boolean CONSOLE_READY = false;
    public static boolean OFFLINE = false;
    private static final LinkedList<String> DELAYED_MESSAGES = new LinkedList<>();

    public static void logMessage (String message) {
        logMessage(null, message);
    }
    public static void logMessage (Level level, String message) {
        String text = ((level == null) ? "" : "["+level.getName()+"]: ")+message;

        System.out.println(text);
        appendToLogFile(text);
    }

    public static void logMessage(String prefix, String... messages) {
        delayedLogMessage("");
        String typePrefix = "@INFO";
        if (prefix.startsWith("[WARNING]")) typePrefix = "@WARN";
        if (prefix.startsWith("[ERROR]")) typePrefix = "@ERROR";
        if (prefix.startsWith("[COMMAND]")) typePrefix = "@COMMAND";

        delayedLogMessage(typePrefix+"@U1@U2 " + prefix);
        delayedLogMessage(typePrefix+"@U3");

        String spacer = typePrefix+"@U3";

        for (String message : messages) {
            delayedLogMessage(spacer + message);
        }

        delayedLogMessage(typePrefix+"@U3");
        delayedLogMessage(typePrefix+"@U4@U5");
        delayedLogMessage("");
    }

    public static void logMessage(LogPrefix prefix, String... messages) {
        logMessage(prefix, "", Arrays.asList(messages));
    }

    public static void logMessage(LogPrefix prefix, String title, List<String> messages) {
        delayedLogMessage("");
        String typePrefix = "@INFO";
        if (prefix == LogPrefix.WARNING) typePrefix = "@WARN";
        if (prefix == LogPrefix.ERROR) typePrefix = "@ERROR";
        if (prefix == LogPrefix.COMMAND) typePrefix = "@COMMAND";

        delayedLogMessage(typePrefix+"@U1@U2 [" + prefix.name()+"] "+title);
        delayedLogMessage(typePrefix+"@U3");

        String spacer = typePrefix+"@U3";

        for (String message : messages) {
            delayedLogMessage(spacer + message);
        }

        delayedLogMessage(typePrefix+"@U3");
        delayedLogMessage(typePrefix+"@U4@U5");
        delayedLogMessage("");
    }
    public static void delayedLogMessage (String message) {
        if (CONSOLE_READY) {
            System.out.println(message);
            return;
        }
        DELAYED_MESSAGES.addLast(message);
    }

    public static UpdateInfo fetchUpdateInfo () {
        try {
            String result = sendGetRequest("https://api.github.com/repos/brainsynder-Dev/ServerMaster/releases", false);
            if (result.isEmpty()) {
                OFFLINE = true;
                return null;
            }
            JsonArray releaseArray = Json.parse(result).asArray();
            JsonObject update = releaseArray.get(0).asObject();

            Version version = Version.parse(update.get("tag_name").asString().replaceFirst("v", ""));
            String releaseUrl = update.get("html_url").asString();
            String title = update.get("name").asString();
            String markdown = update.get("body").asString();

            return new UpdateInfo (releaseUrl, title, version, markdown);
        }catch (Exception e) {
            delayedLogMessage("Error fetching Update Info: "+e.getMessage());
        }
        return null;
    }

    public static String sendGetRequest (String rawUrl, boolean allowRedirects) {
        try {
            URL url = new URL(rawUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setInstanceFollowRedirects(allowRedirects);
            connection.setRequestMethod("GET");
            connection.setUseCaches(false);
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);

            try (InputStream inputStream = connection.getInputStream()) {
                return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            return "";
        }
    }

    private static void appendToLogFile(String content)  {
        try (FileWriter fw = new FileWriter(App.LOG_FILE, true); BufferedWriter bw = new BufferedWriter(fw)) {
            bw.write(content);
            bw.newLine();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String toHex(Color color) {
        return "#" + toBrowserHexValue(color.getRed()) + toBrowserHexValue(color.getGreen()) + toBrowserHexValue(color.getBlue());
    }
    public static String toHex(int r, int g, int b) {
        return "#" + toBrowserHexValue(r) + toBrowserHexValue(g) + toBrowserHexValue(b);
    }

    private static String toBrowserHexValue(int number) {
        StringBuilder builder = new StringBuilder(Integer.toHexString(number & 0xff));
        while (builder.length() < 2) {
            builder.append("0");
        }
        return builder.toString().toUpperCase();
    }

    public static Color hex2Color(String hex) {
        return new Color(
                Integer.valueOf(hex.substring(1, 3), 16),
                Integer.valueOf(hex.substring(3, 5), 16),
                Integer.valueOf(hex.substring(5, 7), 16)
        );
    }

    public static LinkedList<String> getDelayedMessages() {
        return DELAYED_MESSAGES;
    }

    public static void downloadFile(URLConnection httpConnection, File file, Consumer<Integer> progressConsumer, Runnable finishTask) throws IOException {
        new Thread(() -> {
            if (file.exists()) file.delete();
            try {
                file.createNewFile();
            } catch (IOException e) {
                AppUtilities.logMessage(AppUtilities.LogPrefix.ERROR, "Installer Issue", Arrays.asList(
                        "An error has occurred when trying to create the '"+file.getName()+"' file",
                        "Error: ",
                        e.getMessage()
                ));
            }

            OutputStream outputStream = null;
            InputStream inputStream = null;
            try {
                outputStream = new BufferedOutputStream(new FileOutputStream(file));
                inputStream = httpConnection.getInputStream();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            byte[] buffer = new byte[1024];

            long completeFileSize = httpConnection.getContentLength();
            long downloadedFileSize = 0;

            int dataLength;
            try {
                while ((dataLength = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, dataLength);

                    downloadedFileSize += dataLength;
                    int currentProgress = (int) (downloadedFileSize * 100 / completeFileSize);
                    progressConsumer.accept(currentProgress);
                }

                inputStream.close();
                outputStream.close();

                finishTask.run();
            }catch (Exception e) {
                AppUtilities.logMessage(AppUtilities.LogPrefix.ERROR, "Installer Issue", Arrays.asList(
                        "An error has occurred when trying to install the '"+file.getName()+"' file",
                        "Error: ",
                        e.getMessage()
                ));
            }
        }).start();
    }


    public static Component createInfoHeader(String display, String tooltip) {
        Span span = new Span(display);
        Icon icon = VaadinIcon.INFO_CIRCLE.create();
        icon.getElement().setAttribute("title", tooltip);
        icon.getStyle().set("height", "var(--lumo-font-size-m)").set("color", "var(--lumo-contrast-70pct)");

        HorizontalLayout layout = new HorizontalLayout(span, icon);
        layout.setAlignItems(FlexComponent.Alignment.CENTER);
        layout.setSpacing(false);
        return layout;
    }

    public static Component modifyComponent(Component component, Consumer<Component> consumer) {
        consumer.accept(component);
        return component;
    }

    public static Span unicodeSpan (String className, String color) {
        Span span = new Span ();
        span.addClassName (className);
        if (color != null) span.getStyle().set("color", color);
        return span;
    }

    public static void sendNotification (String text, Notification.Position position, NotificationVariant variant) {
        Notification notification = new Notification(text, 5000);
        if (position != null) notification.setPosition(position);
        if (variant != null) notification.addThemeVariants(variant);
        notification.open();
    }

    public static void sendNotification (String text, Notification.Position position) {
        sendNotification(text, position, null);
    }

    public static void sendNotification (String text, NotificationVariant variant) {
        sendNotification(text, null, variant);
    }

    public enum LogPrefix {
        INFO,
        WARNING,
        ERROR,
        COMMAND
    }

    public static void updateTheme () {
        UI ui = UI.getCurrent();
        if (ui == null) return;

        ThemeList themeList = ui.getElement().getThemeList();
        themeList.clear();
        if (!AppConfig.lightTheme) themeList.add(Lumo.DARK);
    }
}
