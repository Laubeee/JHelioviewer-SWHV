package org.helioviewer.jhv.camera.annotateable;

import java.awt.Color;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import com.jogamp.opengl.GL2;

public interface GL3DAnnotatable {

    public static final Color activeColor = Color.red;
    public static final Color dragColor = Color.yellow;
    public static final Color baseColor = Color.blue;

    public static final double radius = 1.01;

    public void render(GL2 gl);

    public void mouseDragged(MouseEvent e);

    public void mouseReleased(MouseEvent e);

    public void keyPressed(KeyEvent e);

    public void reset();

    public void mousePressed(MouseEvent e);

}
