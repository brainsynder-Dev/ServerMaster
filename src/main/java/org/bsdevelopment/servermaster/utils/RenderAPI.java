/*
 * Copyright © 2024
 * BSDevelopment <https://bsdevelopment.org>
 */

package org.bsdevelopment.servermaster.utils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Base64;
import java.util.HashMap;

public class RenderAPI {
    private final BufferedImage image;
    private final HashMap<String, Graphics> layers = new HashMap<>();
    private final int defaultWidth;
    private final int defaultHeight;

    private int width;
    private int height;

    public RenderAPI(int width, int height) {
        this(width, height, ImageType.INT_ARGB);
    }

    /**
     * Constructs a {@link RenderAPI} object.
     * Only handy for advanced usage.
     *
     * @param width  The width of the image.
     * @param height The height of the image.
     * @param type   The imagetype.
     */
    public RenderAPI(int width, int height, ImageType type) {
        this.image = new BufferedImage(width, height, type.getValue());
        this.defaultWidth = width;
        this.defaultHeight = height;
        this.width = width;
        this.height = height;

        //Adds the default layer
        layers.put("background", this.image.createGraphics());
        layers.put("default", this.image.createGraphics());
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    /**
     * Adds a background to the first layer.
     *
     * @param url The url of the image.
     */
    public void addBackgroundImage(String url) {
        //TODO Apply caching.
        try {
            layers.get("background").drawImage(ImageIO.read(new URL(url)), 0, 0, this.width, this.height, null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addBackgroundImage(Image image) {
        layers.get("background").drawImage(image, 0, 0, this.width, this.height, null);
    }

    public void addBackgroundImage(Image image, int width, int height) {
        this.width = width;
        this.height = height;

        layers.get("background").drawImage(image, 0, 0, this.width, this.height, null);
    }

    /**
     * Creates a new layer in the image.
     *
     * @param id This is the ID of the layer. For example: text, images, background, etc.
     */
    public RenderAPI addLayer(String id) {
        layers.put(id, this.image.createGraphics());
        return this;
    }

    /**
     * Removes a layer from the image.
     *
     * @param id This is the ID of the layer. For example: text, images, background, etc.
     */
    public void removeLayer(String id) {
        layers.remove(id);
    }

    /**
     * Set the color of the text in a layer
     *
     * @param id Layer ID
     * @param c  Color
     */
    public void setColor(String id, Color c) {
        layers.get(id).setColor(c);
    }
    public void setColor(Color c) {
        layers.get("default").setColor(c);
    }

    /**
     * Set the font of the text in a layer
     *
     * @param id       Layer ID
     * @param font     Font name
     * @param fontSize Font size as Integer
     */
    public void setTextFont(String id, String font, int fontType, float fontSize) {
        layers.get(id).setFont(new Font(font, fontType, (int) fontSize));
    }
    public void setTextFont(Font font) {
        layers.get("default").setFont(font);
    }

    /**
     * Adds a image from a URL to the first layer.
     *
     * @param url    The URL of the image you want to add.
     * @param width  The width of the image.
     * @param height The height of the image.
     * @param x      The x position of the text (-1 is one to the left, +1 is one to the right)
     * @param y      The y position of the text (-1 is one to the bottom, +1 is one to the top)
     */
    public void addImage(String url, int width, int height, int x, int y) {
        addImage("default", url, width, height, x, y);
    }

    /**
     * Adds a image from a URL to a layer.
     *
     * @param id     The ID of the layer.
     * @param url    The URL of the image you want to add.
     * @param width  The width of the image.
     * @param height The height of the image.
     * @param x      The x position of the text (-1 is one to the left, +1 is one to the right)
     * @param y      The y position of the text (-1 is one to the bottom, +1 is one to the top)
     */
    public void addImage(String id, String url, int width, int height, int x, int y) {
        if (!layers.containsKey(id)) addLayer(id);

        //TODO Apply caching.
        try {
            layers.get(id).drawImage(ImageIO.read(new URL(url)), x, y, width, height, null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Adds a image from the {@link Image} class to the first layer.
     *
     * @param img    The image you want to add.
     * @param width  The width of the image.
     * @param height The height of the image.
     * @param x      The x position of the text (-1 is one to the left, +1 is one to the right)
     * @param y      The y position of the text (-1 is one to the bottom, +1 is one to the top)
     */
    public void addImage(Image img, int width, int height, int x, int y) {
        addImage("default", img, width, height, x, y);
    }

    /**
     * Adds a image from the {@link Image} class to a layer.
     *
     * @param id     The ID of the layer.
     * @param img    The image you want to add.
     * @param width  The width of the image.
     * @param height The height of the image.
     * @param x      The x position of the text (-1 is one to the left, +1 is one to the right)
     * @param y      The y position of the text (-1 is one to the bottom, +1 is one to the top)
     */
    public void addImage(String id, Image img, int width, int height, int x, int y) {
        layers.get(id).drawImage(img, x, y, width, height, null);
    }

    /**
     * Adds text to the first layer.
     *
     * @param text The text you want to add.
     * @param x    The x position of the text (-1 is one to the left, +1 is one to the right)
     * @param y    The y position of the text (-1 is one to the bottom, +1 is one to the top)
     */
    public void addText(String text, int x, int y) {
        addText("default", text, x, y);
    }

    /**
     * Adds text to a layer.
     *
     * @param id   The ID of the layer.
     * @param text The text you want to add.
     * @param x    The x position of the text (-1 is one to the left, +1 is one to the right)
     * @param y    The y position of the text (-1 is one to the bottom, +1 is one to the top)
     */
    public void addText(String id, String text, int x, int y) {
        layers.get(id).drawString(text, x, y);
    }

    /**
     * Adds text to the first layer in the center.
     *
     * @param text The text you want to add.
     * @param y    The y position of the text (-1 is one to the bottom, +1 is one to the top)
     */
    public void addTextCenter(String text, int y) {
        addTextCenter("default", text, y);
    }

    /**
     * Adds text to a layer in the center.
     *
     * @param id   The ID of the layer.
     * @param text The text you want to add.
     * @param y    The y position of the text (-1 is one to the bottom, +1 is one to the top)
     */
    public void addTextCenter(String id, String text, int y) {
        Font f = layers.get(id).getFont();
        FontMetrics fm = layers.get(id).getFontMetrics(f);

        int x = (this.width - fm.stringWidth(text)) / 2;

        layers.get(id).drawString(text, x, y);
    }

    /**
     * Adds a line to the first layer.
     *
     * @param x1 The first position's x.
     * @param y1 The first position's y.
     * @param x2 The second position's x.
     * @param y2 The second position's y.
     */
    public void addLine(int x1, int y1, int x2, int y2) {
        addLine("default", x1, y1, x2, y2);
    }

    /**
     * Adds a line to a layer.
     *
     * @param id The ID of the layer.
     * @param x1 The first position's x.
     * @param y1 The first position's y.
     * @param x2 The second position's x.
     * @param y2 The second position's y.
     */
    public void addLine(String id, int x1, int y1, int x2, int y2) {
        layers.get(id).drawLine(x1, y1, x2, y2);
    }

    /**
     * Adds a rectangle to the first layer.
     *
     * @param x      The startposition's x.
     * @param y      The startposition's y.
     * @param width  The width of the rectangle.
     * @param height The height of the rectangle.
     */
    public void addRectangle(int x, int y, int width, int height) {
        addRectangle("default", x, y, width, height);
    }

    /**
     * Adds a rectangle to a layer.
     *
     * @param id     The ID of the layer.
     * @param x      The startposition's x.
     * @param y      The startposition's y.
     * @param width  The width of the rectangle.
     * @param height The height of the rectangle.
     */
    public void addRectangle(String id, int x, int y, int width, int height) {
        layers.get(id).drawRect(x, y, width, height);
    }

    /**
     * Adds a rounded rectangle to the first layer.
     *
     * @param x               The startposition's x.
     * @param y               The startposition's y.
     * @param width           The width of the circle.
     * @param height          The height of the circle.
     * @param horizontalWidth The horizontal diameter of the circle.
     * @param verticalWidth   The vertical diameter of the circle.
     */
    public void addRoundedRectangle(int x, int y, int width, int height, int horizontalWidth, int verticalWidth) {
        addRoundedRectangle("default", x, y, width, height, horizontalWidth, verticalWidth);
    }

    /**
     * Adds a rounded rectangle to a layer.
     *
     * @param id              The ID of the layer.
     * @param x               The startposition's x.
     * @param y               The startposition's y.
     * @param width           The width of the circle.
     * @param height          The height of the circle.
     * @param horizontalWidth The horizontal diameter of the circle.
     * @param verticalWidth   The vertical diameter of the circle.
     */
    public void addRoundedRectangle(String id, int x, int y, int width, int height, int horizontalWidth, int verticalWidth) {
        layers.get(id).drawRoundRect(x, y, width, height, horizontalWidth, verticalWidth);
    }

    /**
     * Adds a circle to the first layer.
     *
     * @param x The startposition's x.
     * @param y The startposition's y.
     * @param r The radius of the circle.
     */
    public void addCircle(int x, int y, int r) {
        addCircle("default", x, y, r);
    }

    /**
     * Adds a circle to a layer.
     *
     * @param id The ID of the layer.
     * @param x  The startposition's x.
     * @param y  The startposition's y.
     * @param r  The radius of the circle.
     */
    public void addCircle(String id, int x, int y, int r) {
        x = x - (r / 2);
        y = y - (r / 2);
        layers.get(id).drawOval(x, y, r, r);
    }

    /**
     * Get the rendered image.
     *
     * @return Image
     */
    public BufferedImage getRenderedImage() {
        return image;
    }

    BufferedImage getScaledImage(Image image, int width, int height) {
        BufferedImage resizedImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = resizedImg.createGraphics();

        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(image, 0, 0, width, height, null);
        g2.dispose();

        return resizedImg;
    }

    /**
     * Get the rendered image in Base64 format.
     *
     * @param formatName The format [png, jpg, etc.]
     * @return Base64 string
     */
    public String getRenderedImageBase64(String formatName) {
        RenderedImage img = getRenderedImage();

        String out = null;
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            ImageIO.write(img, formatName, os);
            out = Base64.getEncoder().encodeToString(os.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return out;
    }

    /**
     * The {@link ImageType} enum.
     *
     * @author Stijn
     */
    public enum ImageType {
        THREEBYTE_BGR(5),
        FOURBYTE_ABGR(6),
        FOURBYTE_ABGR_PRE(7),
        BYTE_BINARY(12),
        BYTE_GRAY(10),
        BYTE_INDEXED(13),
        INT_ARGB(2),
        INT_ARGB_PRE(3),
        INT_BGR(4),
        INT_RGB(1),
        USHORT_555_RGB(9),
        USHORT_565_RGB(8),
        USHORT_GRAY(11);

        private final int value;

        ImageType(int value) {
            this.value = value;
        }

        public Integer getValue() {
            return value;
        }
    }
}