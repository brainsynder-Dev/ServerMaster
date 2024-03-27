package org.bsdevelopment.servermaster.swing;

import com.osiris.betterlayout.BLayout;
import me.friwi.jcefmaven.EnumProgress;
import me.friwi.jcefmaven.IProgressHandler;
import me.friwi.jcefmaven.impl.progress.ConsoleProgressHandler;
import org.apache.commons.lang3.tuple.Pair;
import org.bsdevelopment.servermaster.App;
import org.bsdevelopment.servermaster.AppConfig;
import org.bsdevelopment.servermaster.utils.AppUtilities;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LoadingWindow extends JFrame {
    BLayout rootLayout;
    JLabel txtStatus = (new JLabel("Loading ..."));

    public LoadingWindow() throws HeadlessException, IOException {
        super(App.name);
        WindowUtils.WINDOW_LIST.add(this);
        setIconImage(App.getIcon());
        setVisible(false);
        setUndecorated(true);
        this.rootLayout = new BLayout(this, true);
        this.setContentPane(rootLayout);

        Image icon = App.getIcon();

        JPanel title = new JPanel();// Swing.transparent(new JPanel());
        rootLayout.addH(title);
        title.add((Swing.image(icon, 30, 30)));//Swing.transparent

        JLabel appName = new JLabel(App.name);
        title.add(appName);
        title.add(txtStatus);
        rootLayout.addV(txtStatus);

        Pair<Color, Color> theme = getTheme();

        title.setBackground(theme.getLeft());
        getContentPane().setBackground(theme.getLeft());
        txtStatus.setForeground(theme.getRight());
        appName.setForeground(theme.getRight());

        setSize(300, 100);
        Swing.center(this);
        Swing.roundCorners(this, 10, 10);
    }

    public Pair<Color, Color> getTheme () {
        Color background = AppUtilities.hex2Color("#e8dcd9");
        Color text = AppUtilities.hex2Color("#261a17");

        if (!AppConfig.lightTheme) {
            background = AppUtilities.hex2Color("#261a17");
            text = AppUtilities.hex2Color("#f4edec");
        }
        return Pair.of(background, text);
    }

    public IProgressHandler getProgressHandler() {
        final Logger logger = Logger.getLogger(ConsoleProgressHandler.class.getName());
        return (state, percent) -> {
            if (state == EnumProgress.INITIALIZED) {
                dispose();
            } else {
                this.setVisible(true);
            }

            Objects.requireNonNull(state, "state cannot be null");
            if (percent == -1.0F || !(percent < 0.0F) && !(percent > 100.0F)) {
                logger.log(Level.INFO, state + " |> " + (percent == -1.0F ? "" : (int) percent + "%"));
            } else {
                throw new RuntimeException("percent has to be -1f or between 0f and 100f. Got " + percent + " instead");
            }

            if (this.isVisible()) txtStatus.setText("JCEF dependency: " + state.name().toLowerCase() + "... " + (percent == -1.0F ? "" : (int) percent + "%"));
        };
    }

    public void close() {
        setVisible(false);
        dispose();
    }
}
