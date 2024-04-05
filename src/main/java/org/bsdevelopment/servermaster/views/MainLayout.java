package org.bsdevelopment.servermaster.views;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.FontIcon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.shared.Tooltip;
import com.vaadin.flow.dom.ThemeList;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.theme.lumo.LumoUtility;
import com.vaadin.flow.theme.lumo.LumoUtility.*;
import org.bsdevelopment.servermaster.App;
import org.bsdevelopment.servermaster.AppConfig;
import org.bsdevelopment.servermaster.server.ServerWrapper;
import org.bsdevelopment.servermaster.swing.DevToolsDialog;
import org.bsdevelopment.servermaster.utils.AppUtilities;
import org.bsdevelopment.servermaster.utils.records.UpdateInfo;

import java.util.function.Consumer;

/**
 * The main view is a top-level placeholder for other views.
 */
public class MainLayout extends AppLayout {

    /**
     * A simple navigation item component, based on ListItem element.
     */
    public static class MenuItemInfo extends ListItem {

        private Class<? extends Component> view;

        public MenuItemInfo(String menuTitle, Component icon) {
            Span link = new Span();
            // Use Lumo classnames for various styling
            link.addClassNames(Display.FLEX, Gap.XSMALL, Height.MEDIUM, AlignItems.CENTER, Padding.Horizontal.SMALL,
                    TextColor.BODY);

            Span text = new Span(menuTitle);
            // Use Lumo classnames for various styling
            text.addClassNames(FontWeight.MEDIUM, FontSize.MEDIUM, Whitespace.NOWRAP);

            if (icon != null) {
                link.add(icon);
            }
            link.add(text);
            add(link);
        }

        public MenuItemInfo(String menuTitle, Component icon, Consumer<ClickEvent> consumer) {
            Span link = new Span();
            // Use Lumo classnames for various styling
            link.addClassNames(Display.FLEX, Gap.XSMALL, Height.MEDIUM, AlignItems.CENTER, Padding.Horizontal.SMALL,
                    TextColor.BODY, "underline");
            link.getStyle().set("cursor", "pointer");

            Span text = new Span(menuTitle);
            // Use Lumo classnames for various styling
            text.addClassNames(FontWeight.MEDIUM, FontSize.MEDIUM, Whitespace.NOWRAP);

            if (icon != null) {
                link.add(icon);
            }
            link.add(text);
            add(link);

            link.addClickListener(consumer::accept);
        }

        public MenuItemInfo(String menuTitle, Component icon, Class<? extends Component> view) {
            this.view = view;
            RouterLink link = new RouterLink();
            // Use Lumo classnames for various styling
            link.addClassNames(Display.FLEX, Gap.XSMALL, Height.MEDIUM, AlignItems.CENTER, Padding.Horizontal.SMALL,
                    TextColor.BODY);
            link.setRoute(view);

            Span text = new Span(menuTitle);
            // Use Lumo classnames for various styling
            text.addClassNames(FontWeight.MEDIUM, FontSize.MEDIUM, Whitespace.NOWRAP);

            if (icon != null) {
                link.add(icon);
            }
            link.add(text);
            add(link);
        }

        public Class<?> getView() {
            return view;
        }
    }

    public MainLayout() {
        addToNavbar(createHeaderContent());
        setDrawerOpened(false);

        addClassNames("background");
        // getStyle().set("background-color", ViewColors.BACKGROUND());
    }

    private Component createHeaderContent() {
        UI ui = UI.getCurrent();
        // ui.getPage().addStyleSheet("https://cdn.bsdevelopment.org/css/servermaster-7.css");

        HorizontalLayout header = new HorizontalLayout();
        header.addClassNames("background-color", "text-color");
        header.setWidthFull();
        header.setWidth("100%");
        header.addClassNames(BoxSizing.BORDER, LumoUtility.Gap.XSMALL, LumoUtility.Padding.XSMALL);
        header.setHeight("min-content");
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
        AppUtilities.updateTheme();

        Div layout = new Div();
        layout.addClassNames(Display.FLEX, AlignItems.CENTER, Padding.Horizontal.LARGE);

        Nav nav = new Nav();
        nav.getStyle().set("background-image", "none").set("background-color", "unset");
        nav.addClassNames(Display.FLEX, Overflow.AUTO, Padding.Horizontal.MEDIUM, Padding.Vertical.XSMALL);

        // Wrap the links in a list; improves accessibility
        UnorderedList list = new UnorderedList();
        list.addClassNames(Display.FLEX, Gap.SMALL, ListStyleType.NONE, Margin.NONE, Padding.NONE);
        nav.add(list);

        for (MenuItemInfo menuItem : createMenuItems()) {
            list.add(menuItem);
        }
        header.add(layout, nav);

        HorizontalLayout layout1 = new HorizontalLayout();
        header.addClassNames(LumoUtility.Gap.MEDIUM, LumoUtility.Padding.XSMALL);
        layout1.setAlignItems(FlexComponent.Alignment.CENTER);
        layout1.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        layout1.getStyle().set("flex-grow", "1").set("padding-right", "1rem");

        {
            ViewHandler.DEV_MODE.add(VaadinIcon.TERMINAL.create());
            ViewHandler.DEV_MODE.addClassName("pointer");
            ViewHandler.DEV_MODE.getStyle().set("border", "solid #121A24 1px").set("padding", "4px");
            ViewHandler.DEV_MODE.addClickListener(spanClickEvent -> {
                new DevToolsDialog("DevTools", App.siteDisplay.browser).setVisible(true);
            });
            Tooltip.forComponent(ViewHandler.DEV_MODE)
                    .withText("Open DevTools Window")
                    .withPosition(Tooltip.TooltipPosition.END_BOTTOM);
            layout1.add(ViewHandler.DEV_MODE);

            ViewHandler.DEV_MODE.setVisible(AppConfig.devMode);
        }

        {
            Span scrollToggle = new Span();
            scrollToggle.addClassName("pointer");
            if (AppConfig.auto_scroll) {
                scrollToggle.add(VaadinIcon.ANGLE_DOUBLE_DOWN.create());
            } else{
                scrollToggle.add(VaadinIcon.LINE_H.create());
            }
            scrollToggle.getStyle().set("border", "solid #121A24 1px").set("padding", "4px");
            scrollToggle.addClickListener(spanClickEvent -> {
                AppConfig.auto_scroll = !AppConfig.auto_scroll;
                scrollToggle.removeAll();

                if (AppConfig.auto_scroll) {
                    scrollToggle.add(VaadinIcon.ANGLE_DOUBLE_DOWN.create());
                }else{
                    scrollToggle.add(VaadinIcon.LINE_H.create());
                }
                App.saveConfig();
            });
            Tooltip.forComponent(scrollToggle)
                    .withText("Toggle console auto-scrolling")
                    .withPosition(Tooltip.TooltipPosition.END_BOTTOM);
            layout1.add(scrollToggle);
        }

        {
            Span themeChange = new Span();
            themeChange.addClassName("pointer");
            themeChange.add(new FontIcon("fa-solid", "fa-palette"));
            themeChange.getStyle().set("border", "solid #121A24 1px").set("padding", "4px");
            themeChange.addClickListener(spanClickEvent -> {
                AppConfig.lightTheme = !AppConfig.lightTheme;

                ThemeList themeList = ui.getElement().getThemeList();
                themeList.clear();
                themeList.add(AppConfig.lightTheme ? "light" : "dark");
                App.saveConfig();
            });


            Tooltip.forComponent(themeChange)
                    .withText("Toggle Light/Dark Mode")
                    .withPosition(Tooltip.TooltipPosition.END_BOTTOM);
            layout1.add(themeChange);
        }

        {
            Span installer = new Span();
            installer.addClassName("pointer");
            installer.add(VaadinIcon.CLOUD_DOWNLOAD_O.create());
            installer.getStyle().set("border", "solid #121A24 1px").set("padding", "4px");
            installer.addClickListener(spanClickEvent -> {
                if (ServerWrapper.getInstance().isServerRunning()) {
                    AppUtilities.sendNotification(
                            "Unable to access server installer while server is running",
                            NotificationVariant.LUMO_ERROR
                    );
                    return;
                }
                if (ViewHandler.INSTALLER == null) return;
                ViewHandler.INSTALLER.open();
            });


            Tooltip.forComponent(installer)
                    .withText("Install Server Jars")
                    .withPosition(Tooltip.TooltipPosition.END_BOTTOM);
            layout1.add(installer);
        }

        {
            Span appSettings = new Span();
            appSettings.addClassName("pointer");
            appSettings.add(VaadinIcon.COG_O.create());
            appSettings.addClickListener(spanClickEvent -> {
                if (ServerWrapper.getInstance().isServerRunning()) {
                    AppUtilities.sendNotification(
                            "Unable to access settings while server is running",
                            NotificationVariant.LUMO_ERROR
                    );
                    return;
                }
                if (ViewHandler.APP_SETTINGS == null) return;
                ViewHandler.APP_SETTINGS.open();
            });


            Tooltip.forComponent(appSettings)
                    .withText("Configure App Settings")
                    .withPosition(Tooltip.TooltipPosition.END_BOTTOM);

            if (App.appVersion != null) {
                UpdateInfo info = App.startupUpdateInfo;

                int compare = App.appVersion.compareTo(info.version());

                if (compare == -1) {
                    Span counter = new Span();
                    counter.getStyle().set("position", "absolute")
                            .set("top", "0")
                            .set("right", "0")
                            .set("border-radius", "90px")
                            .set("color", "var(--lumo-text-color)")
                            .set("background-color", "var(--lumo-error-color)")
                            .set("font-size", "13px")
                            .set("padding", "5px")
                            .set("border", "2px var(--lumo-success-color) solid");

                    String counterLabel = "NEW UPDATE AVAILABLE";
                    counter.getElement().setAttribute("aria-label", counterLabel);
                    counter.getElement().setAttribute("title", counterLabel);

                    Div div = new Div(appSettings, counter);
                    div.getStyle().set("border", "solid #121A24 1px").set("padding", "4px").set("display", "inline-block").set("position", "relative");
                    layout1.add(div);
                }else{
                    appSettings.getStyle().set("border", "solid #121A24 1px").set("padding", "4px");
                    layout1.add(appSettings);
                }
            }else{
                appSettings.getStyle().set("border", "solid #121A24 1px").set("padding", "4px");
                layout1.add(appSettings);
            }
        }

        header.add(layout1);
        return header;
    }

    private MenuItemInfo[] createMenuItems() {
        return new MenuItemInfo[]{
                new MenuItemInfo("Server Console", VaadinIcon.HARDDRIVE.create())
        };
    }

}
