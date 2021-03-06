package org.helioviewer.jhv.opengl;

import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.position.Position;

import com.jogamp.opengl.GL2;

public class GLSLSolarShader extends GLSLShader {

    // float[] bc = { 0.06136, 0.24477, 0.38774, 0.24477, 0.06136 }
    // http://rastergrid.com/blog/2010/09/efficient-gaussian-blur-with-linear-sampling/
    private static final float[] bc = {.30613f, .38774f, .30613f};
    private static final float[] blurKernel = {
            bc[0] * bc[0], bc[0] * bc[1], bc[0] * bc[2],
            bc[1] * bc[0], bc[1] * bc[1], bc[1] * bc[2],
            bc[2] * bc[0], bc[2] * bc[1], bc[2] * bc[2]
    };

    private static final float[] bo = {-1.2004377f, 0, 1.2004377f};
    private static final float[] blurOffset = {
            bo[0], bo[0], /**/ bo[1], bo[0], /**/ bo[2], bo[0],
            bo[0], bo[1], /**/ bo[1], bo[1], /**/ bo[2], bo[1],
            bo[0], bo[2], /**/ bo[1], bo[2], /**/ bo[2], bo[2]
    };

    public static final GLSLSolarShader ortho = new GLSLSolarShader("/glsl/solar.vert", "/glsl/solarOrtho.frag");
    public static final GLSLSolarShader lati = new GLSLSolarShader("/glsl/solar.vert", "/glsl/solarLati.frag");
    public static final GLSLSolarShader polar = new GLSLSolarShader("/glsl/solar.vert", "/glsl/solarPolar.frag");
    public static final GLSLSolarShader logpolar = new GLSLSolarShader("/glsl/solar.vert", "/glsl/solarLogPolar.frag");

    private int isDiffRef;

    private int hgltRef;
    private int hglnRef;
    private int hgltDiffRef;
    private int hglnDiffRef;
    private int crotaRef;
    private int crotaDiffRef;

    private int sectorRef;
    private int radiiRef;
    private int polarRadiiRef;
    private int cutOffDirectionRef;
    private int cutOffValueRef;

    private int slitRef;
    private int brightRef;
    private int colorRef;
    private int sharpenRef;
    private int enhancedRef;
    private int calculateDepthRef;

    private int rectRef;
    private int diffRectRef;
    private int viewportRef;
    private int viewportOffsetRef;

    private int cameraTransformationInverseRef;
    private int cameraDifferenceRotationQuatRef;
    private int diffCameraDifferenceRotationQuatRef;

    private final int[] isDiff = new int[1];

    private final float[] hglt = new float[2];
    private final float[] hgln = new float[1];
    private final float[] crota = new float[3];
    private final float[] hgltDiff = new float[2];
    private final float[] hglnDiff = new float[1];
    private final float[] crotaDiff = new float[3];

    private final float[] sector = new float[3];
    private final float[] radii = new float[2];
    private final float[] polarRadii = new float[2];
    private final float[] cutOffDirection = new float[2];
    private final float[] cutOffValue = new float[1];

    private final float[] slit = new float[2];
    private final float[] bright = new float[3];
    private final float[] color = new float[4];
    private final float[] sharpen = new float[3];
    private final int[] enhanced = new int[1];
    private final int[] calculateDepth = new int[1];

    private final float[] rect = new float[4];
    private final float[] diffRect = new float[4];
    private final float[] viewport = new float[3];
    private final float[] viewportOffset = new float[2];

    private final float[] quatArray = new float[4];

    private GLSLSolarShader(String vertex, String fragment) {
        super(vertex, fragment);
    }

    public static void init(GL2 gl) {
        ortho._init(gl, true);
        lati._init(gl, true);
        polar._init(gl, true);
        logpolar._init(gl, true);
    }

    @Override
    protected void bindAttribLocations(GL2 gl, int id) {
        gl.glBindAttribLocation(id, 0, "Vertex");
    }

    @Override
    protected void initUniforms(GL2 gl, int id) {
        isDiffRef = gl.glGetUniformLocation(id, "isdifference");

        hgltRef = gl.glGetUniformLocation(id, "hglt");
        hglnRef = gl.glGetUniformLocation(id, "hgln");
        crotaRef = gl.glGetUniformLocation(id, "crota");
        hgltDiffRef = gl.glGetUniformLocation(id, "hgltDiff");
        hglnDiffRef = gl.glGetUniformLocation(id, "hglnDiff");
        crotaDiffRef = gl.glGetUniformLocation(id, "crotaDiff");

        sectorRef = gl.glGetUniformLocation(id, "sector");
        radiiRef = gl.glGetUniformLocation(id, "radii");
        polarRadiiRef = gl.glGetUniformLocation(id, "polarRadii");
        cutOffDirectionRef = gl.glGetUniformLocation(id, "cutOffDirection");
        cutOffValueRef = gl.glGetUniformLocation(id, "cutOffValue");

        sharpenRef = gl.glGetUniformLocation(id, "sharpen");
        slitRef = gl.glGetUniformLocation(id, "slit");
        brightRef = gl.glGetUniformLocation(id, "brightness");
        colorRef = gl.glGetUniformLocation(id, "color");
        enhancedRef = gl.glGetUniformLocation(id, "enhanced");
        calculateDepthRef = gl.glGetUniformLocation(id, "calculateDepth");

        rectRef = gl.glGetUniformLocation(id, "rect");
        diffRectRef = gl.glGetUniformLocation(id, "differencerect");
        viewportRef = gl.glGetUniformLocation(id, "viewport");
        viewportOffsetRef = gl.glGetUniformLocation(id, "viewportOffset");

        cameraTransformationInverseRef = gl.glGetUniformLocation(id, "cameraTransformationInverse");
        cameraDifferenceRotationQuatRef = gl.glGetUniformLocation(id, "cameraDifferenceRotationQuat");
        diffCameraDifferenceRotationQuatRef = gl.glGetUniformLocation(id, "diffcameraDifferenceRotationQuat");

        int blurKernelRef = gl.glGetUniformLocation(id, "blurKernel");
        int blurOffsetRef = gl.glGetUniformLocation(id, "blurOffset");

        gl.glUniform1fv(blurKernelRef, blurKernel.length, blurKernel, 0);
        gl.glUniform2fv(blurOffsetRef, blurOffset.length / 2, blurOffset, 0);

        setTextureUnit(gl, id, "image", GLTexture.Unit.ZERO);
        setTextureUnit(gl, id, "lut", GLTexture.Unit.ONE);
        setTextureUnit(gl, id, "diffImage", GLTexture.Unit.TWO);
    }

    public static void dispose(GL2 gl) {
        ortho._dispose(gl);
        lati._dispose(gl);
        polar._dispose(gl);
        logpolar._dispose(gl);
    }

    public void bindMatrix(GL2 gl, float[] matrix) {
        gl.glUniformMatrix4fv(cameraTransformationInverseRef, 1, false, matrix, 0);
    }

    public void bindCameraDifferenceRotationQuat(GL2 gl, Quat quat) {
        quat.setFloatArray(quatArray);
        gl.glUniform4fv(cameraDifferenceRotationQuatRef, 1, quatArray, 0);
    }

    public void bindDiffCameraDifferenceRotationQuat(GL2 gl, Quat quat) {
        quat.setFloatArray(quatArray);
        gl.glUniform4fv(diffCameraDifferenceRotationQuatRef, 1, quatArray, 0);
    }

    public void bindRect(GL2 gl, double xOffset, double yOffset, double xScale, double yScale) {
        rect[0] = (float) xOffset;
        rect[1] = (float) yOffset;
        rect[2] = (float) xScale;
        rect[3] = (float) yScale;
        gl.glUniform4fv(rectRef, 1, rect, 0);
    }

    public void bindDiffRect(GL2 gl, double diffXOffset, double diffYOffset, double diffXScale, double diffYScale) {
        diffRect[0] = (float) diffXOffset;
        diffRect[1] = (float) diffYOffset;
        diffRect[2] = (float) diffXScale;
        diffRect[3] = (float) diffYScale;
        gl.glUniform4fv(diffRectRef, 1, diffRect, 0);
    }

    public void bindColor(GL2 gl, float red, float green, float blue, double alpha, double blend) {
        color[0] = (float) (red * alpha);
        color[1] = (float) (green * alpha);
        color[2] = (float) (blue * alpha);
        color[3] = (float) (alpha * blend); // http://amindforeverprogramming.blogspot.be/2013/07/why-alpha-premultiplied-colour-blending.html
        gl.glUniform4fv(colorRef, 1, color, 0);
    }

    public void bindSlit(GL2 gl, double left, double right) {
        slit[0] = (float) left;
        slit[1] = (float) right;
        gl.glUniform1fv(slitRef, 2, slit, 0);
    }

    public void bindBrightness(GL2 gl, double offset, double scale, double gamma) {
        bright[0] = (float) offset;
        bright[1] = (float) scale;
        bright[2] = (float) gamma;
        gl.glUniform3fv(brightRef, 1, bright, 0);
    }

    public void bindSharpen(GL2 gl, double weight, double pixelWidth, double pixelHeight) {
        sharpen[0] = (float) pixelWidth;
        sharpen[1] = (float) pixelHeight;
        sharpen[2] = -2 * (float) weight; // used for mix
        gl.glUniform3fv(sharpenRef, 1, sharpen, 0);
    }

    public void bindEnhanced(GL2 gl, boolean _enhanced) {
        enhanced[0] = _enhanced ? 1 : 0;
        gl.glUniform1iv(enhancedRef, 1, enhanced, 0);
    }

    public void bindCalculateDepth(GL2 gl, boolean _calculateDepth) {
        calculateDepth[0] = _calculateDepth ? 1 : 0;
        gl.glUniform1iv(calculateDepthRef, 1, calculateDepth, 0);
    }

    public void bindIsDiff(GL2 gl, int _isDiff) {
        isDiff[0] = _isDiff;
        gl.glUniform1iv(isDiffRef, 1, isDiff, 0);
    }

    public void bindViewport(GL2 gl, float offsetX, float offsetY, float width, float height) {
        viewportOffset[0] = offsetX;
        viewportOffset[1] = offsetY;
        gl.glUniform2fv(viewportOffsetRef, 1, viewportOffset, 0);
        viewport[0] = width;
        viewport[1] = height;
        viewport[2] = height / width;
        gl.glUniform3fv(viewportRef, 1, viewport, 0);
    }

    public void bindCutOffValue(GL2 gl, double val) {
        cutOffValue[0] = (float) val;
        gl.glUniform1fv(cutOffValueRef, 1, cutOffValue, 0);
    }

    public void bindCutOffDirection(GL2 gl, double x, double y) {
        cutOffDirection[0] = (float) x;
        cutOffDirection[1] = (float) y;
        gl.glUniform2fv(cutOffDirectionRef, 1, cutOffDirection, 0);
    }

    public void bindAngles(GL2 gl, Position viewpoint, double _crota, double scrota, double ccrota) {
        hglt[0] = (float) Math.sin(viewpoint.lat);
        hglt[1] = (float) Math.cos(viewpoint.lat);
        gl.glUniform1fv(hgltRef, 2, hglt, 0);
        hgln[0] = (float) ((viewpoint.lon + 2. * Math.PI) % (2. * Math.PI));
        gl.glUniform1fv(hglnRef, 1, hgln, 0);
        crota[0] = (float) _crota;
        crota[1] = (float) scrota;
        crota[2] = (float) ccrota;
        gl.glUniform1fv(crotaRef, 3, crota, 0);
    }

    public void bindAnglesDiff(GL2 gl, Position viewpoint, double _crota, double scrota, double ccrota) {
        hgltDiff[0] = (float) Math.sin(viewpoint.lat);
        hgltDiff[1] = (float) Math.cos(viewpoint.lat);
        gl.glUniform1fv(hgltDiffRef, 2, hgltDiff, 0);
        hglnDiff[0] = (float) ((viewpoint.lon + 2. * Math.PI) % (2. * Math.PI));
        gl.glUniform1fv(hglnDiffRef, 1, hglnDiff, 0);
        crotaDiff[0] = (float) _crota;
        crotaDiff[1] = (float) scrota;
        crotaDiff[2] = (float) ccrota;
        gl.glUniform1fv(crotaDiffRef, 3, crotaDiff, 0);
    }

    public void bindSector(GL2 gl, double sector0, double sector1) {
        sector[0] = sector0 == sector1 ? 0 : 1;
        sector[1] = (float) sector0;
        sector[2] = (float) sector1;
        gl.glUniform1fv(sectorRef, 3, sector, 0);
    }

    public void bindRadii(GL2 gl, double innerRadius, double outerRadius) {
        radii[0] = (float) innerRadius;
        radii[1] = (float) outerRadius;
        gl.glUniform1fv(radiiRef, 2, radii, 0);
    }

    public void bindPolarRadii(GL2 gl, double start, double stop) {
        polarRadii[0] = (float) start;
        polarRadii[1] = (float) stop;
        gl.glUniform1fv(polarRadiiRef, 2, polarRadii, 0);
    }

}
