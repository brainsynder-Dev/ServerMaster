package org.bsdevelopment.servermaster.views.serverconsole;

import com.jeff_media.javafinder.JavaInstallation;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import org.bsdevelopment.servermaster.utils.AppUtilities;
import org.bsdevelopment.servermaster.views.ViewHandler;

import java.util.LinkedList;
import java.util.Optional;

public class JavaVersionDialog extends Dialog {
    private final ServerConsoleView consoleView;


    public JavaVersionDialog(ServerConsoleView consoleView) {
        setHeaderTitle("Select a Java Version");

        this.consoleView = consoleView;


        LinkedList<JavaInstallation> javaDataList = new LinkedList<>();
        for (JavaInstallation installation : ViewHandler.JAVA_MANAGER.getInstallations()) {
            javaDataList.addLast(installation);
//            javaDataList.addLast(new JavaData(
//                    installation.isCurrentJavaVersion(),
//                    installation.getType() + " " + installation.getVersion().getMajor(),
//                    installation.getJavaExecutable().getAbsolutePath()
//            ));
        }

        Button installButton = new Button("Select");
        installButton.setEnabled(false);
        installButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        Grid<JavaInstallation> grid = new Grid<>(JavaInstallation.class, false);
        grid.addColumn(JavaInstallation::isCurrentJavaVersion)
                .setHeader(AppUtilities.createInfoHeader("Primary", "Current java version used by your PC"))
                .setWidth("5rem");
        grid.addColumn(installation -> installation.getType() + " " + installation.getVersion().getMajor())
                .setHeader("Version")
                .setWidth("5rem");
        grid.addColumn(javaInstallation -> javaInstallation.getJavaExecutable().getAbsolutePath()).setHeader("Path").setAutoWidth(true);
        grid.setSelectionMode(Grid.SelectionMode.SINGLE);
        grid.setItems(javaDataList);
        grid.addSelectionListener(event -> {
            if (event.getFirstSelectedItem().isEmpty()) {
                if (installButton.isEnabled()) installButton.setEnabled(false);
            }else{
                if (!installButton.isEnabled()) installButton.setEnabled(true);
            }
        });

        installButton.addClickListener(event -> {
            if (!installButton.isEnabled()) return;
            Optional<JavaInstallation> optional = grid.getSelectedItems().stream().findFirst();
            optional.ifPresent(installation -> {
                ViewHandler.JAVA_MANAGER.setSelectedInstallation(installation);
                ViewHandler.APP_SETTINGS.getJavaPath().setValue(installation.getJavaExecutable().getAbsolutePath());
            });
        });

        VerticalLayout dialogLayout = new VerticalLayout();
        dialogLayout.setPadding(true);
        dialogLayout.setSpacing(false);
        dialogLayout.setAlignItems(FlexComponent.Alignment.STRETCH);
        dialogLayout.getStyle().set("width", "50rem").set("max-width", "100%");

        {
            dialogLayout.add(grid);
        }

        Button closeButton = new Button("Close", (e) -> close());
        closeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);
        closeButton.getStyle().set("margin-right", "auto");


        add(dialogLayout);
        getFooter().add(closeButton, installButton);
    }
}
