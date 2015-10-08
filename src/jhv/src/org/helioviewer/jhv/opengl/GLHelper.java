package org.helioviewer.jhv.opengl;

import com.jogamp.opengl.GL2;

public class GLHelper {

    public static void drawCircle(GL2 gl, double x, double y, double r, int segments) {
        gl.glDisable(GL2.GL_TEXTURE_2D);
        {
            gl.glBegin(GL2.GL_TRIANGLE_FAN);
            gl.glVertex2f((float) x, (float) y);
            for (int n = 0; n <= segments; ++n) {
                double t = 2 * Math.PI * n / segments;
                gl.glVertex2f((float) (x + Math.sin(t) * r), (float) (y + Math.cos(t) * r));
            }
            gl.glEnd();
        }
        gl.glEnable(GL2.GL_TEXTURE_2D);
    }

    public static void drawRectangle(GL2 gl, double x0, double y0, double w, double h) {
        float x1 = (float) (x0 + w);
        float y1 = (float) (y0 + h);
        gl.glDisable(GL2.GL_TEXTURE_2D);
        {
            gl.glBegin(GL2.GL_QUADS);
            gl.glVertex2f((float) x0, (float) -y0);
            gl.glVertex2f((float) x0, -y1);
            gl.glVertex2f(x1, -y1);
            gl.glVertex2f(x1, (float) -y0);
            gl.glEnd();
        }
        gl.glEnable(GL2.GL_TEXTURE_2D);
    }

    public static void lineWidth(GL2 gl, double w) {
        gl.glLineWidth((float) (w * GLInfo.pixelScaleFloat[0]));
    }

}
