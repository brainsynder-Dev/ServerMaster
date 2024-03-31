package org.bsdevelopment.servermaster.views.serverconsole.console;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import org.bsdevelopment.servermaster.AppConfig;

public class ServerLog extends Scroller {
    private final VerticalLayout layout;

    public ServerLog() {
        layout = new VerticalLayout();
        layout.setSpacing(false);
        layout.setWidthFull();
        layout.getStyle().set("overflow-y", "auto");

        setScrollDirection(Scroller.ScrollDirection.VERTICAL);
        setId("scrolling_div");
        setContent(layout);
    }

    public void newMessage(Component... components){
        for (Component component : components) {
            layout.add(component);
        }

        if (AppConfig.auto_scroll) scroll();
    }

    public VerticalLayout getLayout() {
        return layout;
    }

    private void scroll() {
        getElement().executeJs("""
                var el = this;
                el.scrollTo(0, el.scrollHeight);
                """);
    }
}
