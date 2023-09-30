package org.bsdevelopment.utils;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

public abstract class MousePressedListener implements MouseListener {
    public abstract void onMousePressed(MouseEvent event);

    @Override
    public void mousePressed(MouseEvent e) {
        onMousePressed(e);
    }

    @Override
    public void mouseClicked(MouseEvent e) {

    }

    @Override
    public void mouseReleased(MouseEvent e) {

    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }
}
