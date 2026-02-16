package org.bsdevelopment.servermaster.utils;

import javafx.scene.Node;
import javafx.scene.layout.AnchorPane;

public final class AnchorUtil {
    private AnchorUtil() {}

    public static <T extends Node> T setAnchors(T node,
                                                Double top,
                                                Double right,
                                                Double bottom,
                                                Double left) {
        if (top    != null) AnchorPane.setTopAnchor(node,    top);
        if (right  != null) AnchorPane.setRightAnchor(node,  right);
        if (bottom != null) AnchorPane.setBottomAnchor(node, bottom);
        if (left   != null) AnchorPane.setLeftAnchor(node,   left);
        return node;
    }
}
