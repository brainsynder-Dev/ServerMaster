package org.bsdevelopment.servermaster.utils;

public enum Outline {
    DEFAULT("window-outline-default"),
    ACCENT("window-outline-accent"),
    DANGER("window-outline-danger"),
    SUCCESS("window-outline-success"),
    NONE("window-outline-none");

    private final String cssClass;

    Outline(String cssClass) {
        this.cssClass = cssClass;
    }

    public String getCssClass() {
        return cssClass;
    }
}