package org.bsdevelopment.servermaster.utils;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import org.bsdevelopment.servermaster.App;

import java.awt.*;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.LinkedList;
import java.util.logging.Level;

public class AppUtilities {
    private static final LinkedList<String> DELAYED_MESSAGES = new LinkedList<>();

    public static void logMessage (String message) {
        logMessage(null, message);
    }
    public static void logMessage (Level level, String message) {
        String text = ((level == null) ? "" : "["+level.getName()+"]: ")+message;

        System.out.println(text);
        appendToUsernameFile(text);
    }
    public static void delayedLogMessage (String message) {
        DELAYED_MESSAGES.addLast(message);
    }

    private static void appendToUsernameFile(String content)  {
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

    public static LinkedList<String> getDelayedMessages() {
        return DELAYED_MESSAGES;
    }

    public static Color hex2Color(String hex) {
        return new Color(
                Integer.valueOf(hex.substring(1, 3), 16),
                Integer.valueOf(hex.substring(3, 5), 16),
                Integer.valueOf(hex.substring(5, 7), 16)
        );
    }

    public static void downloadFile(String fromUrl, File file) throws IOException {
        if (file.exists()) file.delete();
        file.createNewFile();

        URL url = new URL(fromUrl);
        OutputStream out = new BufferedOutputStream(new FileOutputStream(file));
        URLConnection conn = url.openConnection();
        InputStream in = conn.getInputStream();
        byte[] buffer = new byte[1024];

        int numRead;
        while ((numRead = in.read(buffer)) != -1) {
            out.write(buffer, 0, numRead);
        }
        in.close();
        out.close();
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
}
