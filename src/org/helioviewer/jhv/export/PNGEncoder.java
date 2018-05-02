package org.helioviewer.jhv.export;

import java.io.IOException;
import java.io.File;
import java.awt.image.BufferedImage;

import javax.imageio.ImageIO;

public class PNGEncoder implements Encoder {

    private String path;
    private int height;
    private BufferedImage image;

    @Override
    public void open(String _path, int w, int h, int fps) {
        path = _path;
        height = h;
    }

    @Override
    public void encode(BufferedImage _image) {
        image = _image;
    }

    @Override
    public void close() throws IOException {
        // ImageUtils.writePNG(image, path);
        ImageIO.write(image, "png", new File(path));
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public int getHeight() {
        return height;
    }

}
