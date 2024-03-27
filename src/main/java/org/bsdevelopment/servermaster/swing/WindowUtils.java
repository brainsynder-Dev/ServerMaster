package org.bsdevelopment.servermaster.swing;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.dom.ThemeList;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.theme.lumo.Lumo;
import org.bsdevelopment.servermaster.AppConfig;
import org.bsdevelopment.servermaster.utils.AdvString;
import org.bsdevelopment.servermaster.utils.ImageUtils;
import org.bsdevelopment.servermaster.utils.RenderAPI;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class WindowUtils {
    public static final LoadingWindow LOADING_WINDOW;
    public static final List<LoadingWindow> WINDOW_LIST = new ArrayList<>();

    static {
        try {
            LOADING_WINDOW = new LoadingWindow();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void updateTheme () {
        UI ui = UI.getCurrent();
        if (ui == null) return;

        ThemeList themeList = ui.getElement().getThemeList();
        themeList.clear();
        if (!AppConfig.lightTheme) themeList.add(Lumo.DARK);
        //themeList.add(AppConfig.lightTheme ? "customlight" : "customdark");
    }

    public static Image reseizedImage (String path, int width, int height) {
        Image image = new Image(new StreamResource(AdvString.afterLast("/", path), () -> {
            try {
                RenderAPI render = new RenderAPI(50, 50);
                render.addImage(ImageIO.read (Objects.requireNonNull(WindowUtils.class.getResource(path))), 50, 50, 0, 0);

                return new ByteArrayInputStream(ImageUtils.toByteArray(render.getRenderedImage(), "png"));
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }), "");
        image.setHeight(String.valueOf(height));
        image.setWidth(String.valueOf(width));
        return image;
    }
}
