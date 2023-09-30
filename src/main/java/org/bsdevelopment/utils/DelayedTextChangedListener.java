package org.bsdevelopment.utils;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.util.ArrayList;
import java.util.List;

public class DelayedTextChangedListener implements DocumentListener {
    private final Timer timer;
    private final List<ChangeListener> listeners;

    public DelayedTextChangedListener(int delay) {
        listeners = new ArrayList<>(25);
        timer = new Timer(delay, e -> fireStateChanged());
        timer.setRepeats(false);
    }

    public void addChangeListener(ChangeListener listener) {
        listeners.add(listener);
    }

    public void removeChangeListener(ChangeListener listener) {
        listeners.remove(listener);
    }

    protected void fireStateChanged() {
        if (listeners.isEmpty()) return;

        ChangeEvent evt = new ChangeEvent(this);
        for (ChangeListener listener : listeners) listener.stateChanged(evt);
    }

    @Override
    public void insertUpdate(DocumentEvent e) {
        timer.restart();
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
        timer.restart();
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
        timer.restart();
    }

}