package org.bsdevelopment;

import com.formdev.flatlaf.FlatLaf;
import org.bsdevelopment.handlers.PropertiesHandler;
import org.bsdevelopment.server.API;
import org.bsdevelopment.server.ServerJarManager;
import org.bsdevelopment.server.ServerState;
import org.bsdevelopment.server.ServerWrapper;
import org.bsdevelopment.text.MessageConsole;
import org.bsdevelopment.utils.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Bootstrap extends JFrame {
    public static File LOG_FILE;
    private JPopupMenu context;
    private final ServerJarManager jarManager;

    public static Bootstrap INSTANCE;

    private static int MAX_RAM;
    public static final ImageIcon LOGO = Utils.getIcon("icon.png");
    public Runnable stoppedServerRunnable = () -> {};

    public File SERVER_FOLDER;
    private final MessageConsole console;
    private final String placeholderVersion = "Select Server Version...";
    private final String placeholderType = "Select Server Type...";


    public static void main(String[] args) {
        MAX_RAM = (int) (Runtime.getRuntime().freeMemory() / 125000);

        Bootstrap bootstrap = new Bootstrap();
        bootstrap.setLocationRelativeTo(null);
        bootstrap.setVisible(true);
    }

    public Bootstrap() {
        INSTANCE = this;
        AppConfig.init();

        SERVER_FOLDER = new File(AppConfig.serverPath.isBlank() ? "." : AppConfig.serverPath);
        LOG_FILE = new File(new File("."), "server-master.log");
        try {
            LOG_FILE.createNewFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        jarManager = new ServerJarManager(SERVER_FOLDER);
        new ServerWrapper(jarManager);

        initComponents();

        console = new MessageConsole(serverPanel.consolePane.consoleOutput, true);
        console.setMessageLines(20000);
        console.redirectErr(Color.RED, null);
        console.redirectOut(null, null);

        updateComponents();
        loadServerData();
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        Dimension dimension = new Dimension(1200, 745);

        setSize(dimension);
        setMinimumSize(dimension);
        setTitle("Server Master");
        setIconImage(LOGO.getImage());

        setContentPane(tabbedPane1);

        AppConfig.sendAppMessage(() -> {
            System.out.println(" Finished loading all Server Jars...");
            System.out.println(" Found a total of " + jarManager.getTotalServerJars() + " different types/versions to pick from...");
            System.out.println();
            System.out.println("  *** Type '??' to get some help ***");
            System.out.println();
        });
    }

    private boolean toggleServerState (ServerState state) {
        switch (state) {
            case TYPE_SELECTION -> {
                serverPanel.startServer.setEnabled(false);
                serverPanel.forceStop.setEnabled(false);
                serverPanel.stopServer.setEnabled(false);
                serverPanel.serverCommand.setEnabled(false);

                serverPanel.serverVersionSelection.removeAllItems();

                if (placeholderType.equalsIgnoreCase(String.valueOf(serverPanel.serverTypeSelection.getSelectedItem()))) {
                    serverPanel.serverVersionSelection.setEnabled(false);
                    return false;
                }else{
                    serverPanel.serverVersionSelection.setEnabled(true);

                    serverPanel.serverVersionSelection.addItem(placeholderVersion);
                    serverPanel.serverVersionSelection.setSelectedItem(placeholderVersion);

                    jarManager.sortVersions(jarManager.getSupportedJars().get(String.valueOf(serverPanel.serverTypeSelection.getSelectedItem()))).forEach(version -> {
                        serverPanel.serverVersionSelection.addItem(version);
                    });
                    return true;
                }
            }
            case VERSION_SELECTION -> {
                if (placeholderVersion.equalsIgnoreCase(String.valueOf(serverPanel.serverVersionSelection.getSelectedItem()))) {
                    serverPanel.startServer.setEnabled(false);
                    return false;
                }else{
                    serverPanel.startServer.setEnabled(true);
                    return true;
                }
            }
            case STARTUP -> {
                serverPanel.serverVersionSelection.setEnabled(false);
                serverPanel.serverTypeSelection.setEnabled(false);
                serverPanel.startServer.setEnabled(false);

                tabbedPane1.setEnabledAt(1, false);
                tabbedPane1.setEnabledAt(2, false);

                serverPanel.serverCommand.setEnabled(true);
                serverPanel.forceStop.setEnabled(true);
                serverPanel.stopServer.setEnabled(true);


                if (ServerWrapper.getInstance().getServer() != null) return false;

                serverPanel.consolePane.getConsoleArea().setText("");

                int port = API.startServer(
                        String.valueOf(serverPanel.serverTypeSelection.getSelectedItem()),
                        String.valueOf(serverPanel.serverVersionSelection.getSelectedItem()), (server, statusCode) -> {
                    AppConfig.sendAppMessage(() -> {
                        toggleServerState(ServerState.STOPPED);
                        if (statusCode == 0) System.out.println("[ServerMaster] Stopped the server");
                        if (statusCode == 1)
                            System.out.println("[ServerMaster] The server process was forced to stop");
                    });
                });

                AppConfig.sendAppMessage(() -> {
                    System.out.println();
                    System.out.println("[ServerMaster] Starting server on localhost:" + port);
                    System.out.println();
                });
            }
            case STOPPED -> {
                serverPanel.serverVersionSelection.setEnabled(true);
                serverPanel.serverTypeSelection.setEnabled(true);
                tabbedPane1.setEnabledAt(1, true);
                tabbedPane1.setEnabledAt(2, true);
                serverPanel.startServer.setEnabled(true);

                serverPanel.forceStop.setEnabled(false);
                serverPanel.stopServer.setEnabled(false);
                serverPanel.serverCommand.setEnabled(false);
            }
        }
        return false;
    }

    private void loadServerData() {
        serverPanel.startServer.addActionListener(e -> {
            toggleServerState(ServerState.STARTUP);
        });

        serverPanel.stopServer.addActionListener(e -> {
            API.stopServer();
        });
        serverPanel.forceStop.addActionListener(e -> {
            API.killServer();
            toggleServerState(ServerState.STOPPED);
        });

        {
            serverPanel.serverTypeSelection.addItem(placeholderType);
            serverPanel.serverTypeSelection.setSelectedItem(placeholderType);

            jarManager.getSupportedJars().keySet().forEach(serverType -> {
                serverPanel.serverTypeSelection.addItem(serverType);
            });

            serverPanel.serverTypeSelection.addItemListener(e -> {
                if (e.getStateChange() != ItemEvent.SELECTED) return;
                toggleServerState(ServerState.TYPE_SELECTION);
            });
        }

        serverPanel.serverVersionSelection.addItemListener(e -> {
            if (e.getStateChange() != ItemEvent.SELECTED) return;
            toggleServerState(ServerState.VERSION_SELECTION);
        });

        context = new JPopupMenu();

        jarManager.getSupportedJars().forEach((s, serverJars) -> {
            String serverType = WordUtils.capitalizeFully(s.toLowerCase());
            JMenu menu = new JMenu(serverType);

            List<String> versions = new ArrayList<>();
            serverJars.forEach(serverJar -> {
                String version = serverJar.getVersion().replace(".jar", "");
                if (version.split("\\.").length != 3) version += ".0";
                if (version.startsWith(".")) version = "0" + version;
                versions.add(version);
            });

            versions.stream()
                    .map(Version::parse)
                    .sorted()
                    .forEach(version -> {
                        String string = version.toString();
                        if (string.endsWith(".0")) string = string.replace(".0", "");

                        JMenuItem menuItem = new JMenuItem(string);
                        String finalString = string;
                        menuItem.addActionListener(actionEvent -> {
                        });

                        menu.add(menuItem);
                    });

            context.add(menu);
        });

        if (!jarManager.getSupportedJars().isEmpty()) context.addSeparator();

        JMenuItem stopServer = new JMenuItem("Stop Server");
        stopServer.addActionListener(e -> API.stopServer());
        context.add(stopServer);

        JMenuItem killServer = new JMenuItem("Force Stop Server (Emergencies ONLY)");
        killServer.addActionListener(e -> API.killServer());
        context.add(killServer);


        Container contentPane = getContentPane();
        contentPane.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent me) {
                if (me.isPopupTrigger())
                    context.show(me.getComponent(), me.getX(), me.getY());
            }
        });

        // serverPanel.consolePane.getConsoleArea().setComponentPopupMenu(context);
    }

    private void updateComponents() {
        try {
            PropertiesHandler.handle(propertiesPanel);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        serverPanel.consolePane.getConsoleArea().setEditable(false);
        serverPanel.consolePane.getConsoleArea().setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        serverPanel.consolePane.getConsoleArea().setMargin(new Insets(12, 12, 12, 12));

        handleRamField();
        handlePortField();

        Utils.handleToggleButton(settingsPanel.getEulaToggle(), AppConfig.acceptEULA, value -> {
            AppConfig.acceptEULA = value;
            AppConfig.saveData();
        });

        {
            JComboBox<Theme> themeSelector = settingsPanel.getThemeSelection();
            for (Theme theme : Theme.values()) {
                if (theme == Theme.UNKNOWN) continue;
                themeSelector.addItem(theme);
            }
            themeSelector.setSelectedItem(AppConfig.theme);
            themeSelector.addItemListener(e -> {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    Theme theme = ((Theme) e.getItem());
                    theme.apply();
                    FlatLaf.updateUI();

                    AppConfig.theme = theme;
                    AppConfig.saveData();
                }
            });
        }

        serverPanel.serverCommand.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if ((e.getKeyCode() != 10) && (e.getKeyCode() != 13)) return;
                String text = serverPanel.serverCommand.getText();
                if (text == null || text.trim().isBlank()) return;
                serverPanel.serverCommand.setText("");

                text = text.trim();

                // Display help information
                if ("??".equalsIgnoreCase(text)) {
                    AppConfig.sendAppMessage(() -> {
                        System.out.println(" Master Commands:");
                        System.out.println(" ?? welcome");
                        System.out.println("    Will explain how to use the application in simple steps");
                        System.out.println(" ?? jar");
                        System.out.println("    Will explain how to name the server jar files");
                        System.out.println(" ?? clear");
                        System.out.println("    Will clear the console window of all text");
                    });
                    return;
                }

                if ("?? welcome".equalsIgnoreCase(text)) {
                    AppConfig.sendAppMessage(() -> {
                        System.out.println();
                        System.out.println(" How to use the Server Master");
                        System.out.println(" - Make sure you have server jar files in the folder");
                        System.out.println("   Also make sure you have all the server files in the same folder");
                        System.out.println("   Like the plugins/world folder as well as all the configuration files");
                        System.out.println(" - Right Click in the center");
                        System.out.println(" - Hover over what type of server you want to run");
                        System.out.println(" - Click the version you want to run for that server type");
                        System.out.println("  The Application will automatically change what world is used");
                        System.out.println("  based on what version of server you are running, everything else");
                        System.out.println("  is shared between all the versions (plugins & configs)");
                        System.out.println();
                    });
                    return;
                }

                if ("?? jar".equalsIgnoreCase(text)) {
                    AppConfig.sendAppMessage(() -> {
                        System.out.println(" How to add server jar files:");
                        System.out.println(" - Simply drag and drop the selected server's jar file");
                        System.out.println(" - Rename the jar file into this format: ServerType-MCVersion.jar");
                        System.out.println("   Example: spigot-1.18.2.jar");
                        System.out.println(" - Once you add a new jar file you will need to restart the application");
                    });
                    return;
                }

                if ("?? clear".equalsIgnoreCase(text) || "cls".equalsIgnoreCase(text)) {
                    serverPanel.consolePane.getConsoleArea().setText("");
                    return;
                }
                API.sendServerCommand(text);
            }
        });
    }

    private void handleRamField() {
        JTextField ramField = settingsPanel.getServerRam();
        Utils.numericalCharacterCheck(ramField);
        ramField.getDocument().addDocumentListener(getMemoryChangedListener());
    }

    private void handlePortField() {
        JTextField portField = settingsPanel.getServerPort();
        Utils.numericalCharacterCheck(portField);
        portField.getDocument().addDocumentListener(getPortChangedListener());
    }

    private DelayedTextChangedListener getPortChangedListener() {
        DelayedTextChangedListener listener = new DelayedTextChangedListener(2000);
        listener.addChangeListener(e -> {
            try {
                String rawText = settingsPanel.getServerPort().getText().trim();
                if (rawText.isBlank()) {
                    AppConfig.port = 25565;
                }

                int newPort = Integer.parseInt(rawText);
                if (newPort > 65535) {
                    JOptionPane.showMessageDialog(null,
                            "Error: Port number over the max, lowering...", "Error Message",
                            JOptionPane.ERROR_MESSAGE);
                    newPort = 65535;
                    settingsPanel.getServerPort().setText("65535");
                }

                if (newPort < 0) {
                    JOptionPane.showMessageDialog(null,
                            "Error: Port numbers must be more greater than 0", "Error Message",
                            JOptionPane.ERROR_MESSAGE);
                    newPort = 25565;
                    settingsPanel.getServerPort().setText("25565");
                }

                AppConfig.port = newPort;
                AppConfig.saveData();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(null,
                        "Error: Invalid number entered for the 'Server Port' - Default: 25565", "Error Message",
                        JOptionPane.ERROR_MESSAGE);
            }
        });
        return listener;
    }
    private DelayedTextChangedListener getMemoryChangedListener() {
        DelayedTextChangedListener listener = new DelayedTextChangedListener(2000);
        listener.addChangeListener(e -> {
            try {
                String rawText = settingsPanel.getServerRam().getText().trim();
                if (rawText.isBlank()) {
                    AppConfig.ram = 1024;
                    settingsPanel.getServerRam().setText("1024");
                    AppConfig.saveData();
                    return;
                }

                int newRam = Integer.parseInt(rawText);
                if (newRam < 512) {
                    JOptionPane.showMessageDialog(null,
                            "Error: Ram is recommended to be over 512", "Error Message",
                            JOptionPane.ERROR_MESSAGE);
                    newRam = 512;
                    settingsPanel.getServerRam().setText("512");
                }

                AppConfig.ram = newRam;
                AppConfig.saveData();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(null,
                        "Error: Invalid number entered for the 'Server Ram' - Default: 1024", "Error Message",
                        JOptionPane.ERROR_MESSAGE);

                AppConfig.ram = 1024;
                settingsPanel.getServerRam().setText("1024");
                AppConfig.saveData();
            }
        });
        return listener;
    }

    public ConsolePane getConsolePane() {
        return serverPanel.consolePane;
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        // Generated using JFormDesigner non-commercial license
        tabbedPane1 = new JTabbedPane();
        serverPanel = new ServerPanel();
        propertiesPanel = new PropertiesPanel();
        settingsPanel = new SettingsPanel();

        tabbedPane1.addTab("Server Console", serverPanel);
        tabbedPane1.addTab("Server Properties", propertiesPanel);
        tabbedPane1.addTab("Settings", settingsPanel);
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    // Generated using JFormDesigner non-commercial license
    private JTabbedPane tabbedPane1;
    private ServerPanel serverPanel;
    private PropertiesPanel propertiesPanel;
    private SettingsPanel settingsPanel;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on

    public class ConsolePane extends JScrollPane {

        public ConsolePane() {
            initComponents();
        }

        public JTextPane getConsoleArea() {
            return consoleOutput;
        }

        private void initComponents() {
            // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
            // Generated using JFormDesigner non-commercial license
            consoleOutput = new JTextPane();


            consoleOutput.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
            consoleOutput.setEditable(false);
            consoleOutput.setFocusable(false);
            setViewportView(consoleOutput);
            // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
        }

        // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
        // Generated using JFormDesigner non-commercial license
        private JTextPane consoleOutput;
        // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
    }

    private class SettingsPanel extends JPanel {
        private SettingsPanel() {
            initComponents();
        }

        public JToggleButton getEulaToggle() { // eulaToggle
            return eulaToggle;
        }

        public JComboBox<Theme> getThemeSelection() {
            return themeSelection;
        }

        public JTextField getServerPort() {
            return serverPort;
        }

        public JTextField getServerRam() {
            return serverRam;
        }

        private void initComponents() {
            // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
            // Generated using JFormDesigner non-commercial license
            panel3 = new JPanel();
            serverRam = new JTextField();
            themeSelection = new JComboBox();
            label7 = new JLabel();
            label8 = new JLabel();
            label9 = new JLabel();
            label10 = new JLabel();
            label11 = new JLabel();
            serverPort = new JTextField();
            label12 = new JLabel();
            eulaToggle = new JToggleButton();



            serverRam.setText("1024");
            serverRam.setFont(new Font("Lucida Console", Font.PLAIN, 16));

            label7.setText("EULA");
            label7.setFont(new Font("Lucida Console", Font.PLAIN, 13));

            label8.setText("Server Ram");
            label8.setFont(new Font("Lucida Console", Font.PLAIN, 14));

            label9.setText("App Settings");
            label9.setFont(new Font("Hack", Font.BOLD, 16));
            label9.setForeground(UIManager.getColor("Label.foreground"));

            label10.setText("Server Settings");
            label10.setFont(new Font("Hack", Font.BOLD, 16));
            label10.setForeground(UIManager.getColor("Label.foreground"));

            label11.setText("Server Port");
            label11.setFont(new Font("Lucida Console", Font.PLAIN, 14));

            serverPort.setText("25565");
            serverPort.setFont(new Font("Lucida Console", Font.PLAIN, 16));

            label12.setText("App Theme");
            label12.setFont(new Font("Lucida Console", Font.PLAIN, 14));

            GroupLayout panel3Layout = new GroupLayout(panel3);
            panel3.setLayout(panel3Layout);
            panel3Layout.setHorizontalGroup(
                panel3Layout.createParallelGroup()
                    .addGroup(panel3Layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(panel3Layout.createParallelGroup()
                            .addGroup(panel3Layout.createSequentialGroup()
                                .addGroup(panel3Layout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                                    .addComponent(label8, GroupLayout.Alignment.LEADING, GroupLayout.PREFERRED_SIZE, 94, GroupLayout.PREFERRED_SIZE)
                                    .addComponent(serverRam, GroupLayout.Alignment.LEADING, GroupLayout.PREFERRED_SIZE, 200, GroupLayout.PREFERRED_SIZE)
                                    .addComponent(label10, GroupLayout.Alignment.LEADING, GroupLayout.PREFERRED_SIZE, 176, GroupLayout.PREFERRED_SIZE))
                                .addGroup(panel3Layout.createParallelGroup()
                                    .addGroup(panel3Layout.createSequentialGroup()
                                        .addGap(11, 11, 11)
                                        .addComponent(label11, GroupLayout.PREFERRED_SIZE, 94, GroupLayout.PREFERRED_SIZE))
                                    .addGroup(panel3Layout.createSequentialGroup()
                                        .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                        .addComponent(serverPort, GroupLayout.PREFERRED_SIZE, 200, GroupLayout.PREFERRED_SIZE)))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(panel3Layout.createParallelGroup()
                                    .addComponent(label7, GroupLayout.PREFERRED_SIZE, 40, GroupLayout.PREFERRED_SIZE)
                                    .addComponent(eulaToggle, GroupLayout.PREFERRED_SIZE, 40, GroupLayout.PREFERRED_SIZE))
                                .addContainerGap())
                            .addGroup(panel3Layout.createSequentialGroup()
                                .addGroup(panel3Layout.createParallelGroup()
                                    .addComponent(label9, GroupLayout.PREFERRED_SIZE, 176, GroupLayout.PREFERRED_SIZE)
                                    .addComponent(label12, GroupLayout.PREFERRED_SIZE, 94, GroupLayout.PREFERRED_SIZE)
                                    .addComponent(themeSelection, GroupLayout.PREFERRED_SIZE, 200, GroupLayout.PREFERRED_SIZE))
                                .addGap(0, 698, Short.MAX_VALUE))))
            );
            panel3Layout.setVerticalGroup(
                panel3Layout.createParallelGroup()
                    .addGroup(panel3Layout.createSequentialGroup()
                        .addGap(31, 31, 31)
                        .addComponent(label10)
                        .addGroup(panel3Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                            .addComponent(label8, GroupLayout.PREFERRED_SIZE, 19, GroupLayout.PREFERRED_SIZE)
                            .addComponent(label11, GroupLayout.PREFERRED_SIZE, 19, GroupLayout.PREFERRED_SIZE)
                            .addComponent(label7, GroupLayout.PREFERRED_SIZE, 24, GroupLayout.PREFERRED_SIZE))
                        .addGap(1, 1, 1)
                        .addGroup(panel3Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                            .addComponent(serverRam, GroupLayout.PREFERRED_SIZE, 40, GroupLayout.PREFERRED_SIZE)
                            .addComponent(serverPort, GroupLayout.PREFERRED_SIZE, 41, GroupLayout.PREFERRED_SIZE)
                            .addComponent(eulaToggle, GroupLayout.PREFERRED_SIZE, 40, GroupLayout.PREFERRED_SIZE))
                        .addGap(143, 143, 143)
                        .addComponent(label9)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(label12, GroupLayout.PREFERRED_SIZE, 19, GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(themeSelection, GroupLayout.PREFERRED_SIZE, 40, GroupLayout.PREFERRED_SIZE)
                        .addContainerGap(288, Short.MAX_VALUE))
            );

            GroupLayout layout = new GroupLayout(this);
            setLayout(layout);
            layout.setHorizontalGroup(
                layout.createParallelGroup()
                    .addGroup(layout.createSequentialGroup()
                        .addGap(40, 40, 40)
                        .addComponent(panel3, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        .addContainerGap(56, Short.MAX_VALUE))
            );
            layout.setVerticalGroup(
                layout.createParallelGroup()
                    .addGroup(layout.createSequentialGroup()
                        .addGap(36, 36, 36)
                        .addComponent(panel3, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        .addContainerGap(40, Short.MAX_VALUE))
            );
            // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
        }

        // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
        // Generated using JFormDesigner non-commercial license
        private JPanel panel3;
        private JTextField serverRam;
        private JComboBox themeSelection;
        private JLabel label7;
        private JLabel label8;
        private JLabel label9;
        private JLabel label10;
        private JLabel label11;
        private JTextField serverPort;
        private JLabel label12;
        private JToggleButton eulaToggle;
        // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
    }

    private class ServerPanel extends JPanel {
        private ServerPanel() {
            initComponents();
        }

        private void initComponents() {
            // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
            // Generated using JFormDesigner non-commercial license
            consolePane = new ConsolePane();
            serverCommand = new JTextField();
            startServer = new JButton();
            serverTypeSelection = new JComboBox();
            forceStop = new JButton();
            stopServer = new JButton();
            serverVersionSelection = new JComboBox();


            serverCommand.setEnabled(false);

            startServer.setText("START SERVER");
            startServer.setEnabled(false);

            serverTypeSelection.setToolTipText("Server Type");

            forceStop.setText("FORCE STOP (EMERGENCIES ONLY)");
            forceStop.setBackground(UIManager.getColor("TextField.selectionBackground"));
            forceStop.setForeground(UIManager.getColor("TextField.foreground"));
            forceStop.setHorizontalTextPosition(SwingConstants.RIGHT);
            forceStop.setEnabled(false);

            stopServer.setText("STOP SERVER");
            stopServer.setBackground(UIManager.getColor("List.selectionBackground"));
            stopServer.setForeground(UIManager.getColor("List.foreground"));
            stopServer.setEnabled(false);

            serverVersionSelection.setEnabled(false);
            serverVersionSelection.setToolTipText("Server Version");

            GroupLayout layout = new GroupLayout(this);
            setLayout(layout);
            layout.setHorizontalGroup(
                layout.createParallelGroup()
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(layout.createParallelGroup()
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(serverCommand, GroupLayout.DEFAULT_SIZE, 873, Short.MAX_VALUE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(startServer, GroupLayout.PREFERRED_SIZE, 109, GroupLayout.PREFERRED_SIZE))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(serverTypeSelection, GroupLayout.PREFERRED_SIZE, 150, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(serverVersionSelection, GroupLayout.PREFERRED_SIZE, 175, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 270, Short.MAX_VALUE)
                                .addComponent(stopServer, GroupLayout.PREFERRED_SIZE, 150, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(forceStop, GroupLayout.PREFERRED_SIZE, 231, GroupLayout.PREFERRED_SIZE))
                            .addComponent(consolePane, GroupLayout.DEFAULT_SIZE, 988, Short.MAX_VALUE))
                        .addContainerGap())
            );
            layout.setVerticalGroup(
                layout.createParallelGroup()
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
                            .addComponent(stopServer, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(forceStop, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(serverTypeSelection, GroupLayout.PREFERRED_SIZE, 45, GroupLayout.PREFERRED_SIZE)
                                .addComponent(serverVersionSelection, GroupLayout.PREFERRED_SIZE, 45, GroupLayout.PREFERRED_SIZE)))
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(consolePane, GroupLayout.DEFAULT_SIZE, 601, Short.MAX_VALUE)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup()
                            .addComponent(startServer, GroupLayout.PREFERRED_SIZE, 43, GroupLayout.PREFERRED_SIZE)
                            .addComponent(serverCommand, GroupLayout.PREFERRED_SIZE, 43, GroupLayout.PREFERRED_SIZE))
                        .addContainerGap())
            );
            // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
        }

        // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
        // Generated using JFormDesigner non-commercial license
        private ConsolePane consolePane;
        private JTextField serverCommand;
        private JButton startServer;
        private JComboBox serverTypeSelection;
        private JButton forceStop;
        private JButton stopServer;
        private JComboBox serverVersionSelection;
        // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
    }

    public class PropertiesPanel extends JPanel {
        private PropertiesPanel() {
            initComponents();
        }

        private void initComponents() {
            // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
            // Generated using JFormDesigner non-commercial license
            tabbedPane2 = new JTabbedPane();
            panel4 = new JPanel();
            cmdBlocksField = new JToggleButton();
            label1 = new JLabel();
            queryField = new JToggleButton();
            label3 = new JLabel();
            secureprofile = new JToggleButton();
            label4 = new JLabel();
            pvpField = new JToggleButton();
            label5 = new JLabel();
            structureField = new JToggleButton();
            label6 = new JLabel();
            resourcePackToggle = new JToggleButton();
            label13 = new JLabel();
            useNativeTransport = new JToggleButton();
            label14 = new JLabel();
            onlineMode = new JToggleButton();
            label15 = new JLabel();
            toggleStatus = new JToggleButton();
            label16 = new JLabel();
            toggleFlight = new JToggleButton();
            label17 = new JLabel();
            hideOnlinePlayers = new JToggleButton();
            label18 = new JLabel();
            preventProxies = new JToggleButton();
            label19 = new JLabel();
            syncChunks = new JToggleButton();
            label20 = new JLabel();
            toggleRcon = new JToggleButton();
            label21 = new JLabel();
            toggleNether = new JToggleButton();
            label22 = new JLabel();
            broadcastRcon = new JToggleButton();
            label23 = new JLabel();
            forceGamemode = new JToggleButton();
            label24 = new JLabel();
            hardcore = new JToggleButton();
            label25 = new JLabel();
            whitelist = new JToggleButton();
            label26 = new JLabel();
            broadcastConsoleMessages = new JToggleButton();
            label27 = new JLabel();
            logIps = new JToggleButton();
            label29 = new JLabel();
            enforceWhitelist = new JToggleButton();
            label30 = new JLabel();
            spawnMonsters = new JToggleButton();
            label31 = new JLabel();
            spawnAnimals = new JToggleButton();
            label32 = new JLabel();
            spawnNpc = new JToggleButton();
            label33 = new JLabel();




            label1.setText("Command Blocks");
            label1.setFont(new Font("Hack", Font.PLAIN, 14));
            label1.setToolTipText("Enables command blocks");

            label3.setText("Query");
            label3.setFont(new Font("Hack", Font.PLAIN, 14));
            label3.setToolTipText("Enables GameSpy4 protocol server listener. Used to get information about server");

            label4.setText("Secure Profile");
            label4.setFont(new Font("Hack", Font.PLAIN, 14));
            label4.setToolTipText("If set to true, players without a Mojang-signed public key will not be able to connect to the server");

            label5.setText("PVP");
            label5.setFont(new Font("Hack", Font.PLAIN, 14));
            label5.setToolTipText("Enable PvP on the server. Players shooting themselves with arrows receive damage only if PvP is enabled");

            label6.setText("Generate Structures");
            label6.setFont(new Font("Hack", Font.PLAIN, 14));
            label6.setToolTipText("Defines whether structures (such as villages) can be generated");

            label13.setText("Require Resource Pack");
            label13.setFont(new Font("Hack", Font.PLAIN, 14));
            label13.setToolTipText("When this option is enabled (set to true), players will be prompted for a response and will be disconnected if they decline the required pack");

            label14.setText("Native Transport");
            label14.setFont(new Font("Hack", Font.PLAIN, 14));
            label14.setToolTipText("Linux server performance improvements: optimized packet sending/receiving on Linux");

            label15.setText("Online Mode");
            label15.setFont(new Font("Hack", Font.PLAIN, 14));
            label15.setToolTipText("Server checks connecting players against Minecraft account database");

            label16.setText("Show Server Status");
            label16.setFont(new Font("Hack", Font.PLAIN, 14));
            label16.setToolTipText("If set to false, it will suppress replies from clients. This means it will appear as offline, but will still accept connections");

            label17.setText("Allow Flight");
            label17.setFont(new Font("Hack", Font.PLAIN, 14));
            label17.setToolTipText("Allows users to use flight on the server while in Survival mode, if they have a mod that provides flight installed");

            label18.setText("Hide Online Players");
            label18.setFont(new Font("Hack", Font.PLAIN, 14));

            label19.setText("Prevent Proxies");
            label19.setFont(new Font("Hack", Font.PLAIN, 14));
            label19.setToolTipText("If the ISP/AS sent from the server is different from the one from Mojang Studios' authentication server, the player is kicked");

            label20.setText("Sync Chunks");
            label20.setFont(new Font("Hack", Font.PLAIN, 14));
            label20.setToolTipText("Enables synchronous chunk writes");

            label21.setText("Rcon");
            label21.setFont(new Font("Hack", Font.PLAIN, 14));
            label21.setToolTipText("Enables remote access to the server console");

            label22.setText("Allow The Nether");
            label22.setFont(new Font("Hack", Font.PLAIN, 14));
            label22.setToolTipText("Allows players to travel to the Nether");

            label23.setText("Broadcast Rcon");
            label23.setFont(new Font("Hack", Font.PLAIN, 14));
            label23.setToolTipText("Send rcon console command outputs to all online operators");

            label24.setText("Force Gamemode");
            label24.setFont(new Font("Hack", Font.PLAIN, 14));

            label25.setText("Hardcore");
            label25.setFont(new Font("Hack", Font.PLAIN, 14));
            label25.setToolTipText("If set to true, server difficulty is ignored and set to hard and players are set to spectator mode if they die");

            label26.setText("Whitelist");
            label26.setFont(new Font("Hack", Font.PLAIN, 14));
            label26.setToolTipText("With a whitelist enabled, users not on the whitelist cannot connect. Intended for private servers, such as those for real-life friends or strangers carefully selected via an application process");

            label27.setText("Broadcast Console");
            label27.setFont(new Font("Hack", Font.PLAIN, 14));
            label27.setToolTipText("Send console command outputs to all online operators");

            label29.setText("Log IPs");
            label29.setFont(new Font("Hack", Font.PLAIN, 14));

            label30.setText("Enforce Whitelist");
            label30.setFont(new Font("Hack", Font.PLAIN, 14));
            label30.setToolTipText("When this option is enabled, users who are not present on the whitelist (if it's enabled) get kicked from the server after the server reloads the whitelist file");

            label31.setText("Spawn Monsters");
            label31.setFont(new Font("Hack", Font.PLAIN, 14));
            label31.setToolTipText("Determines if monsters can spawn");

            label32.setText("Spawn Animals");
            label32.setFont(new Font("Hack", Font.PLAIN, 14));
            label32.setToolTipText("Determines if animals can spawn");

            label33.setText("Spawn NPCs");
            label33.setFont(new Font("Hack", Font.PLAIN, 14));
            label33.setToolTipText("Determines whether villagers can spawn");

            GroupLayout panel4Layout = new GroupLayout(panel4);
            panel4.setLayout(panel4Layout);
            panel4Layout.setHorizontalGroup(
                panel4Layout.createParallelGroup()
                    .addGroup(panel4Layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(panel4Layout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
                            .addGroup(panel4Layout.createSequentialGroup()
                                .addComponent(toggleFlight, GroupLayout.PREFERRED_SIZE, 40, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(label17)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(broadcastConsoleMessages, GroupLayout.PREFERRED_SIZE, 40, GroupLayout.PREFERRED_SIZE))
                            .addGroup(GroupLayout.Alignment.TRAILING, panel4Layout.createSequentialGroup()
                                .addComponent(toggleStatus, GroupLayout.PREFERRED_SIZE, 40, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(label16)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(whitelist, GroupLayout.PREFERRED_SIZE, 40, GroupLayout.PREFERRED_SIZE))
                            .addGroup(GroupLayout.Alignment.TRAILING, panel4Layout.createSequentialGroup()
                                .addComponent(onlineMode, GroupLayout.PREFERRED_SIZE, 40, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(label15)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(hardcore, GroupLayout.PREFERRED_SIZE, 40, GroupLayout.PREFERRED_SIZE))
                            .addGroup(GroupLayout.Alignment.TRAILING, panel4Layout.createSequentialGroup()
                                .addComponent(useNativeTransport, GroupLayout.PREFERRED_SIZE, 40, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(label14)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(forceGamemode, GroupLayout.PREFERRED_SIZE, 40, GroupLayout.PREFERRED_SIZE))
                            .addGroup(GroupLayout.Alignment.TRAILING, panel4Layout.createSequentialGroup()
                                .addComponent(cmdBlocksField, GroupLayout.PREFERRED_SIZE, 40, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(label1)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(broadcastRcon, GroupLayout.PREFERRED_SIZE, 40, GroupLayout.PREFERRED_SIZE))
                            .addGroup(GroupLayout.Alignment.TRAILING, panel4Layout.createSequentialGroup()
                                .addComponent(queryField, GroupLayout.PREFERRED_SIZE, 40, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(label3)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(toggleNether, GroupLayout.PREFERRED_SIZE, 40, GroupLayout.PREFERRED_SIZE))
                            .addGroup(GroupLayout.Alignment.TRAILING, panel4Layout.createSequentialGroup()
                                .addComponent(secureprofile, GroupLayout.PREFERRED_SIZE, 40, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(label4)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(toggleRcon, GroupLayout.PREFERRED_SIZE, 40, GroupLayout.PREFERRED_SIZE))
                            .addGroup(GroupLayout.Alignment.TRAILING, panel4Layout.createSequentialGroup()
                                .addComponent(pvpField, GroupLayout.PREFERRED_SIZE, 40, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(label5)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(syncChunks, GroupLayout.PREFERRED_SIZE, 40, GroupLayout.PREFERRED_SIZE))
                            .addGroup(panel4Layout.createSequentialGroup()
                                .addComponent(structureField, GroupLayout.PREFERRED_SIZE, 40, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(label6)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(preventProxies, GroupLayout.PREFERRED_SIZE, 40, GroupLayout.PREFERRED_SIZE))
                            .addGroup(panel4Layout.createSequentialGroup()
                                .addComponent(resourcePackToggle, GroupLayout.PREFERRED_SIZE, 40, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(label13)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(hideOnlinePlayers, GroupLayout.PREFERRED_SIZE, 40, GroupLayout.PREFERRED_SIZE)))
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(panel4Layout.createParallelGroup()
                            .addGroup(panel4Layout.createSequentialGroup()
                                .addGroup(panel4Layout.createParallelGroup()
                                    .addComponent(label22)
                                    .addComponent(label19)
                                    .addComponent(label20)
                                    .addComponent(label21)
                                    .addComponent(label23))
                                .addGroup(panel4Layout.createParallelGroup()
                                    .addGroup(panel4Layout.createSequentialGroup()
                                        .addGap(29, 29, 29)
                                        .addComponent(spawnAnimals, GroupLayout.PREFERRED_SIZE, 40, GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(label32))
                                    .addGroup(panel4Layout.createSequentialGroup()
                                        .addGap(30, 30, 30)
                                        .addComponent(spawnMonsters, GroupLayout.PREFERRED_SIZE, 40, GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(label31))
                                    .addGroup(panel4Layout.createSequentialGroup()
                                        .addGap(30, 30, 30)
                                        .addGroup(panel4Layout.createParallelGroup()
                                            .addGroup(panel4Layout.createSequentialGroup()
                                                .addComponent(logIps, GroupLayout.PREFERRED_SIZE, 40, GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(label29))
                                            .addGroup(panel4Layout.createSequentialGroup()
                                                .addComponent(enforceWhitelist, GroupLayout.PREFERRED_SIZE, 40, GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(label30))))))
                            .addComponent(label24)
                            .addComponent(label25)
                            .addComponent(label26)
                            .addComponent(label27)
                            .addGroup(panel4Layout.createSequentialGroup()
                                .addGroup(panel4Layout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                                    .addComponent(spawnNpc, GroupLayout.PREFERRED_SIZE, 40, GroupLayout.PREFERRED_SIZE)
                                    .addGroup(panel4Layout.createSequentialGroup()
                                        .addComponent(label18)
                                        .addGap(46, 46, 46)))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(label33)))
                        .addContainerGap(332, Short.MAX_VALUE))
            );
            panel4Layout.setVerticalGroup(
                panel4Layout.createParallelGroup()
                    .addGroup(panel4Layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(panel4Layout.createParallelGroup()
                            .addGroup(panel4Layout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
                                .addGroup(panel4Layout.createSequentialGroup()
                                    .addGap(190, 190, 190)
                                    .addComponent(label19, GroupLayout.PREFERRED_SIZE, 40, GroupLayout.PREFERRED_SIZE))
                                .addGroup(GroupLayout.Alignment.TRAILING, panel4Layout.createSequentialGroup()
                                    .addGroup(panel4Layout.createParallelGroup()
                                        .addComponent(spawnNpc, GroupLayout.PREFERRED_SIZE, 40, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(label33, GroupLayout.PREFERRED_SIZE, 40, GroupLayout.PREFERRED_SIZE))
                                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addGroup(panel4Layout.createParallelGroup()
                                        .addComponent(label31, GroupLayout.PREFERRED_SIZE, 40, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(spawnMonsters, GroupLayout.PREFERRED_SIZE, 40, GroupLayout.PREFERRED_SIZE))
                                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                    .addGroup(panel4Layout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                                        .addComponent(enforceWhitelist, GroupLayout.PREFERRED_SIZE, 40, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(label30, GroupLayout.PREFERRED_SIZE, 40, GroupLayout.PREFERRED_SIZE))
                                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                    .addGroup(panel4Layout.createParallelGroup()
                                        .addComponent(label29, GroupLayout.PREFERRED_SIZE, 40, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(logIps, GroupLayout.PREFERRED_SIZE, 40, GroupLayout.PREFERRED_SIZE))
                                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)))
                            .addGroup(panel4Layout.createSequentialGroup()
                                .addGroup(panel4Layout.createParallelGroup()
                                    .addComponent(label1, GroupLayout.PREFERRED_SIZE, 40, GroupLayout.PREFERRED_SIZE)
                                    .addComponent(cmdBlocksField, GroupLayout.PREFERRED_SIZE, 40, GroupLayout.PREFERRED_SIZE)
                                    .addComponent(broadcastRcon, GroupLayout.PREFERRED_SIZE, 40, GroupLayout.PREFERRED_SIZE)
                                    .addComponent(label23, GroupLayout.PREFERRED_SIZE, 40, GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(panel4Layout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                                    .addComponent(queryField, GroupLayout.PREFERRED_SIZE, 40, GroupLayout.PREFERRED_SIZE)
                                    .addComponent(label3, GroupLayout.PREFERRED_SIZE, 40, GroupLayout.PREFERRED_SIZE)
                                    .addComponent(toggleNether, GroupLayout.PREFERRED_SIZE, 40, GroupLayout.PREFERRED_SIZE)
                                    .addComponent(label22, GroupLayout.PREFERRED_SIZE, 40, GroupLayout.PREFERRED_SIZE)
                                    .addComponent(spawnAnimals, GroupLayout.PREFERRED_SIZE, 40, GroupLayout.PREFERRED_SIZE)
                                    .addComponent(label32, GroupLayout.PREFERRED_SIZE, 40, GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(panel4Layout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                                    .addGroup(panel4Layout.createParallelGroup()
                                        .addComponent(secureprofile, GroupLayout.PREFERRED_SIZE, 40, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(label4, GroupLayout.PREFERRED_SIZE, 40, GroupLayout.PREFERRED_SIZE))
                                    .addComponent(toggleRcon, GroupLayout.PREFERRED_SIZE, 40, GroupLayout.PREFERRED_SIZE)
                                    .addComponent(label21, GroupLayout.PREFERRED_SIZE, 40, GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(panel4Layout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                                    .addComponent(pvpField, GroupLayout.PREFERRED_SIZE, 40, GroupLayout.PREFERRED_SIZE)
                                    .addComponent(label5, GroupLayout.PREFERRED_SIZE, 40, GroupLayout.PREFERRED_SIZE)
                                    .addComponent(syncChunks, GroupLayout.PREFERRED_SIZE, 40, GroupLayout.PREFERRED_SIZE)
                                    .addComponent(label20, GroupLayout.PREFERRED_SIZE, 40, GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(panel4Layout.createParallelGroup()
                                    .addComponent(preventProxies, GroupLayout.PREFERRED_SIZE, 40, GroupLayout.PREFERRED_SIZE)
                                    .addComponent(structureField, GroupLayout.PREFERRED_SIZE, 40, GroupLayout.PREFERRED_SIZE)
                                    .addComponent(label6, GroupLayout.PREFERRED_SIZE, 40, GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)))
                        .addGroup(panel4Layout.createParallelGroup()
                            .addComponent(resourcePackToggle, GroupLayout.PREFERRED_SIZE, 40, GroupLayout.PREFERRED_SIZE)
                            .addComponent(label13, GroupLayout.PREFERRED_SIZE, 40, GroupLayout.PREFERRED_SIZE)
                            .addComponent(hideOnlinePlayers, GroupLayout.PREFERRED_SIZE, 40, GroupLayout.PREFERRED_SIZE)
                            .addComponent(label18, GroupLayout.PREFERRED_SIZE, 40, GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(panel4Layout.createParallelGroup()
                            .addComponent(useNativeTransport, GroupLayout.PREFERRED_SIZE, 40, GroupLayout.PREFERRED_SIZE)
                            .addComponent(label14, GroupLayout.PREFERRED_SIZE, 40, GroupLayout.PREFERRED_SIZE)
                            .addComponent(forceGamemode, GroupLayout.PREFERRED_SIZE, 40, GroupLayout.PREFERRED_SIZE)
                            .addComponent(label24, GroupLayout.PREFERRED_SIZE, 40, GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(panel4Layout.createParallelGroup()
                            .addComponent(onlineMode, GroupLayout.PREFERRED_SIZE, 40, GroupLayout.PREFERRED_SIZE)
                            .addComponent(label15, GroupLayout.PREFERRED_SIZE, 40, GroupLayout.PREFERRED_SIZE)
                            .addComponent(hardcore, GroupLayout.PREFERRED_SIZE, 40, GroupLayout.PREFERRED_SIZE)
                            .addComponent(label25, GroupLayout.PREFERRED_SIZE, 40, GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(panel4Layout.createParallelGroup()
                            .addComponent(toggleStatus, GroupLayout.PREFERRED_SIZE, 40, GroupLayout.PREFERRED_SIZE)
                            .addComponent(label16, GroupLayout.PREFERRED_SIZE, 40, GroupLayout.PREFERRED_SIZE)
                            .addComponent(whitelist, GroupLayout.PREFERRED_SIZE, 40, GroupLayout.PREFERRED_SIZE)
                            .addComponent(label26, GroupLayout.PREFERRED_SIZE, 40, GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(panel4Layout.createParallelGroup()
                            .addGroup(panel4Layout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                                .addComponent(toggleFlight, GroupLayout.PREFERRED_SIZE, 40, GroupLayout.PREFERRED_SIZE)
                                .addComponent(label17, GroupLayout.PREFERRED_SIZE, 40, GroupLayout.PREFERRED_SIZE))
                            .addComponent(label27, GroupLayout.PREFERRED_SIZE, 40, GroupLayout.PREFERRED_SIZE)
                            .addComponent(broadcastConsoleMessages, GroupLayout.PREFERRED_SIZE, 40, GroupLayout.PREFERRED_SIZE))
                        .addContainerGap(175, Short.MAX_VALUE))
            );
            tabbedPane2.addTab("Toggle Properties", panel4);

            GroupLayout layout = new GroupLayout(this);
            setLayout(layout);
            layout.setHorizontalGroup(
                layout.createParallelGroup()
                    .addGroup(layout.createSequentialGroup()
                        .addGap(22, 22, 22)
                        .addComponent(tabbedPane2)
                        .addGap(34, 34, 34))
            );
            layout.setVerticalGroup(
                layout.createParallelGroup()
                    .addGroup(layout.createSequentialGroup()
                        .addGap(18, 18, 18)
                        .addComponent(tabbedPane2, GroupLayout.PREFERRED_SIZE, 667, GroupLayout.PREFERRED_SIZE)
                        .addContainerGap(28, Short.MAX_VALUE))
            );
            // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
        }

        // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
        // Generated using JFormDesigner non-commercial license
        private JTabbedPane tabbedPane2;
        private JPanel panel4;
        public JToggleButton cmdBlocksField;
        private JLabel label1;
        public JToggleButton queryField;
        private JLabel label3;
        public JToggleButton secureprofile;
        private JLabel label4;
        public JToggleButton pvpField;
        private JLabel label5;
        public JToggleButton structureField;
        private JLabel label6;
        public JToggleButton resourcePackToggle;
        private JLabel label13;
        public JToggleButton useNativeTransport;
        private JLabel label14;
        public JToggleButton onlineMode;
        private JLabel label15;
        public JToggleButton toggleStatus;
        private JLabel label16;
        public JToggleButton toggleFlight;
        private JLabel label17;
        public JToggleButton hideOnlinePlayers;
        private JLabel label18;
        public JToggleButton preventProxies;
        private JLabel label19;
        public JToggleButton syncChunks;
        private JLabel label20;
        public JToggleButton toggleRcon;
        private JLabel label21;
        public JToggleButton toggleNether;
        private JLabel label22;
        public JToggleButton broadcastRcon;
        private JLabel label23;
        public JToggleButton forceGamemode;
        private JLabel label24;
        public JToggleButton hardcore;
        private JLabel label25;
        public JToggleButton whitelist;
        private JLabel label26;
        public JToggleButton broadcastConsoleMessages;
        private JLabel label27;
        public JToggleButton logIps;
        private JLabel label29;
        public JToggleButton enforceWhitelist;
        private JLabel label30;
        public JToggleButton spawnMonsters;
        private JLabel label31;
        public JToggleButton spawnAnimals;
        private JLabel label32;
        public JToggleButton spawnNpc;
        private JLabel label33;
        // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
    }
}
