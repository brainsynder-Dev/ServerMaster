package org.bsdevelopment.servermaster.swing;

import me.friwi.jcefmaven.CefAppBuilder;
import me.friwi.jcefmaven.MavenCefAppHandlerAdapter;
import org.bsdevelopment.servermaster.App;
import org.cef.CefApp;
import org.cef.CefClient;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefMessageRouter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Objects;

/**
 *
 */
public class Window extends JFrame {
    public final CefBrowser browser;
    public final Component browserUI;
    private final CefApp cefApp;
    private final CefClient client;

    public Window(LoadingWindow loadingWindow) throws HeadlessException {
        this("http://localhost:" + App.sitePort, loadingWindow);
    }

    public Window(String startURL, LoadingWindow loadingWindow) {
        this(startURL, false, loadingWindow);
    }

    public Window(String startURL, boolean isTransparent, LoadingWindow loadingWindow) {
        this(startURL, isTransparent, 70, 60, loadingWindow);
    }

    public Window(String startURL, boolean isTransparent, int widthPercent, int heightPercent, LoadingWindow loadingWindow) {
        try {
            UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        SwingUtilities.updateComponentTreeUI(this);
        pack();

        try {
            CefAppBuilder builder = new CefAppBuilder();
            builder.setProgressHandler(loadingWindow.getProgressHandler());
            builder.getCefSettings().windowless_rendering_enabled = false;

            builder.setAppHandler(new MavenCefAppHandlerAdapter() {
                @Override
                public void stateHasChanged(org.cef.CefApp.CefAppState state) {
                    if (state == CefApp.CefAppState.TERMINATED) System.exit(0);
                }
            });
            cefApp = builder.build();
            client = cefApp.createClient();

            CefMessageRouter msgRouter = CefMessageRouter.create();
            client.addMessageRouter(msgRouter);
            browser = client.createBrowser(startURL, false, isTransparent);
            browserUI = browser.getUIComponent();

            getContentPane().add(browserUI, BorderLayout.CENTER);
            if (widthPercent <= 0 || heightPercent <= 0) {
                widthPercent = 100;
                heightPercent = 100;
            }
            width(widthPercent);
            height(heightPercent);
            setIconImage(App.getIcon());
            setTitle(App.name);
            Swing.center(this);
            revalidate();

            addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    CefApp.getInstance().dispose();
                    dispose();
                    App.LOADING_WINDOW.dispose();
                }
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @see #width(int)
     */
    public void widthFull() {
        width(100);
    }

    /**
     * This invalidates the container and thus to see changes in the UI
     * make sure execute {@link Component#revalidate()} manually.
     *
     * @param widthPercent 0 to 100% of the parent size (screen if null).
     */
    public void width(int widthPercent) {
        Objects.requireNonNull(this);
        updateWidth(this.getParent(), this, widthPercent);
    }

    /**
     * @see #height(int)
     */
    public void heightFull() {
        height(100);
    }

    /**
     * This invalidates the container and thus to see changes in the UI
     * make sure execute {@link Component#revalidate()} manually.
     *
     * @param heightPercent 0 to 100% of the parent size (screen if null).
     */
    public void height(int heightPercent) {
        updateHeight(this.getParent(), this, heightPercent);
    }

    private void updateWidth(Component parent, Component target, int widthPercent) {
        int parentWidth; // If no parent provided use the screen dimensions
        if (parent != null) parentWidth = parent.getWidth();
        else parentWidth = Toolkit.getDefaultToolkit().getScreenSize().width;
        Dimension size = new Dimension(parentWidth / 100 * widthPercent, target.getHeight());
        target.setSize(size);
        target.setPreferredSize(size);
        target.setMaximumSize(size);
    }

    private void updateHeight(Component parent, Component target, int heightPercent) {
        int parentHeight; // If no parent provided use the screen dimensions
        if (parent != null) parentHeight = parent.getHeight();
        else parentHeight = Toolkit.getDefaultToolkit().getScreenSize().height;
        Dimension size = new Dimension(target.getWidth(), parentHeight / 100 * heightPercent);
        target.setSize(size);
        target.setPreferredSize(size);
        target.setMaximumSize(size);
    }
}
