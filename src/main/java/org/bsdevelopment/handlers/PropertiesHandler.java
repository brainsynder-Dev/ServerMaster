package org.bsdevelopment.handlers;

import org.bsdevelopment.Bootstrap;
import org.bsdevelopment.utils.DelayedTextChangedListener;
import org.bsdevelopment.utils.Utils;

import javax.swing.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class PropertiesHandler {
    private static Bootstrap.PropertiesPanel propertiesPanel;
    private static Properties properties;
    private static File file;

    public static void handle(Bootstrap.PropertiesPanel propertiesPanel) throws IOException {
        PropertiesHandler.propertiesPanel = propertiesPanel;
        properties = new Properties();
        file = new File(Bootstrap.INSTANCE.SERVER_FOLDER, "server.properties");


        if (!file.createNewFile()) {
            try {
                properties.load(new FileInputStream(file));
            } catch (IOException ignored) {
            }
        }

        handleToggle(propertiesPanel.cmdBlocksField, "enable-command-block", false);
        handleToggle(propertiesPanel.queryField, "enable-query", false);
        handleToggle(propertiesPanel.secureprofile, "enforce-secure-profile", true);
        handleToggle(propertiesPanel.pvpField, "pvp", true);
        handleToggle(propertiesPanel.structureField, "generate-structures", true);
        handleToggle(propertiesPanel.resourcePackToggle, "require-resource-pack", false);
        handleToggle(propertiesPanel.useNativeTransport, "use-native-transport", true);
        handleToggle(propertiesPanel.onlineMode, "online-mode", true);
        handleToggle(propertiesPanel.toggleStatus, "enable-status", true);
        handleToggle(propertiesPanel.toggleFlight, "allow-flight", false);
        handleToggle(propertiesPanel.broadcastRcon, "broadcast-rcon-to-ops", false);
        handleToggle(propertiesPanel.toggleNether, "allow-nether", true);
        handleToggle(propertiesPanel.toggleRcon, "enable-rcon", false);
        handleToggle(propertiesPanel.syncChunks, "sync-chunk-writes", true);
        handleToggle(propertiesPanel.preventProxies, "prevent-proxy-connections", false);
        handleToggle(propertiesPanel.hideOnlinePlayers, "hide-online-players", false);
        handleToggle(propertiesPanel.forceGamemode, "force-gamemode", false);
        handleToggle(propertiesPanel.hardcore, "hardcore", false);//
        handleToggle(propertiesPanel.whitelist, "white-list", false);
        handleToggle(propertiesPanel.broadcastConsoleMessages, "broadcast-console-to-ops", true);
        handleToggle(propertiesPanel.spawnNpc, "spawn-npcs", true);
        handleToggle(propertiesPanel.spawnAnimals, "spawn-animals", true);
        handleToggle(propertiesPanel.spawnMonsters, "spawn-monsters", true);
        handleToggle(propertiesPanel.logIps, "log-ips", true);
        handleToggle(propertiesPanel.enforceWhitelist, "enforce-whitelist", false);
    }

    private static void handleToggle(JToggleButton button, String key, boolean defaultValue) {
        Utils.handleToggleButton(button,
                Boolean.parseBoolean(properties.getProperty(key, String.valueOf(defaultValue))),
                value -> {
                    properties.setProperty(key, String.valueOf(value));
                    try {
                        properties.store(new FileOutputStream(file), null);
                        handle(propertiesPanel);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
        );
    }

    private void handleTextChange(JTextField field, String key, String defaultValue) {
        DelayedTextChangedListener listener = new DelayedTextChangedListener(3000);
        listener.addChangeListener(e -> {
            String rawText = field.getText().trim();
            if (rawText.isBlank() && (defaultValue.isBlank() || defaultValue.isEmpty())) {
                field.setText(defaultValue);
                properties.setProperty(key, defaultValue);
                try {
                    properties.store(new FileOutputStream(file), null);
                    handle(propertiesPanel);
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
                return;
            }

            properties.setProperty(key, rawText);
            try {
                properties.store(new FileOutputStream(file), null);
                handle(propertiesPanel);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });

        field.getDocument().addDocumentListener(listener);
    }
}
