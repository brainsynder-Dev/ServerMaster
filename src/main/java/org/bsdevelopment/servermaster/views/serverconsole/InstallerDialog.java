package org.bsdevelopment.servermaster.views.serverconsole;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.select.Select;
import org.apache.commons.lang3.tuple.Pair;
import org.bsdevelopment.servermaster.App;
import org.bsdevelopment.servermaster.AppConfig;
import org.bsdevelopment.servermaster.utils.AdvString;
import org.bsdevelopment.servermaster.utils.AppUtilities;
import org.bsdevelopment.servermaster.views.data.ServerTypeBuild;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;

public class InstallerDialog extends Dialog {
    private final ServerConsoleView consoleView;

    private final Button installButton;
    private Select<String> serverType;
    private Select<String> versions;
    private Select<ServerTypeBuild> buildSelect;

    public InstallerDialog(ServerConsoleView consoleView) {
        setHeaderTitle("Server Installer");

        this.consoleView = consoleView;
        installButton = new Button("Install");
        installButton.setEnabled(false);
        installButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);



        VerticalLayout dialogLayout = createDialogLayout();

        Button closeButton = new Button("Close", (e) -> close());
        closeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);
        closeButton.getStyle().set("margin-right", "auto");


        add(dialogLayout);
        getFooter().add(closeButton, installButton);
    }

    private VerticalLayout createDialogLayout() {
        VerticalLayout dialogLayout = new VerticalLayout();
        dialogLayout.setPadding(true);
        dialogLayout.setSpacing(false);
        dialogLayout.setAlignItems(FlexComponent.Alignment.STRETCH);
        dialogLayout.getStyle().set("width", "30rem").set("max-width", "100%");

        Div container = new Div();
        // Server Location handle
        {
            serverType = new Select<>();
            serverType.setLabel("Server Type");
            serverType.setItems("paper", "purpur", "pufferfish");
            serverType.setPlaceholder("Select Server Type");
            serverType.addValueChangeListener(event -> {
                container.removeAll();
                String type = event.getValue();

                HorizontalLayout horizontalLayout = new HorizontalLayout();
                container.add(horizontalLayout);

                versions = new Select<>();
                versions.setLabel("Version");
                versions.setPlaceholder("Select Version");
                horizontalLayout.add(versions);

                buildSelect = new Select<>();
                buildSelect.setLabel("Build");
                buildSelect.setPlaceholder("Select Build");
                horizontalLayout.add(buildSelect);

                UI ui = UI.getCurrent();
                consoleView.getApiService().getVersions(type).whenComplete((strings, throwable) -> {
                    ui.access(() -> {
                        versions.setItems(strings);
                    });
                });

                versions.addValueChangeListener(event1 -> {
                    buildSelect.clear();
                    buildSelect.setItems(new ArrayList<>());

                    consoleView.getApiService().getBuilds(type, event1.getValue()).whenComplete((builds, throwable) -> {
                        ui.access(() -> {
                            buildSelect.setItems(builds);
                        });
                    });
                });

                buildSelect.addValueChangeListener(event1 -> {
                    installButton.setEnabled(true);
                });
            });

            dialogLayout.add(serverType, container);
        }

        {
            ProgressBar progress = new ProgressBar(0, 100, 10);

            Label progressBarLabelText = new Label("Installing...");
            progressBarLabelText.setId("pblabel");
            progress.getElement().setAttribute("aria-labelledby", "pblabel");

            HorizontalLayout progressBarLabel = new HorizontalLayout(progressBarLabelText);
            progressBarLabel.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

            Div progressContainer = new Div(progressBarLabel, progress);
            progressContainer.setVisible(false);
            dialogLayout.add(progressContainer);

            // progress.getValue()
            installButton.addClickListener(event1 -> {
                progress.setValue(0);
                String type = serverType.getValue();
                String version = versions.getValue();
                ServerTypeBuild build = buildSelect.getValue();


                File folder = App.getJarManager().getRepo();
                if (folder == null) {
                    Notification.show("No server location was set.");
                    return;
                }
                progressContainer.setVisible(true);

                UI ui = UI.getCurrent();
                String url = build.jar();
                if (type.equalsIgnoreCase("pufferfish")) {
                    consoleView.getApiService().getPufferfishUrl(version, build.build()).whenComplete((s, throwable1) -> {
                        consoleView.getApiService().getConnection(s).whenComplete((pair, throwable) -> {
                            handleDownload(ui, progress, progressContainer, folder, type, AdvString.between("pufferfish-paperclip-", "-R0", s), build.build(), pair.getLeft());
                        });
                    });
                }else{
                    consoleView.getApiService().getConnection(url).whenComplete((pair, throwable) -> {
                        handleDownload(ui, progress, progressContainer, folder, type, version, build.build(), pair.getLeft());
                    });
                }
            });
        }

        return dialogLayout;
    }

    private void handleDownload (UI ui, ProgressBar progress, Div progressContainer, File folder, String type, String version, String build, HttpURLConnection connection) {
        try {
            File file = new File(folder, type+"-"+version+"-"+build+".jar");

            String fileName = file.getName();

            AppUtilities.downloadFile(connection, file, integer -> ui.access(() -> progress.setValue(integer)), () -> {
                ui.access(() -> {
                    installButton.setEnabled(false);
                    progressContainer.setVisible(false);
                    progress.setValue(0);

                    consoleView.updateServerPath(AppConfig.serverPath);
                    Notification.show("Install has been completed for '"+fileName+"'");
                });
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    /*

                        if (type.equalsIgnoreCase("pufferfish")) {
                            System.out.println("Pufferfish download...");
                            System.out.println("ContentType: "+connection.getHeaderField("Content-Type"));
                            System.out.println("Content-Location: "+connection.getHeaderField("Content-Location"));
                            System.out.println("filename: "+connection.getHeaderField("filename"));
                            String fieldValue = connection.getHeaderField("Content-Disposition");
                            System.out.println("Fields: "+fieldValue);
                            if (fieldValue != null && fieldValue.contains("filename=\"")) {
                                String filename = fieldValue.substring(fieldValue.indexOf("filename=\"") + 10, fieldValue.length() - 2);
                                System.out.println("Filename: "+filename);
                                String version1 = AdvString.between("-1.", "-R", filename);
                                file = new File(folder, type+"-1."+version1+"-"+build.build()+".jar");
                            }
                        }
     */

    private void updateServerPath (String path) {
        App.getJarManager().updateRepo(((path == null) || path.isEmpty()) ? null : new File(path));

        consoleView.SERVER_LOG.getLayout().removeAll();

        AppConfig.serverPath = path;
        AppConfig.serverType = "";
        AppConfig.serverVersion = "";
        AppConfig.serverBuild = "";
        App.saveConfig();

        AppUtilities.logMessage("[SERVER-MASTER]: Finished loading all server jars");
        AppUtilities.logMessage("[SERVER-MASTER]: Found "+App.getJarManager().getTotalServerJars()+" different types/versions to pick from");
        AppUtilities.logMessage("[SERVER-MASTER]: ");
        AppUtilities.logMessage("[SERVER-MASTER]: *** Type '??' to get help with the console ***");
        AppUtilities.logMessage("[SERVER-MASTER]: ");

        consoleView.handleFieldUpdates();
    }

    private JFileChooser getFolderChooser() {
        JFileChooser fileChooser = new JFileChooser();
        if ((AppConfig.serverPath != null) && (!AppConfig.serverPath.isEmpty()))
            fileChooser.setCurrentDirectory(new File(AppConfig.serverPath));
        fileChooser.setDialogTitle("Select Server Location");

        Pair<Color, Color> theme = App.LOADING_WINDOW.getTheme();
        fileChooser.setBackground(theme.getLeft());
        fileChooser.setForeground(theme.getRight());

        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        return fileChooser;
    }
}
