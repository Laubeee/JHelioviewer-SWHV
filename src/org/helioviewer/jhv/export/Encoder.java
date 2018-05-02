package org.helioviewer.jhv.export;

import java.awt.image.BufferedImage;

interface Encoder {

    void open(String path, int w, int h, int fps) throws Exception;

    void encode(BufferedImage image) throws Exception;

    void close() throws Exception;

    String getPath();

    int getHeight();

}
