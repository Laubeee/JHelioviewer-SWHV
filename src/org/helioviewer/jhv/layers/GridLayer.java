package org.helioviewer.jhv.layers;

import java.awt.Component;
import java.text.DecimalFormat;
import java.util.ArrayList;

import javax.annotation.Nullable;

import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.base.scale.GridScale;
import org.helioviewer.jhv.base.scale.GridType;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.math.MathUtils;
import org.helioviewer.jhv.math.Transform;
import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.opengl.GLSLLine;
import org.helioviewer.jhv.opengl.GLSLShape;
import org.helioviewer.jhv.opengl.GLText;
import org.helioviewer.jhv.opengl.text.JhvTextRenderer;
import org.helioviewer.jhv.position.Position;
import org.json.JSONObject;

import com.jogamp.opengl.GL2;

public class GridLayer extends AbstractLayer {

    private static final double RADIAL_UNIT = Sun.Radius;
    private static final double RADIAL_STEP = 15;
    private static final double RADIAL_UNIT_FAR = Sun.MeanEarthDistance / 10;
    private static final double RADIAL_STEP_FAR = 45;
    private static final float[] R_LABEL_POS = { (float) (2 * RADIAL_UNIT), (float) (8 * RADIAL_UNIT), (float) (24 * RADIAL_UNIT) };
    private static final float[] R_LABEL_POS_FAR = { (float) (2 * RADIAL_UNIT_FAR), (float) (8 * RADIAL_UNIT_FAR), (float) (24 * RADIAL_UNIT_FAR) };

    // height of text in solar radii
    private static final float textScale = GridLabel.textScale;
    private static final double thickness = 0.002;
    private static final double thicknessEarth = 0.002;
    private static final double thicknessAxes = 0.005;

    private static final DecimalFormat formatter2 = MathUtils.numberFormatter("0", 2);

    private GridType gridType = GridType.Viewpoint;

    private double lonStep = 15;
    private double latStep = 20;
    private boolean gridNeedsInit = true;

    private boolean showAxis = true;
    private boolean showLabels = true;
    private boolean showRadial = false;

    private final GLSLShape earthPoint = new GLSLShape();
    private final GLSLLine axesLine = new GLSLLine();
    private final GLSLLine earthCircleLine = new GLSLLine();
    private final GLSLLine radialCircleLine = new GLSLLine();
    private final GLSLLine radialThickLine = new GLSLLine();
    private final GLSLLine radialCircleLineFar = new GLSLLine();
    private final GLSLLine radialThickLineFar = new GLSLLine();
    private final GLSLLine flatLine = new GLSLLine();
    private final GLSLLine gridLine = new GLSLLine();

    private ArrayList<GridLabel> latLabels;
    private ArrayList<GridLabel> lonLabels;
    private final ArrayList<GridLabel> radialLabels;
    private final ArrayList<GridLabel> radialLabelsFar;

    private final Component optionsPanel;

    @Override
    public void serialize(JSONObject jo) {
        jo.put("lonStep", lonStep);
        jo.put("latStep", latStep);
        jo.put("showAxis", showAxis);
        jo.put("showLabels", showLabels);
        jo.put("showRadial", showRadial);
        jo.put("type", gridType);
    }

    private void deserialize(JSONObject jo) {
        lonStep = jo.optDouble("lonStep", lonStep);
        latStep = jo.optDouble("latStep", latStep);
        showAxis = jo.optBoolean("showAxis", showAxis);
        showLabels = jo.optBoolean("showLabels", showLabels);
        showRadial = jo.optBoolean("showRadial", showRadial);

        String strGridType = jo.optString("type", gridType.toString());
        try {
            gridType = GridType.valueOf(strGridType);
        } catch (Exception ignore) {
        }
    }

    public GridLayer(JSONObject jo) {
        if (jo != null)
            deserialize(jo);
        else
            setEnabled(true);
        optionsPanel = new GridLayerOptions(this);

        latLabels = GridLabel.makeLatLabels(latStep);
        lonLabels = GridLabel.makeLonLabels(gridType, lonStep);
        radialLabels = GridLabel.makeRadialLabels(0, RADIAL_STEP);
        radialLabelsFar = GridLabel.makeRadialLabels(Math.PI / 2, RADIAL_STEP_FAR);
    }

    public Vec2 gridPoint(Camera camera, Viewport vp, int x, int y) {
        return Display.mode.scale.mouseToGrid(x, y, vp, camera, gridType);
    }

    @Override
    public void render(Camera camera, Viewport vp, GL2 gl) {
        if (!isVisible[vp.idx])
            return;
        if (gridNeedsInit) {
            GridMath.initGrid(gl, gridLine, lonStep, latStep);
            gridNeedsInit = false;
        }

        if (showAxis)
            axesLine.render(gl, vp, thicknessAxes);

        Position viewpoint = camera.getViewpoint();
        double pixFactor = vp.height / (2 * camera.getWidth());
        drawEarthCircles(gl, vp, pixFactor, Sun.getEarth(viewpoint.time));

        double pixelsPerSolarRadius = textScale * pixFactor;

        Transform.pushView();
        Transform.rotateViewInverse(gridType.toQuat(viewpoint));
        {
            gridLine.render(gl, vp, thickness);
            if (showLabels) {
                drawGridText(gl, (int) pixelsPerSolarRadius);
            }
        }
        Transform.popView();

        if (showRadial) {
            boolean far = viewpoint.distance > 100 * Sun.MeanEarthDistance;
            Transform.pushView();
            Transform.rotateViewInverse(viewpoint.toQuat());
            {
                if (far) {
                    Transform.pushProjection();
                    camera.projectionOrthoFar(vp.aspect);
                    radialCircleLineFar.render(gl, vp, thickness);
                    radialThickLineFar.render(gl, vp, 3 * thickness);
                    if (showLabels)
                        drawRadialGridText(gl, radialLabelsFar, pixelsPerSolarRadius * RADIAL_UNIT_FAR, R_LABEL_POS_FAR);
                    Transform.popProjection();
                } else {
                    radialCircleLine.render(gl, vp, thickness);
                    radialThickLine.render(gl, vp, 3 * thickness);
                    if (showLabels)
                        drawRadialGridText(gl, radialLabels, pixelsPerSolarRadius * RADIAL_UNIT, R_LABEL_POS);
                }
            }
            Transform.popView();
        }
    }

    @Override
    public void renderScale(Camera camera, Viewport vp, GL2 gl) {
        if (!isVisible[vp.idx])
            return;
        int pixelsPerSolarRadius = (int) (textScale * vp.height / (2 * camera.getWidth()));
        drawGridFlat(gl, vp);
        if (showLabels) {
            drawGridTextFlat(pixelsPerSolarRadius, Display.mode.scale, vp);
        }
    }

    private double previousAspect = -1;

    private void drawGridFlat(GL2 gl, Viewport vp) {
        if (previousAspect != vp.aspect) {
            GridMath.initFlatGrid(gl, flatLine, vp.aspect);
            previousAspect = vp.aspect;
        }
        flatLine.render(gl, vp, thickness);
    }

    private static void drawGridTextFlat(int size, GridScale scale, Viewport vp) {
        float w = (float) vp.aspect;
        float h = 1;
        JhvTextRenderer renderer = GLText.getRenderer(size);
        float textScaleFactor = textScale / renderer.getFont().getSize2D() * w / GridMath.FLAT_STEPS_THETA * 5;

        renderer.begin3DRendering();
        {
            for (int i = 0; i <= GridMath.FLAT_STEPS_THETA; i++) {
                if (i == GridMath.FLAT_STEPS_THETA / 2) {
                    continue;
                }
                float start = -w / 2 + i * w / GridMath.FLAT_STEPS_THETA;
                String label = formatter2.format(scale.getInterpolatedXValue(1. / GridMath.FLAT_STEPS_THETA * i));
                renderer.draw3D(label, start, 0, 0, textScaleFactor);
            }
            for (int i = 0; i <= GridMath.FLAT_STEPS_RADIAL; i++) {
                String label = formatter2.format(scale.getInterpolatedYValue(1. / GridMath.FLAT_STEPS_RADIAL * i));
                float start = -h / 2 + i * h / GridMath.FLAT_STEPS_RADIAL;
                renderer.draw3D(label, 0, start, 0, textScaleFactor);
            }
        }
        renderer.end3DRendering();
    }

    private void drawEarthCircles(GL2 gl, Viewport vp, double factor, Position p) {
        Transform.pushView();
        Transform.rotateViewInverse(p.toQuat());
        {
            earthCircleLine.render(gl, vp, thicknessEarth);
            earthPoint.renderPoints(gl, factor);
        }
        Transform.popView();
    }

    private static void drawRadialGridText(GL2 gl, ArrayList<GridLabel> labels, double size, float[] labelPos) {
        gl.glDisable(GL2.GL_CULL_FACE);

        float fuzz = 0.75f;
        for (float rsize : labelPos) {
            JhvTextRenderer renderer = GLText.getRenderer((int) (fuzz * rsize * size));
            float textScaleFactor = textScale / renderer.getFont().getSize2D();
            renderer.begin3DRendering();
            for (GridLabel label : labels) {
                renderer.draw3D(label.txt, rsize * label.x, rsize * label.y, 0, fuzz * rsize * textScaleFactor);
            }
            renderer.end3DRendering();
        }

        gl.glEnable(GL2.GL_CULL_FACE);
    }

    private void drawGridText(GL2 gl, int size) {
        JhvTextRenderer renderer = GLText.getRenderer(size);
        // the scale factor has to be divided by the current font size
        float textScaleFactor = textScale / renderer.getFont().getSize2D();

        renderer.begin3DRendering();

        gl.glDisable(GL2.GL_CULL_FACE);
        for (GridLabel label : latLabels) {
            renderer.draw3D(label.txt, label.x, label.y, 0, textScaleFactor);
        }
        renderer.flush();
        gl.glEnable(GL2.GL_CULL_FACE);

        for (GridLabel lonLabel : lonLabels) {
            Transform.pushView();
            {
                Transform.translateView(lonLabel.x, 0, lonLabel.y);
                Transform.rotateView(lonLabel.theta, 0, 1, 0);

                renderer.draw3D(lonLabel.txt, 0, 0, 0, textScaleFactor);
                renderer.flush();
            }
            Transform.popView();
        }
        renderer.end3DRendering();
    }

    @Override
    public void init(GL2 gl) {
        gridLine.init(gl);
        GridMath.initGrid(gl, gridLine, lonStep, latStep);
        gridNeedsInit = false;

        axesLine.init(gl);
        GridMath.initAxes(gl, axesLine);

        earthCircleLine.init(gl);
        GridMath.initEarthCircles(gl, earthCircleLine);
        earthPoint.init(gl);
        GridMath.initEarthPoint(gl, earthPoint);

        radialCircleLine.init(gl);
        radialThickLine.init(gl);
        GridMath.initRadialCircles(gl, radialCircleLine, radialThickLine, RADIAL_UNIT, RADIAL_STEP);
        radialCircleLineFar.init(gl);
        radialThickLineFar.init(gl);
        GridMath.initRadialCircles(gl, radialCircleLineFar, radialThickLineFar, RADIAL_UNIT_FAR, RADIAL_STEP_FAR);

        flatLine.init(gl);
    }

    @Override
    public void dispose(GL2 gl) {
        gridLine.dispose(gl);
        axesLine.dispose(gl);
        earthCircleLine.dispose(gl);
        earthPoint.dispose(gl);
        radialCircleLine.dispose(gl);
        radialThickLine.dispose(gl);
        radialCircleLineFar.dispose(gl);
        radialThickLineFar.dispose(gl);
        flatLine.dispose(gl);
    }

    @Override
    public void remove(GL2 gl) {
        dispose(gl);
    }

    @Override
    public Component getOptionsPanel() {
        return optionsPanel;
    }

    @Override
    public String getName() {
        return "Grid";
    }

    public double getLonStep() {
        return lonStep;
    }

    public void setLonStep(double _lonStep) {
        lonStep = _lonStep;
        lonLabels = GridLabel.makeLonLabels(gridType, lonStep);
        gridNeedsInit = true;
    }

    public double getLatStep() {
        return latStep;
    }

    public void setLatStep(double _latStep) {
        latStep = _latStep;
        latLabels = GridLabel.makeLatLabels(latStep);
        gridNeedsInit = true;
    }

    public boolean getShowLabels() {
        return showLabels;
    }

    public boolean getShowAxis() {
        return showAxis;
    }

    public boolean getShowRadial() {
        return showRadial;
    }

    public void showLabels(boolean show) {
        showLabels = show;
    }

    public void showAxis(boolean show) {
        showAxis = show;
    }

    public void showRadial(boolean show) {
        showRadial = show;
    }

    @Nullable
    @Override
    public String getTimeString() {
        return null;
    }

    @Override
    public boolean isDeletable() {
        return false;
    }

    GridType getGridType() {
        return gridType;
    }

    void setGridType(GridType _gridType) {
        gridType = _gridType;
        lonLabels = GridLabel.makeLonLabels(gridType, lonStep);
    }

}
