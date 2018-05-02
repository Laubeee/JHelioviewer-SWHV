package org.helioviewer.jhv.export;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.Serializable;

import javax.swing.ImageIcon;

import org.helioviewer.jhv.base.image.NIOImageFactory;

class ExportFrame implements Serializable {

    private static final long serialVersionUID = 42L;

    private final ImageIcon mainIcon;
    private final ImageIcon eveIcon;

    ExportFrame(BufferedImage mainImage, BufferedImage eveImage) {
        mainIcon = new ImageIcon(mainImage);
        eveIcon = eveImage == null ? null : new ImageIcon(eveImage);
    }

    BufferedImage getMainImage() {
        BufferedImage bi = NIOImageFactory.createCompatible(mainIcon.getIconWidth(), mainIcon.getIconHeight(), BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D g = bi.createGraphics();
        mainIcon.paintIcon(null, g, 0, 0);
        g.dispose();

        return bi;
    }

    BufferedImage getEveImage() {
        if (eveIcon == null)
            return null;

        BufferedImage bi = new BufferedImage(eveIcon.getIconWidth(), eveIcon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = bi.createGraphics();
        eveIcon.paintIcon(null, g, 0, 0);
        g.dispose();

        return bi;
    }

}
