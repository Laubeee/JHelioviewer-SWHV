package org.helioviewer.jhv.opengl;

import com.jogamp.opengl.GL2;

class GLSLTextureShader extends GLSLShader {

    static final GLSLTextureShader texture = new GLSLTextureShader("/data/TextureVertex.glsl", "/data/TextureFrag.glsl");
    static int positionRef = 0;
    static int coordRef = 1;

    private int colorRef;

    private float[] color = { 1, 1, 1, 1 };

    private GLSLTextureShader(String vertex, String fragment) {
        super(vertex, fragment);
    }

    public static void init(GL2 gl) {
        texture._init(gl, false);
    }

    public static void dispose(GL2 gl) {
        texture._dispose(gl);
    }

    @Override
    protected void _dispose(GL2 gl) {
        super._dispose(gl);
    }

    @Override
    protected void _init(GL2 gl, boolean f) {
        super._init(gl, f);
    }

    @Override
    protected void _after_init(GL2 gl) {
        positionRef = gl.glGetAttribLocation(progID, "position");
        coordRef = gl.glGetAttribLocation(progID, "coord");
        colorRef = gl.glGetUniformLocation(progID, "color");
        setTextureUnit(gl, "image", 0);
    }

    public void bindParams(GL2 gl) {
        gl.glUniform4fv(colorRef, 1, color, 0);
    }

    public void setColor(float[] _color) {
        color = _color;
    }

}
