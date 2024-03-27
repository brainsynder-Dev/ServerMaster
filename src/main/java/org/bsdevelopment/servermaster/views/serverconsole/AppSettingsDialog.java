package org.bsdevelopment.servermaster.views.serverconsole;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import org.apache.commons.lang3.tuple.Pair;
import org.bsdevelopment.servermaster.App;
import org.bsdevelopment.servermaster.AppConfig;
import org.bsdevelopment.servermaster.swing.WindowUtils;
import org.bsdevelopment.servermaster.views.ViewHandler;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class AppSettingsDialog extends Dialog {
    private final ServerConsoleView consoleView;
    private TextField javaPath;

    public AppSettingsDialog (ServerConsoleView consoleView) {
        this.consoleView = consoleView;
        setHeaderTitle("App Settings");


        VerticalLayout dialogLayout = createDialogLayout();

        Button closeButton = new Button("Close", (e) -> close());
        closeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);
        closeButton.getStyle().set("margin-right", "auto");

        add(dialogLayout);
        getFooter().add(closeButton);
    }

    private VerticalLayout createDialogLayout() {
        VerticalLayout dialogLayout = new VerticalLayout();
        dialogLayout.setPadding(true);
        dialogLayout.setSpacing(false);
        dialogLayout.setAlignItems(FlexComponent.Alignment.STRETCH);
        dialogLayout.getStyle().set("width", "30rem").set("max-width", "100%");

        // Server Location handle
        {
            TextField serverFolder = new TextField("Server Folder Location");
            serverFolder.setValueChangeMode(ValueChangeMode.TIMEOUT);
            if ((AppConfig.serverPath != null) && (!AppConfig.serverPath.isEmpty()))
                serverFolder.setValue(AppConfig.serverPath);
            serverFolder.addValueChangeListener(event -> {
                consoleView.updateServerPath(event.getValue());
            });

            Icon icon = VaadinIcon.FOLDER_OPEN.create();
            icon.addClassName("underline");
            icon.getStyle().set("color", "var(--lumo-success-color)");
            icon.addClickListener(event -> {
                JFileChooser fileChooser = getFolderChooser();
                if (fileChooser.showSaveDialog(App.siteDisplay) == JFileChooser.APPROVE_OPTION) {
                    File file = fileChooser.getSelectedFile();
                    serverFolder.setValue(file.getAbsolutePath());
                }
            });

            serverFolder.setSuffixComponent(icon);

            dialogLayout.add(serverFolder);
        }

        // Dev mode toggle
        {
            Checkbox devMode = new Checkbox("App Developer Mode");
            devMode.setValue(AppConfig.devMode);
            devMode.setTooltipText("Access to the developer tools window");
            devMode.addValueChangeListener(event -> {
                ViewHandler.DEV_MODE.setVisible(event.getValue());
                AppConfig.devMode = event.getValue();
                App.saveConfig();
            });
            dialogLayout.add(devMode);
        }

        // Java Settings
        {
            javaPath = new TextField("Java Executable");
            javaPath.setValue(ViewHandler.JAVA_MANAGER.getPrimaryInstallation().getJavaExecutable().getAbsolutePath());

            Button detect = new Button("Detect Java");
            detect.addClickListener(event -> ViewHandler.JAVA_VERSION.open());

            HorizontalLayout layout = new HorizontalLayout();
            layout.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
            layout.add(javaPath, detect);
            layout.expand(javaPath);
            layout.setPadding(false);

            dialogLayout.add(layout);
        }

        return dialogLayout;
    }

    public TextField getJavaPath() {
        return javaPath;
    }

    private JFileChooser getFolderChooser() {
        JFileChooser fileChooser = new JFileChooser();
        if ((AppConfig.serverPath != null) && (!AppConfig.serverPath.isEmpty()))
            fileChooser.setCurrentDirectory(new File(AppConfig.serverPath));
        fileChooser.setDialogTitle("Select Server Location");

        Pair<Color, Color> theme = WindowUtils.LOADING_WINDOW.getTheme();
        fileChooser.setBackground(theme.getLeft());
        fileChooser.setForeground(theme.getRight());

        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        return fileChooser;
    }
}
