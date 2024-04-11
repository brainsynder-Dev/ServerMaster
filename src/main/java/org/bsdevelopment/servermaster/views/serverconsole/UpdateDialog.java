package org.bsdevelopment.servermaster.views.serverconsole;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.icon.FontIcon;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.open.Open;
import org.bsdevelopment.servermaster.App;
import org.bsdevelopment.servermaster.utils.AppUtilities;
import org.bsdevelopment.servermaster.utils.markdown.MarkdownArea;
import org.bsdevelopment.servermaster.utils.records.UpdateInfo;

public class UpdateDialog extends Dialog {
    private final Div container;

    public UpdateDialog() {
        // setHeaderTitle("Update Available");
        container = new Div();

        VerticalLayout dialogLayout = createDialogLayout();
        setClassName("changelog");
        add(dialogLayout);
    }

    private VerticalLayout createDialogLayout() {
        VerticalLayout dialogLayout = new VerticalLayout();
        dialogLayout.setPadding(true);
        dialogLayout.setSpacing(false);
        dialogLayout.setAlignItems(FlexComponent.Alignment.STRETCH);
        dialogLayout.getStyle()
                .set("width", "50rem").set("max-width", "100%")
                .set("height", "50rem").set("max-height", "100%").set("overflow", "hidden");

        container.setWidth("100%");
        container.getStyle().set("position", "relative").set("display", "contents");
        dialogLayout.add(container);

        return dialogLayout;
    }

    public void populate (UpdateInfo updateInfo) {
        if ((updateInfo == null) || (App.appVersion == null)) {
            AppUtilities.sendNotification("Unable to find update information", NotificationVariant.LUMO_ERROR);
            return;
        }
        int compare = App.appVersion.compareTo(updateInfo.version());

        container.removeAll();

        if (compare > -1) {
            AppUtilities.sendNotification("Application is up-to-date", NotificationVariant.LUMO_CONTRAST);
            return;
        }
        HorizontalLayout top = new HorizontalLayout();
        top.getStyle().set("padding-bottom", "20px").set("text-decoration", "underline");
        top.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        top.setAlignItems(FlexComponent.Alignment.CENTER);

        top.add(AppUtilities.modifyComponent(new H2(updateInfo.title()), component -> component.getStyle().set("color", "var(--lumo-success-color)")));
        top.add(AppUtilities.modifyComponent(new H3(updateInfo.version().toString()), component -> component.getStyle().set("color", "var(--lumo-success-color)")));
        container.add(top);

        Div div = new Div();
        div.getStyle().set("height", "50rem").set("overflow", "auto");
        MarkdownArea markdown = new MarkdownArea();
        markdown.setValue(updateInfo.markdown());
        markdown.previewOnly();
        div.add(markdown);

        container.add(div);

        Button viewRelease = new Button("View Update Release", (e) -> Open.open(updateInfo.releaseUrl()));
        viewRelease.getStyle().set("position", "absolute").set("margin", "20px").set("bottom", "0").set("margin-left", "0").set("width", "46rem").set("padding", "25px");
        viewRelease.setIcon(new FontIcon("fa-brands", "fa-github"));
        container.add(viewRelease);
        open();
    }
}
