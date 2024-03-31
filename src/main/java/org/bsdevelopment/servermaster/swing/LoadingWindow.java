package org.bsdevelopment.servermaster.swing;

import me.friwi.jcefmaven.EnumProgress;
import me.friwi.jcefmaven.IProgressHandler;
import me.friwi.jcefmaven.impl.progress.ConsoleProgressHandler;
import org.apache.commons.lang3.tuple.Pair;
import org.bsdevelopment.servermaster.App;
import org.bsdevelopment.servermaster.AppConfig;
import org.bsdevelopment.servermaster.utils.AppUtilities;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LoadingWindow extends JFrame {
    private final JPanel container = new JPanel();
    private JLabel icon = new JLabel();
    private final JLabel appName = new JLabel(App.name);
    private final JLabel txtStatus = new JLabel("Loading ...");
    private final JProgressBar progressBar = new JProgressBar();

    public LoadingWindow() {
        super(App.name);
        setVisible(false);
        setUndecorated(true);

        setSize(300, 120);
        Swing.center(this);
        Swing.roundCorners(this, 30, 30);

        try {
            Image icon = App.getIcon();
            setIconImage(icon);
            this.icon = Swing.image(icon, 30, 30);
        } catch (Exception ignored) {}
        progressBar.setPreferredSize(new Dimension(146, 10));
        progressBar.setIndeterminate(true);

        {
            Pair<Color, Color> theme = getTheme();
            container.setBackground(theme.getLeft());
            getContentPane().setBackground(theme.getLeft());
            txtStatus.setForeground(theme.getRight());
            appName.setForeground(theme.getRight());
        }

        GroupLayout panel1Layout = new GroupLayout(container);
        container.setLayout(panel1Layout);
        panel1Layout.setHorizontalGroup(
                panel1Layout.createParallelGroup()
                        .addGroup(panel1Layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(panel1Layout.createParallelGroup()
                                        .addGroup(panel1Layout.createSequentialGroup()
                                                .addComponent(icon, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(appName, GroupLayout.PREFERRED_SIZE, 99, GroupLayout.PREFERRED_SIZE)
                                                .addGap(0, 153, Short.MAX_VALUE))
                                        .addComponent(progressBar, GroupLayout.DEFAULT_SIZE, 288, Short.MAX_VALUE)
                                        .addComponent(txtStatus, GroupLayout.DEFAULT_SIZE, 288, Short.MAX_VALUE))
                                .addContainerGap())
        );
        panel1Layout.setVerticalGroup(
                panel1Layout.createParallelGroup()
                        .addGroup(panel1Layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(panel1Layout.createParallelGroup()
                                        .addComponent(icon, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(appName, GroupLayout.DEFAULT_SIZE, 32, Short.MAX_VALUE))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(txtStatus)
                                .addGap(25, 25, 25)
                                .addComponent(progressBar, GroupLayout.PREFERRED_SIZE, 12, GroupLayout.PREFERRED_SIZE)
                                .addContainerGap())
        );

        setContentPane(container);
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
