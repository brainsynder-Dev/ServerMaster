package org.bsdevelopment.servermaster.views.serverconsole;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.Uses;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.shared.Tooltip;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.*;
import com.vaadin.flow.theme.lumo.LumoUtility;
import org.bsdevelopment.servermaster.App;
import org.bsdevelopment.servermaster.AppConfig;
import org.bsdevelopment.servermaster.server.ServerHandlerAPI;
import org.bsdevelopment.servermaster.server.ServerWrapper;
import org.bsdevelopment.servermaster.server.jar.ServerJar;
import org.bsdevelopment.servermaster.server.jar.ServerJarManager;
import org.bsdevelopment.servermaster.utils.AppUtilities;
import org.bsdevelopment.servermaster.views.MainLayout;
import org.bsdevelopment.servermaster.views.ViewHandler;
import org.bsdevelopment.servermaster.views.serverconsole.console.MessageConsole;
import org.bsdevelopment.servermaster.views.serverconsole.console.ServerLog;
import org.bsdevelopment.servermaster.views.service.ApiService;
import org.springframework.beans.factory.annotation.Autowired;

import java.awt.*;
import java.io.File;
import java.util.List;
import java.util.*;
import java.util.concurrent.TimeUnit;

@PageTitle("Server Console")
// @Route(value = "console", layout = MainLayout.class)
@Route(value = "", layout = MainLayout.class)
@Uses(Icon.class)
@PreserveOnRefresh
public class ServerConsoleView extends Composite<VerticalLayout> implements BeforeEnterObserver {
    private final LinkedHashMap<String, List<String>> versionBuildMap = new LinkedHashMap<>();
    private @Autowired ApiService apiService;

    private final Select<String> SERVER_TYPE, SERVER_VERSION, SERVER_BUILD;
    private final Button STOP_BUTTON, RESTART, FORCE_STOP, START_SERVER;
    private final TextField COMMAND_FIELD;
    public final ServerLog SERVER_LOG;

    private int historyIndex = -1;

    // Server is not running disable un-used sections
    private void toggleALL(boolean value) {
        START_SERVER.setEnabled(value);
        STOP_BUTTON.setEnabled(value);
        RESTART.setEnabled(value);
        FORCE_STOP.setEnabled(value);
    }

    // Disable all server selections
    private void toggleSelections(boolean value) {
        SERVER_TYPE.setEnabled(value);
        SERVER_VERSION.setEnabled(value);
        SERVER_BUILD.setEnabled(value);

        if ((!SERVER_TYPE.isEmpty()) && (!SERVER_VERSION.isEmpty())) {
            if (versionBuildMap.containsKey(SERVER_TYPE.getValue() + "-" + SERVER_VERSION.getValue())) {
                if (!SERVER_BUILD.isEmpty()) START_SERVER.setEnabled(true);
                return;
            }
            START_SERVER.setEnabled(true);
        }
    }

    private void fillVersions() {
        if (START_SERVER.isEnabled()) START_SERVER.setEnabled(false);
        versionBuildMap.clear();
        ServerJarManager jarManager = App.getJarManager();

        // Populate all the server versions...
        LinkedList<String> alreadyAdded = new LinkedList<>();
        jarManager.sortVersions(jarManager.getSupportedJars().getOrDefault(SERVER_TYPE.getValue(), new ArrayList<>())).forEach(version -> {
            String finalVersion = version.toString();
            if (!alreadyAdded.contains(finalVersion)) alreadyAdded.add(finalVersion);
        });
        if (alreadyAdded.isEmpty()) Notification.show("No versions were found for " + SERVER_TYPE.getValue());

        Collections.reverse(alreadyAdded);
        SERVER_VERSION.setItems(alreadyAdded);
    }

    private void fillBuilds() {
        SERVER_BUILD.clear();

        LinkedList<ServerJar> jars = App.getJarManager().getVersionBuilds(SERVER_TYPE.getValue(), SERVER_VERSION.getValue());
        if (jars.isEmpty()) {
            SERVER_BUILD.setItems(new ArrayList<>());
            SERVER_BUILD.setVisible(false);
            SERVER_BUILD.setEnabled(false);

            // Final version Selection...
            if (!START_SERVER.isEnabled()) START_SERVER.setEnabled(true);
            return;
        }
        if (START_SERVER.isEnabled()) START_SERVER.setEnabled(false);
        List<String> builds = new ArrayList<>();
        jars.forEach(serverJar -> builds.add(serverJar.getBuild()));

        SERVER_BUILD.setEnabled(true);
        SERVER_BUILD.setVisible(true);
        SERVER_BUILD.setItems(builds);
    }

    public void handleFieldUpdates() {
        SERVER_TYPE.setPlaceholder("Select Server Type");
        SERVER_TYPE.clear();

        SERVER_VERSION.setPlaceholder("Select Server Version");
        SERVER_VERSION.setEnabled(false);
        SERVER_VERSION.clear();

        SERVER_BUILD.setPlaceholder("Select Server Build");
        SERVER_BUILD.setEnabled(false);
        SERVER_BUILD.setVisible(false);
        SERVER_BUILD.clear();
        versionBuildMap.clear();

        // Server Type Selection handling
        {
            List<String> types = new ArrayList<>();
            types.addAll(App.getJarManager().getSupportedJars().keySet());
            SERVER_TYPE.setItems(types);

            if (types.isEmpty()) {
                Notification.show("No server jars were found type '?? jar' to see examples");
                return;
            }

            if ((!AppConfig.serverType.equals(SERVER_TYPE.getValue()))
                    && (!AppConfig.serverType.isEmpty()) && SERVER_TYPE.isEmpty()) {
                if (SERVER_TYPE.getListDataView().contains(AppConfig.serverType)) {
                    SERVER_TYPE.setValue(AppConfig.serverType);
                    SERVER_VERSION.setEnabled(true);
                    fillVersions();
                }
            }

            SERVER_TYPE.addValueChangeListener(typeChangeEvent -> {
                // Clear version and build data for fresh info...
                {
                    SERVER_VERSION.setEnabled(true);
                    SERVER_VERSION.clear();
                    SERVER_VERSION.setItems(new ArrayList<>());

                    SERVER_BUILD.clear();
                    SERVER_BUILD.setItems(new ArrayList<>());
                    SERVER_BUILD.setVisible(false);
                    SERVER_BUILD.setEnabled(false);

                    if (START_SERVER.isEnabled()) START_SERVER.setEnabled(false);
                }

                if ((typeChangeEvent.getValue() == null) || typeChangeEvent.getValue().isEmpty()) return;

                fillVersions();
            });
        }

        // Server Version Selection handling
        {
            if ((!AppConfig.serverVersion.equals(SERVER_VERSION.getValue()))
                    && (!AppConfig.serverVersion.isEmpty()) && SERVER_VERSION.isEmpty()) {
                if (SERVER_VERSION.getListDataView().contains(AppConfig.serverVersion)) {
                    SERVER_VERSION.setValue(AppConfig.serverVersion);
                    fillBuilds();
                }
            }

            SERVER_VERSION.addValueChangeListener(versionChangeEvent -> {
                if ((versionChangeEvent.getValue() == null) || versionChangeEvent.getValue().isEmpty()) return;
                fillBuilds();
            });
        }

        // Server Build Selection handler
        {
            if ((!AppConfig.serverBuild.equals(SERVER_BUILD.getValue()))
                    && (!AppConfig.serverBuild.isEmpty()) && SERVER_BUILD.isEmpty()
                    && versionBuildMap.containsKey(SERVER_TYPE.getValue() + "-" + SERVER_VERSION.getValue())) {
                if (SERVER_BUILD.getListDataView().contains(AppConfig.serverBuild)) {
                    SERVER_BUILD.setValue(AppConfig.serverBuild);

                    if (!START_SERVER.isEnabled()) START_SERVER.setEnabled(true);
                }
            }

            SERVER_BUILD.addValueChangeListener(buildChangeEvent -> {
                if ((buildChangeEvent.getValue() == null) || buildChangeEvent.getValue().isEmpty()) return;
                if (!START_SERVER.isEnabled()) START_SERVER.setEnabled(true);
            });
        }
    }

    private void handleFields(UI ui) {
        STOP_BUTTON.addClickListener(clickEvent -> {
            ServerWrapper.getInstance().getServer().getThread().setServerStopCallback((server, statusCode) -> {
                ui.access(() -> {
                    toggleALL(false);
                    toggleSelections(true);
                    START_SERVER.setEnabled(true);
                });
            });
            ServerHandlerAPI.stopServer();
        });
        RESTART.addClickListener(clickEvent -> {
            ServerWrapper.getInstance().getServer().getThread().setServerStopCallback((server, statusCode) -> {
                ui.access(() -> handleStartup(ui));
            });
            ServerHandlerAPI.stopServer();
        });
        FORCE_STOP.addClickListener(clickEvent -> {
            ui.access(() -> {
                toggleALL(false);
                toggleSelections(true);
                START_SERVER.setEnabled(true);
            });

            ServerHandlerAPI.killServer();
        });

        AppUtilities.getDelayedMessages().forEach(System.out::println);
        AppUtilities.CONSOLE_READY = true;

        // Server is not running disable un-used sections
        toggleALL(false);
        handleFieldUpdates();

        START_SERVER.addClickListener(buttonClickEvent -> {
            if (!START_SERVER.isEnabled()) return;

            AppConfig.serverType = SERVER_TYPE.getValue();
            AppConfig.serverVersion = SERVER_VERSION.getValue();
            AppConfig.serverBuild = SERVER_BUILD.isVisible() ? SERVER_BUILD.getValue() : "";
            App.saveConfig();

            handleStartup(ui);
        });
    }

    private void handleStartup(UI ui) {
        SERVER_LOG.getLayout().removeAll();

        toggleSelections(false);

        String version = SERVER_VERSION.getValue();
        String build = SERVER_BUILD.getValue();
        if ((build == null) || build.isEmpty()) build = "";

        int port = ServerHandlerAPI.startServer(SERVER_TYPE.getValue(), version, build, (server, statusCode) -> {
            if (statusCode == 0) AppUtilities.logMessage("[SERVER-MASTER]: The server was stopped");
            if (statusCode == 1) AppUtilities.logMessage("[SERVER-MASTER]: The server was force stopped");
            if (statusCode == 2) AppUtilities.logMessage("[SERVER-MASTER]: Unknown error prevented server from starting");
            if (statusCode <= 2) {
                ui.access(() -> {
                    toggleALL(false);
                    toggleSelections(true);
                    START_SERVER.setEnabled(true);
                });
            }
        });
        System.out.println();
        AppUtilities.logMessage("[SERVER-MASTER]: Started server on: localhost:" + port);
        System.out.println();

        toggleALL(true);
        START_SERVER.setEnabled(false);
    }

    public ServerConsoleView() {
        getContent().setWidthFull();
        getContent().setHeight("100%");

        {
            HorizontalLayout topLayout = new HorizontalLayout();
            topLayout.addClassName("rounded-background-static11");
            topLayout.setWidthFull();
            topLayout.setWidth("100%");
            topLayout.addClassNames(LumoUtility.Gap.XSMALL, LumoUtility.Padding.XSMALL);
            topLayout.setHeight("min-content");
            topLayout.setAlignItems(FlexComponent.Alignment.CENTER);
            topLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.START);

            SERVER_TYPE = new Select<>();
            SERVER_TYPE.getStyle().set("--vaadin-input-field-background", "var(--secondary)");
            SERVER_TYPE.setLabel("Server Type");

            SERVER_VERSION = new Select<>();
            SERVER_VERSION.setLabel("Server Version");

            SERVER_BUILD = new Select<>();
            SERVER_BUILD.setLabel("Server Build");

            HorizontalLayout rightLayout = new HorizontalLayout();
            rightLayout.setHeightFull();
            rightLayout.setWidth("100%");
            rightLayout.addClassNames(LumoUtility.Gap.MEDIUM);
            rightLayout.setAlignItems(FlexComponent.Alignment.CENTER);
            rightLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
            rightLayout.getStyle().set("flex-grow", "1");
            {
                STOP_BUTTON = new Button("Stop");
                STOP_BUTTON.setWidth("min-content");
                STOP_BUTTON.addThemeVariants(ButtonVariant.LUMO_PRIMARY);


                RESTART = new Button("Restart");
                RESTART.setWidth("min-content");
                RESTART.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

                FORCE_STOP = new Button("Force Stop");
                FORCE_STOP.setWidth("min-content");
                FORCE_STOP.addThemeVariants(ButtonVariant.LUMO_ERROR);

                rightLayout.add(STOP_BUTTON, RESTART, FORCE_STOP);
            }

            Span reloadRepo = new Span();
            {
                reloadRepo.addClassName("pointer");
                reloadRepo.add(VaadinIcon.REFRESH.create());
                reloadRepo.getStyle().set("padding", "4px").set("padding-bottom", "10px").set("padding-left", "30px");
                reloadRepo.addClickListener(spanClickEvent -> {
                    if (ServerWrapper.getInstance().isServerRunning()) {
                        AppUtilities.sendNotification(
                                "Unable to refresh server jars while server is running",
                                NotificationVariant.LUMO_ERROR
                        );
                        return;
                    }

                    updateServerPath(App.getJarManager().getRepo().getAbsolutePath());
                    SERVER_TYPE.clear();
                    SERVER_VERSION.setEnabled(false);
                    SERVER_VERSION.clear();

                    SERVER_BUILD.setEnabled(false);
                    SERVER_BUILD.setVisible(false);
                    SERVER_BUILD.clear();
                    versionBuildMap.clear();
                });


                Tooltip.forComponent(reloadRepo)
                        .withText("Refresh Server Jars")
                        .withPosition(Tooltip.TooltipPosition.END_BOTTOM);
            }

            topLayout.add(SERVER_TYPE, SERVER_VERSION, SERVER_BUILD, reloadRepo, rightLayout);
            topLayout.setAlignSelf(FlexComponent.Alignment.END, reloadRepo);
            getContent().add(topLayout);
        }

        {
            VerticalLayout layout1 = new VerticalLayout();
            layout1.addClassName("rounded-background-static11");
            layout1.setHeight("60%");
            layout1.setWidthFull();
            getContent().setFlexGrow(1.0, layout1);
            layout1.addClassName(LumoUtility.Padding.XSMALL);
            layout1.getStyle().set("flex-grow", "1");

            layout1.add(new Span("Server Log"));

            SERVER_LOG = new ServerLog();
            SERVER_LOG.setScrollDirection(Scroller.ScrollDirection.VERTICAL);
            SERVER_LOG.setWidthFull();
            SERVER_LOG.setHeightFull();
            SERVER_LOG.getStyle().set("flex-grow", "1")
                    .set("color", "var(--text-800)")
                    .set("padding-top", "9px")
                    .set("padding-left", "9px")
                    .set("padding-bottom", "9px")
                    .set("border-radius", "15px")
                    .set("background-color", "var(--lumo-tint-5pct)");

            layout1.add(SERVER_LOG);

            {
                HorizontalLayout bottomLayout = new HorizontalLayout();
                layout1.setFlexGrow(1.0, bottomLayout);
                bottomLayout.setWidthFull();
                bottomLayout.addClassName(LumoUtility.Gap.XSMALL);
                bottomLayout.addClassName(LumoUtility.Padding.XSMALL);
                bottomLayout.setWidth("100%");
                bottomLayout.setHeight("min-content");
                bottomLayout.setAlignItems(FlexComponent.Alignment.CENTER);
                bottomLayout.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);


                COMMAND_FIELD = new TextField();
                COMMAND_FIELD.getStyle().set("flex-grow", "1");
                bottomLayout.setAlignSelf(FlexComponent.Alignment.END, COMMAND_FIELD);

                START_SERVER = new Button("Start Server");
                START_SERVER.setWidth("min-content");
                START_SERVER.setHeight("50px");
                bottomLayout.setAlignSelf(FlexComponent.Alignment.START, START_SERVER);
                bottomLayout.add(COMMAND_FIELD, START_SERVER);
                layout1.add(bottomLayout);
                layout1.setHorizontalComponentAlignment(FlexComponent.Alignment.END, bottomLayout);
            }

            getContent().add(layout1);
        }

        AppUtilities.updateTheme();

        {
            VerticalLayout layout = new VerticalLayout();
            layout.setSpacing(false);
            layout.setPadding(false);
            layout.getStyle().set("max-width", "min-content");

            Icon up = VaadinIcon.ANGLE_UP.create();
            up.setClassName("cmd-history-buttons");
            up.addClickListener(iconClickEvent -> {
                LinkedList<String> history = new LinkedList<>(Arrays.asList(AppConfig.commandHistory));
                if (history.isEmpty()) return;

                historyIndex--;
                if (historyIndex <= 0) historyIndex = (history.size()-1);
                COMMAND_FIELD.setValue(history.get(historyIndex));

                COMMAND_FIELD.focus();
            });

            Icon down = VaadinIcon.ANGLE_DOWN.create();
            down.setClassName("cmd-history-buttons");
            down.addClickListener(iconClickEvent -> {
                LinkedList<String> history = new LinkedList<>(Arrays.asList(AppConfig.commandHistory));
                if (history.isEmpty()) return;

                historyIndex++;
                if (historyIndex >= history.size()) historyIndex = 0;
                COMMAND_FIELD.setValue(history.get(historyIndex));

                COMMAND_FIELD.focus();
            });

            layout.add(up, down);
            COMMAND_FIELD.setSuffixComponent(layout);
        }

        COMMAND_FIELD.addKeyPressListener(Key.ENTER, keyPressEvent -> {
            String text = COMMAND_FIELD.getValue();
            if (text == null || text.trim().isBlank()) return;
            COMMAND_FIELD.clear();

            text = text.trim();

            {
                LinkedList<String> history = new LinkedList<>(Arrays.asList(AppConfig.commandHistory));
                if (history.isEmpty() || (!history.peekLast().equals(text))) history.addLast(text);
                if (history.size() > 20) history.removeFirst();
                AppConfig.commandHistory = history.toArray(String[]::new);
                App.saveConfig();

                historyIndex = (history.size()-1);
            }

            // Display help information
            if ("??".equalsIgnoreCase(text)) {
                AppUtilities.logMessage(AppUtilities.LogPrefix.COMMAND, "ServerMaster Commands", Arrays.asList(
                        "?? jar     - Explains how to setup server jar files",
                        "?? clear   - Clears the console log of all text"
                ));
                return;
            }

            if ("?? jar".equalsIgnoreCase(text)) {
                AppUtilities.logMessage(AppUtilities.LogPrefix.COMMAND, "Jar Formatting", Arrays.asList(
                        "Jar files should always be placed into the "+App.getJarManager().getRepo().getAbsolutePath()+" folder",
                        "and they can have one of two different name styles:",
                        "- TYPE-VERSION.jar - Where the TYPE is what type of server it is and VERSION is the MC version",
                        "@SPACE20@ Example: spigot-1.20.4.jar",
                        "- TYPE-VERSION-BUILD.jar - Same info above, except BUILD is a unique identifier for this jar",
                        "@SPACE20@ Example: paper-1.20.4-463.jar",
                        "The BUILD identifier can be a multitude of things such as a build number, commit hash, or plain text",
                        "",
                        "Once you setup the jar file you can click the 'Refresh Server Jars' button",
                        "It will then refresh the stored data and update the jars with the new data"
                ));
                return;
            }

            if ("?? clear".equalsIgnoreCase(text) || "cls".equalsIgnoreCase(text)) {
                SERVER_LOG.getLayout().removeAll();
                return;
            }

            if (ServerWrapper.getInstance().getServer() == null) {
                Notification.show("Server is currently offline and unable to send this command");
                return;
            }

            ServerHandlerAPI.sendServerCommand(text);
        });
        handleDialogs();

        if (AppUtilities.OFFLINE) {
            String message = """
                    It appears you are either offline or ran into a connection issue.
                    If you think this is an issue please reopen the app
                    If it still persists report it: https://github.com/brainsynder-Dev/ServerMaster/issues
                    """;

            Notification offlineNotification = new Notification(message);
            offlineNotification.setPosition(Notification.Position.BOTTOM_CENTER);
            offlineNotification.addThemeVariants(NotificationVariant.LUMO_WARNING);
            offlineNotification.setDuration((int) TimeUnit.MINUTES.toMillis(1));
            offlineNotification.open();
        }
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        UI.getCurrent().getPage().addStyleSheet("https://assets.bsdevelopment.org/font-awesome/css/all.css");
        UI.getCurrent().getPage().addStyleSheet("https://cdn.bsdevelopment.org/css/unicode-test-12.css");

        MessageConsole CONSOLE = new MessageConsole(SERVER_LOG, event.getUI());
        CONSOLE.redirectErr(Color.RED, null);
        CONSOLE.redirectOut(null);

        handleFields(event.getUI());

        AppUtilities.logMessage("[SERVER-MASTER]: Finished loading all server jars");
        AppUtilities.logMessage("[SERVER-MASTER]: Found " + App.getJarManager().getTotalServerJars() + " different types/versions to pick from");
        AppUtilities.logMessage("[SERVER-MASTER]: ");
        AppUtilities.logMessage("[SERVER-MASTER]: *** Type '??' to get help with the console ***");
        AppUtilities.logMessage("[SERVER-MASTER]: ");

        addAttachListener(event1 -> {
            App.showApplication();
        });

        // TODO: This might have to be looked into...
        addDetachListener(event1 -> {
            event1.getUI().getPage().reload();

            AppUtilities.logMessage("[WARNING]", "Application encountered an error", "Please re-open the application");
        });
    }

    private void handleDialogs() {
        ViewHandler.UPDATE_DIALOG = new UpdateDialog();
        ViewHandler.APP_SETTINGS = new AppSettingsDialog(this, apiService);
        ViewHandler.INSTALLER = new InstallerDialog(this);
        ViewHandler.JAVA_VERSION = new JavaVersionDialog(this);
        getContent().add(ViewHandler.APP_SETTINGS, ViewHandler.UPDATE_DIALOG, ViewHandler.INSTALLER, ViewHandler.JAVA_VERSION);
    }

    public ApiService getApiService() {
        return apiService;
    }

    public void updateServerPath(String path) {
        App.getJarManager().updateRepo(((path == null) || path.isEmpty()) ? null : new File(path));

        SERVER_LOG.getLayout().removeAll();

        if (!AppConfig.serverPath.equals(path)) {
            AppConfig.serverPath = path;
            AppConfig.serverType = "";
            AppConfig.serverVersion = "";
            AppConfig.serverBuild = "";
            App.saveConfig();
        }

        AppUtilities.logMessage("[SERVER-MASTER]: Finished loading all server jars");
        AppUtilities.logMessage("[SERVER-MASTER]: Found " + App.getJarManager().getTotalServerJars() + " different types/versions to pick from");
        AppUtilities.logMessage("[SERVER-MASTER]: ");
        AppUtilities.logMessage("[SERVER-MASTER]: *** Type '??' to get help with the console ***");
        AppUtilities.logMessage("[SERVER-MASTER]: ");

        handleFieldUpdates();
    }
}
