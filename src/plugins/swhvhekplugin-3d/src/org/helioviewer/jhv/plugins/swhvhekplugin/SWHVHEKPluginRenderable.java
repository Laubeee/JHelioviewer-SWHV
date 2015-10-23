package org.helioviewer.jhv.plugins.swhvhekplugin;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.ImageIcon;

import org.helioviewer.jhv.base.astronomy.Position;
import org.helioviewer.jhv.base.astronomy.Sun;
import org.helioviewer.jhv.base.math.Mat4d;
import org.helioviewer.jhv.base.math.Quatd;
import org.helioviewer.jhv.base.math.Vec2d;
import org.helioviewer.jhv.base.math.Vec3d;
import org.helioviewer.jhv.camera.GL3DViewport;
import org.helioviewer.jhv.data.datatype.event.JHVCoordinateSystem;
import org.helioviewer.jhv.data.datatype.event.JHVEvent;
import org.helioviewer.jhv.data.datatype.event.JHVEventParameter;
import org.helioviewer.jhv.data.datatype.event.JHVPositionInformation;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.UIGlobals;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.opengl.GLHelper;
import org.helioviewer.jhv.opengl.GLTexture;
import org.helioviewer.jhv.renderable.gui.AbstractRenderable;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.util.awt.TextRenderer;

public class SWHVHEKPluginRenderable extends AbstractRenderable {

    private static final SWHVHEKPopupController controller = new SWHVHEKPopupController();

    private static final double LINEWIDTH = 0.5;
    private static final double LINEWIDTH_CACTUS = 1.01;

    private static final double LINEWIDTH_HI = 1;

    private static HashMap<String, GLTexture> iconCacheId = new HashMap<String, GLTexture>();
    private final static double ICON_SIZE = 0.1;
    private final static double ICON_SIZE_HIGHLIGHTED = 0.16;
    private final static int LEFT_MARGIN_TEXT = 10;
    private final static int RIGHT_MARGIN_TEXT = 10;
    private final static int TOP_MARGIN_TEXT = 5;
    private final static int BOTTOM_MARGIN_TEXT = 5;
    private final static int MOUSE_OFFSET_X = 45;
    private final static int MOUSE_OFFSET_Y = 45;
    private JHVEvent highLightedEvent = null;

    private void bindTexture(GL2 gl, String key, ImageIcon icon) {
        GLTexture tex = iconCacheId.get(key);
        if (tex == null) {
            BufferedImage bi = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics graph = bi.createGraphics();
            icon.paintIcon(null, graph, 0, 0);
            graph.dispose();

            tex = new GLTexture(gl);
            tex.bind(gl, GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE0);
            tex.copyBufferedImage2D(gl, bi);
            iconCacheId.put(key, tex);
        }

        tex.bind(gl, GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE0);
    }

    private void interPolatedDraw(GL2 gl, int mres, double r_start, double r_end, double t_start, double t_end, Quatd q) {
        gl.glBegin(GL2.GL_LINE_STRIP);
        {
            for (int i = 0; i <= mres; i++) {
                double alpha = 1. - i / (double) mres;
                double r = alpha * r_start + (1 - alpha) * (r_end);
                double theta = alpha * t_start + (1 - alpha) * (t_end);
                Vec3d res = q.rotateInverseVector(new Vec3d(r * Math.cos(theta), r * Math.sin(theta), 0));
                gl.glVertex3f((float) res.x, (float) res.y, (float) res.z);
            }
        }
        gl.glEnd();
    }

    private final int texCoordHelpers[][] = { { 0, 0 }, { 1, 0 }, { 1, 1 }, { 0, 1 } };;

    private void drawCactusArc(GL2 gl, JHVEvent evt, Date timestamp) {

        Map<String, JHVEventParameter> params = evt.getAllEventParameters();
        double angularWidthDegree = SWHVHEKData.readCMEAngularWidthDegree(params);
        double angularWidth = Math.toRadians(angularWidthDegree);
        double principalAngleDegree = SWHVHEKData.readCMEPrincipalAngleDegree(params);
        double principalAngle = Math.toRadians(principalAngleDegree);
        double speed = SWHVHEKData.readCMESpeed(params);
        double factor = Sun.RadiusMeter;
        double distSunBegin = 2.4;
        double distSun = distSunBegin + speed * (timestamp.getTime() - evt.getStartDate().getTime()) / factor;
        int lineResolution = 2;

        Date date = new Date((evt.getStartDate().getTime() + evt.getEndDate().getTime()) / 2);
        Position.Latitudinal p = Sun.getEarth(date);
        Quatd q = new Quatd(p.lat, p.lon);

        double thetaStart = principalAngle - angularWidth / 2.;
        double thetaEnd = principalAngle + angularWidth / 2.;

        Color color = evt.getEventRelationShip().getRelationshipColor();
        if (color == null) {
            color = evt.getColor();
        }

        gl.glColor3f(0f, 0f, 0f);
        GLHelper.lineWidth(gl, LINEWIDTH_CACTUS * 1.2);
        int angularResolution = (int) (angularWidthDegree / 4);
        interPolatedDraw(gl, angularResolution, distSun, distSun, thetaStart, principalAngle, q);
        interPolatedDraw(gl, angularResolution, distSun, distSun, principalAngle, thetaEnd, q);

        gl.glColor3f(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f);
        GLHelper.lineWidth(gl, LINEWIDTH_CACTUS);

        interPolatedDraw(gl, angularResolution, distSun, distSun, thetaStart, principalAngle, q);
        interPolatedDraw(gl, angularResolution, distSun, distSun, principalAngle, thetaEnd, q);

        interPolatedDraw(gl, lineResolution, distSunBegin, distSun + 0.05, thetaStart, thetaStart, q);
        interPolatedDraw(gl, lineResolution, distSunBegin, distSun + 0.05, principalAngle, principalAngle, q);
        interPolatedDraw(gl, lineResolution, distSunBegin, distSun + 0.05, thetaEnd, thetaEnd, q);

        String type = evt.getJHVEventType().getEventType();
        bindTexture(gl, type, evt.getIcon());
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_LINEAR);

        double sz = ICON_SIZE;
        if (evt.isHighlighted()) {
            sz = ICON_SIZE_HIGHLIGHTED;
        }
        gl.glColor3f(1, 1, 1);
        gl.glEnable(GL2.GL_CULL_FACE);
        gl.glEnable(GL2.GL_TEXTURE_2D);
        gl.glBegin(GL2.GL_QUADS);
        {
            for (int i = 0; i < texCoordHelpers.length; i++) {
                int[] el = texCoordHelpers[i];
                double deltatheta = sz / distSun * (el[1] * 2 - 1);
                double deltar = sz * (el[0] * 2 - 1);
                double r = distSun + deltar;
                double theta = principalAngle + deltatheta;
                Vec3d res = q.rotateInverseVector(new Vec3d(r * Math.cos(theta), r * Math.sin(theta), 0));
                gl.glTexCoord2f(el[0], el[1]);
                gl.glVertex3f((float) res.x, (float) res.y, (float) res.z);
            }
        }
        gl.glEnd();
        gl.glDisable(GL2.GL_TEXTURE_2D);
        gl.glDisable(GL2.GL_CULL_FACE);
    }

    private void drawPolygon(GL2 gl, JHVEvent evt, Date timestamp) {
        HashMap<JHVCoordinateSystem, JHVPositionInformation> pi = evt.getPositioningInformation();

        if (!pi.containsKey(JHVCoordinateSystem.JHV)) {
            return;
        }

        JHVPositionInformation el = pi.get(JHVCoordinateSystem.JHV);
        List<Vec3d> points = el.getBoundCC();
        if (points == null || points.size() == 0) {
            points = el.getBoundBox();
            if (points == null || points.size() == 0) {
                return;
            }
        }

        Color color = evt.getEventRelationShip().getRelationshipColor();
        if (color == null) {
            color = evt.getColor();
        }
        gl.glColor3f(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f);

        GLHelper.lineWidth(gl, evt.isHighlighted() ? LINEWIDTH_HI : LINEWIDTH);

        // draw bounds
        Vec3d oldBoundaryPoint3d = null;

        for (Vec3d point : points) {
            int divpoints = 10;

            gl.glBegin(GL2.GL_LINE_STRIP);
            if (oldBoundaryPoint3d != null) {
                for (int j = 0; j <= divpoints; j++) {
                    double alpha = 1. - j / (double) divpoints;
                    double xnew = alpha * oldBoundaryPoint3d.x + (1 - alpha) * point.x;
                    double ynew = alpha * oldBoundaryPoint3d.y + (1 - alpha) * point.y;
                    double znew = alpha * oldBoundaryPoint3d.z + (1 - alpha) * point.z;
                    double r = Math.sqrt(xnew * xnew + ynew * ynew + znew * znew);
                    gl.glVertex3f((float) (xnew / r), (float) -(ynew / r), (float) (znew / r));
                }
            }
            gl.glEnd();

            oldBoundaryPoint3d = point;
        }
    }

    private void drawIcon(GL2 gl, JHVEvent evt, Date timestamp) {
        String type = evt.getJHVEventType().getEventType();
        HashMap<JHVCoordinateSystem, JHVPositionInformation> pi = evt.getPositioningInformation();

        if (pi.containsKey(JHVCoordinateSystem.JHV)) {
            JHVPositionInformation el = pi.get(JHVCoordinateSystem.JHV);
            if (el.centralPoint() != null) {
                Vec3d pt = el.centralPoint();
                bindTexture(gl, type, evt.getIcon());
                if (evt.isHighlighted()) {
                    this.drawImage3d(gl, pt.x, pt.y, pt.z, ICON_SIZE_HIGHLIGHTED, ICON_SIZE_HIGHLIGHTED);
                } else {
                    this.drawImage3d(gl, pt.x, pt.y, pt.z, ICON_SIZE, ICON_SIZE);
                }
            }
        }
    }

    private void drawImage3d(GL2 gl, double x, double y, double z, double width, double height) {
        y = -y;

        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_LINEAR);

        gl.glColor3f(1, 1, 1);
        double width2 = width / 2.;
        double height2 = height / 2.;

        Vec3d sourceDir = new Vec3d(0, 0, 1);
        Vec3d targetDir = new Vec3d(x, y, z);

        Vec3d axis = sourceDir.cross(targetDir);
        axis.normalize();
        Mat4d r = Mat4d.rotation(Math.atan2(x, z), Vec3d.YAxis);
        r.rotate(-Math.asin(y / targetDir.length()), Vec3d.XAxis);

        Vec3d p0 = new Vec3d(-width2, -height2, 0);
        Vec3d p1 = new Vec3d(-width2, height2, 0);
        Vec3d p2 = new Vec3d(width2, height2, 0);
        Vec3d p3 = new Vec3d(width2, -height2, 0);

        p0 = r.multiply(p0);
        p1 = r.multiply(p1);
        p2 = r.multiply(p2);
        p3 = r.multiply(p3);
        p0.add(targetDir);
        p1.add(targetDir);
        p2.add(targetDir);
        p3.add(targetDir);

        gl.glEnable(GL2.GL_CULL_FACE);
        gl.glEnable(GL2.GL_TEXTURE_2D);
        gl.glBegin(GL2.GL_QUADS);
        {
            gl.glTexCoord2f(1, 1);
            gl.glVertex3f((float) p3.x, (float) p3.y, (float) p3.z);
            gl.glTexCoord2f(1, 0);
            gl.glVertex3f((float) p2.x, (float) p2.y, (float) p2.z);
            gl.glTexCoord2f(0, 0);
            gl.glVertex3f((float) p1.x, (float) p1.y, (float) p1.z);
            gl.glTexCoord2f(0, 1);
            gl.glVertex3f((float) p0.x, (float) p0.y, (float) p0.z);
        }
        gl.glEnd();
        gl.glDisable(GL2.GL_TEXTURE_2D);
        gl.glDisable(GL2.GL_CULL_FACE);
    }

    private static final double vpScale = 0.019;
    private TextRenderer textRenderer;
    private Font font;
    private float oldFontSize = -1;

    public void drawText(GL2 gl, JHVEvent evt, Point pt) {
        int height = Displayer.getGLHeight();
        int width = Displayer.getGLWidth();

        float fontSize = (int) (height * vpScale);
        if (textRenderer == null || fontSize != oldFontSize) {
            oldFontSize = fontSize;
            font = UIGlobals.UIFontRoboto.deriveFont(fontSize);
            if (textRenderer != null) {
                textRenderer.dispose();
            }
            textRenderer = new TextRenderer(font, true, true);
            textRenderer.setUseVertexArrays(true);
            textRenderer.setSmoothing(false);
            textRenderer.setColor(Color.WHITE);
        }

        textRenderer.beginRendering(width, height, true);

        Map<String, JHVEventParameter> params = evt.getVisibleEventParameters();

        Vec2d bd = new Vec2d(0, 0);
        int ct = 0;
        for (Entry<String, JHVEventParameter> entry : params.entrySet()) {
            String txt = entry.getValue().getParameterDisplayName() + " : " + entry.getValue().getParameterValue();
            Rectangle2D bound = textRenderer.getBounds(txt);
            if (bd.x < bound.getWidth())
                bd.x = bound.getWidth();
            ct++;
        }
        bd.y = fontSize * 1.1 * (ct);

        Point textInit = new Point(pt.x, pt.y);
        float w = (float) (bd.x + LEFT_MARGIN_TEXT + RIGHT_MARGIN_TEXT);
        float h = (float) (bd.y + BOTTOM_MARGIN_TEXT + TOP_MARGIN_TEXT);

        // Correct if out of view
        if (w + pt.x + MOUSE_OFFSET_X - LEFT_MARGIN_TEXT > width) {
            textInit.x -= (w + pt.x + MOUSE_OFFSET_X - LEFT_MARGIN_TEXT - width);
        }
        if (h + pt.y + MOUSE_OFFSET_Y - fontSize - TOP_MARGIN_TEXT > height) {
            textInit.y -= (h + pt.y + MOUSE_OFFSET_Y - fontSize - TOP_MARGIN_TEXT - height);
        }
        float left = textInit.x + MOUSE_OFFSET_X - LEFT_MARGIN_TEXT;
        float bottom = textInit.y + MOUSE_OFFSET_Y - fontSize - TOP_MARGIN_TEXT;

        gl.glColor4f(0.5f, 0.5f, 0.5f, 0.75f);
        gl.glDisable(GL2.GL_TEXTURE_2D);
        gl.glPushMatrix();
        gl.glLoadIdentity();
        {
            gl.glBegin(GL2.GL_QUADS);
            gl.glVertex2f(left, height - bottom);
            gl.glVertex2f(left, height - bottom - h);
            gl.glVertex2f(left + w, height - bottom - h);
            gl.glVertex2f(left + w, height - bottom);
            gl.glEnd();

        }
        gl.glPopMatrix();

        gl.glEnable(GL2.GL_TEXTURE_2D);
        textRenderer.setColor(Color.WHITE);
        int deltaY = MOUSE_OFFSET_Y;
        for (Entry<String, JHVEventParameter> entry : params.entrySet()) {
            String txt = entry.getValue().getParameterDisplayName() + " : " + entry.getValue().getParameterValue();
            textRenderer.draw(txt, textInit.x + MOUSE_OFFSET_X, height - textInit.y - deltaY);
            deltaY += fontSize * 1.1;
        }
        textRenderer.endRendering();
        gl.glDisable(GL2.GL_TEXTURE_2D);
    }

    @Override
    public void render(GL2 gl, GL3DViewport vp) {
        if (isVisible[vp.getIndex()]) {
            ArrayList<JHVEvent> eventsToDraw = SWHVHEKData.getSingletonInstance().getActiveEvents(controller.currentTime);
            highLightedEvent = null;
            for (JHVEvent evt : eventsToDraw) {
                if (evt.getName().equals("Coronal Mass Ejection")) {
                    drawCactusArc(gl, evt, controller.currentTime);
                } else {
                    drawPolygon(gl, evt, controller.currentTime);

                    gl.glDisable(GL2.GL_DEPTH_TEST);
                    drawIcon(gl, evt, controller.currentTime);
                    gl.glEnable(GL2.GL_DEPTH_TEST);
                }
                if (evt.isHighlighted()) {
                    highLightedEvent = evt;
                }
            }

            SWHVHEKSettings.resetCactusColor();
        }
    }

    @Override
    public void renderFloat(GL2 gl, GL3DViewport vp) {
        if (isVisible[vp.getIndex()]) {
            if (SWHVHEKPopupController.mouseOverJHVEvent != null) {
                drawText(gl, SWHVHEKPopupController.mouseOverJHVEvent, SWHVHEKPopupController.mouseOverPosition);
            }
        }
    }

    @Override
    public void remove(GL2 gl) {
        dispose(gl);
        ImageViewerGui.getInputController().removePlugin(controller);
    }

    @Override
    public Component getOptionsPanel() {
        return null;
    }

    @Override
    public String getName() {
        return "SWEK events";
    }

    @Override
    public void setVisible(boolean isVisible) {
        super.setVisible(isVisible);

        if (isVisible) {
            controller.timeChanged(Layers.addTimeListener(controller));
            ImageViewerGui.getInputController().addPlugin(controller);
        } else {
            ImageViewerGui.getInputController().removePlugin(controller);
            Layers.removeTimeListener(controller);
        }
    }

    @Override
    public String getTimeString() {
        return null;
    }

    @Override
    public boolean isDeletable() {
        return false;
    }

    @Override
    public void init(GL2 gl) {
        setVisible(true);
    }

    @Override
    public void dispose(GL2 gl) {
        for (GLTexture el : iconCacheId.values()) {
            el.delete(gl);
        }
        iconCacheId.clear();
    }

    @Override
    public void renderMiniview(GL2 gl, GL3DViewport vp) {
    }

}
