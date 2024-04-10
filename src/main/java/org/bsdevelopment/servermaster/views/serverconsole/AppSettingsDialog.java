package org.bsdevelopment.servermaster.views.serverconsole;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.icon.FontIcon;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.open.Open;
import org.apache.commons.lang3.tuple.Pair;
import org.bsdevelopment.servermaster.App;
import org.bsdevelopment.servermaster.AppConfig;
import org.bsdevelopment.servermaster.utils.AppUtilities;
import org.bsdevelopment.servermaster.utils.system.SystemUtilities;
import org.bsdevelopment.servermaster.views.ViewHandler;
import org.bsdevelopment.servermaster.views.service.ApiService;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.math.BigDecimal;

public class AppSettingsDialog extends Dialog {
    private final ServerConsoleView consoleView;
    private final ApiService apiService;
    private TextField javaPath;

    public AppSettingsDialog(ServerConsoleView consoleView, ApiService apiService) {
        this.consoleView = consoleView;
        this.apiService = apiService;

        HorizontalLayout top = new HorizontalLayout();
        top.getStyle().set("padding-bottom", "10px");
        top.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        top.setAlignItems(FlexComponent.Alignment.CENTER);
        top.setWidthFull();
        top.add(new H2("App Settings"));

        Button button = new Button("Report an Issue", (e) -> Open.open("https://github.com/brainsynder-Dev/ServerMaster/issues"));
        button.setIcon(new FontIcon("fa-brands", "fa-github"));
        top.add(button);
        getHeader().add(top);


        VerticalLayout dialogLayout = createDialogLayout();

        Button closeButton = new Button("Close", (e) -> close());
        closeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);
        closeButton.getStyle().set("margin-right", "auto");

        add(dialogLayout);
        getFooter().add(closeButton);

        Button checkUpdate = new Button("Update Check");
        checkUpdate.addClickListener(clickEvent -> {
            clickEvent.getSource().getUI().ifPresent(ui -> {
                ViewHandler.UPDATE_DIALOG.populate(AppUtilities.fetchUpdateInfo());
            });
        });
        checkUpdate.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        if (!AppUtilities.OFFLINE) getFooter().add(checkUpdate);
    }

    private VerticalLayout createDialogLayout() {
        VerticalLayout dialogLayout = new VerticalLayout();
        dialogLayout.setPadding(true);
        dialogLayout.setSpacing(false);
        dialogLayout.setAlignItems(FlexComponent.Alignment.STRETCH);
        dialogLayout.getStyle().set("width", "50rem").set("max-width", "100%");

        HorizontalLayout javaLayout = new HorizontalLayout();

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

        // Server Ram
        {
            NumberField ramField = new NumberField("Server Ram");
            ramField.setHelperText("Max ram available: " + SystemUtilities.convertToStringRepresentation(App.MAX_RAM));
            ramField.addThemeVariants(TextFieldVariant.LUMO_HELPER_ABOVE_FIELD);
            ramField.setStep(512);
            ramField.setMin(512);
            ramField.setMax(App.MAX_MB_RAM);
            ramField.setValue(Double.valueOf(AppConfig.ram));
            ramField.setLabel("Server Ram");
            ramField.setStepButtonsVisible(true);
            ramField.addValueChangeListener(event -> {
                int ram = BigDecimal.valueOf(event.getValue()).intValue();
                if (ram < 512) {
                    ramField.setErrorMessage("Value can not be below 512");
                    return;
                }
                if (ram > App.MAX_MB_RAM) {
                    ramField.setErrorMessage("Value can't exceed your free memory of: " + App.MAX_MB_RAM);
                    return;
                }
                ramField.setErrorMessage("");

                AppConfig.ram = ram;
                App.saveConfig();
            });
            dialogLayout.add(ramField);
        }

        dialogLayout.add(new Hr());
        // Java Settings
        {
            NativeLabel label = new NativeLabel("Java Executable Path");
            label.getStyle()
                    .set("color", "var(--lumo-secondary-text-color)")
                    .set("font-size", "var(--lumo-font-size-s)")
                    .set("line-height", "1")
                    .set("font-weight", "500");
            dialogLayout.add(label);

            javaPath = new TextField();
            javaPath.setPlaceholder("Java Executable");
            javaPath.setValue(((AppConfig.javaPath == null) || AppConfig.javaPath.isEmpty()) ? ViewHandler.JAVA_MANAGER.getPrimaryInstallation().getJavaExecutable().getAbsolutePath() : AppConfig.javaPath);
            javaPath.addValueChangeListener(event -> {
                AppConfig.javaPath = event.getValue();
                App.saveConfig();
            });

            Button detect = new Button("Detect Java");
            detect.addClickListener(event -> ViewHandler.JAVA_VERSION.open());

            javaLayout.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
            javaLayout.add(javaPath, detect);
            javaLayout.expand(javaPath);
            javaLayout.setPadding(false);
            javaLayout.setSpacing(false);

            dialogLayout.add(javaLayout);
        }

        dialogLayout.add(new Hr());

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

        Pair<Color, Color> theme = App.LOADING_WINDOW.getTheme();
        fileChooser.setBackground(theme.getLeft());
        fileChooser.setForeground(theme.getRight());

        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        return fileChooser;
    }
}
