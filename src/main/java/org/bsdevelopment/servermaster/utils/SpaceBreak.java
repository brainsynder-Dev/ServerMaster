package org.bsdevelopment.servermaster.utils;

import com.vaadin.flow.component.HtmlComponent;
import com.vaadin.flow.component.Tag;

@Tag("span")
public class SpaceBreak extends HtmlComponent {
    public SpaceBreak () {
        this ("5px");
    }

    public SpaceBreak (String padding) {
        getStyle().set("padding-left", padding);
    }
}