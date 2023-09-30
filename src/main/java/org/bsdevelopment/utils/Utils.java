package org.bsdevelopment.utils;

import com.google.common.io.ByteStreams;
import org.bsdevelopment.Bootstrap;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class Utils {
    public static final ImageIcon CHECKED = Utils.getIcon("checked.png");
    public static final ImageIcon NOT_CHECKED = Utils.getIcon("not-checked.png");

    public static void numericalCharacterCheck (JTextComponent component) {
        component.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent keyEvent) {
                if ((keyEvent.getKeyCode() == 8) || (keyEvent.getKeyCode() == 46)) return;
                if (((keyEvent.getKeyCode() >= 96) && (keyEvent.getKeyCode() <= 105)) ||
                        (keyEvent.getKeyCode() >= 48) && (keyEvent.getKeyCode() <= 57)) {
                    component.setEditable(true);
                } else {
                    component.setEditable(false);
                    JOptionPane.showMessageDialog(null,
                            "Error: Enter only numeric digits (0-9)", "Error Message",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        });
    }

    public static void handleToggleButton(JToggleButton button, boolean value, Consumer<Boolean> consumer) {
        button.setSelected(value);
        if (value) {
            button.setIcon(CHECKED);
        } else {
            button.setIcon(NOT_CHECKED);
        }

        button.addItemListener(e -> {
            if (button.isSelected()) {
                button.setIcon(CHECKED);
            }else{
                button.setIcon(NOT_CHECKED);
            }

            consumer.accept(button.isSelected());
        });
    }

    public static String getReadableStacktrace(Throwable throwable) {
        final StringBuilder builder = new StringBuilder();
        builder.append(throwable.getMessage());
        builder.append("\n");
        for (StackTraceElement stackTraceElement : throwable.getStackTrace()) {
            builder.append(stackTraceElement.toString());
            builder.append("\n");
        }

        return builder.toString();
    }

    public Set<Class<?>> findAllClassesUsingClassLoader(String packageName) {
        InputStream stream = ClassLoader.getSystemClassLoader()
                .getResourceAsStream(packageName.replaceAll("[.]", "/"));
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        return reader.lines()
                .filter(line -> line.endsWith(".class"))
                .map(line -> getClass(line, packageName))
                .collect(Collectors.toSet());
    }

    private Class<?> getClass(String className, String packageName) {
        try {
            return Class.forName(packageName + "."
                    + className.substring(0, className.lastIndexOf('.')));
        } catch (ClassNotFoundException e) {
            // handle the exception
        }
        return null;
    }

    public static ImageIcon getIcon(String fileName) {
        try (InputStream input = Bootstrap.class.getClassLoader().getResourceAsStream(fileName)) {
            return new ImageIcon(ByteStreams.toByteArray(input));
        } catch (IOException ignored) {
        }
        return null;
    }
}
