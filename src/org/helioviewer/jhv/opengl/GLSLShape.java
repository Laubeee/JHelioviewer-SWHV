package org.helioviewer.jhv.opengl;

import org.helioviewer.jhv.base.Buf;
import org.helioviewer.jhv.log.Log;

import com.jogamp.opengl.GL2;

public class GLSLShape extends VAO {

    private static final int size0 = 4;
    private static final int size1 = 4;
    public static final int stride = 4 * size0 + size1;

    private int count;

    public GLSLShape(boolean _dynamic) {
        super(_dynamic, new VAA[]{new VAA(0, size0, false, stride, 0, 0), new VAA(1, size1, true, stride, 4 * size0, 0)});
    }

    public void setData(GL2 gl, Buf buf) {
        if ((count = buf.getFloats() / size0) == 0)
            return;
        if (count * size0 != buf.getFloats() || count != buf.getBytes4()) {
            Log.error("Something is wrong with the attributes of this GLSLShape");
            return;
        }
        vbo.setData(gl, buf);
    }

    public void renderPoints(GL2 gl, double factor) {
        if (count == 0)
            return;

        GLSLShapeShader.point.use(gl);
        GLSLShapeShader.point.bindParams(gl, factor);
        GLSLShapeShader.point.bindMVP(gl);

        bind(gl);
        gl.glDrawArrays(GL2.GL_POINTS, 0, count);
    }

    public void renderShape(GL2 gl, int mode) {
        if (count == 0)
            return;

        GLSLShapeShader.shape.use(gl);
        GLSLShapeShader.shape.bindMVP(gl);

        bind(gl);
        gl.glDrawArrays(mode, 0, count);
    }

}
