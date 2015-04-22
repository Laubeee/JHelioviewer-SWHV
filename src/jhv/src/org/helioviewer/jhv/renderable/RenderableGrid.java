package org.helioviewer.jhv.renderable;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontFormatException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;

import javax.media.opengl.GL2;

import org.helioviewer.base.FileUtils;
import org.helioviewer.base.logging.Log;
import org.helioviewer.base.physics.Constants;
import org.helioviewer.gl3d.GL3DState;
import org.helioviewer.gl3d.camera.GL3DCamera;
import org.helioviewer.jhv.plugin.renderable.Renderable;
import org.helioviewer.jhv.plugin.renderable.RenderableType;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.util.awt.TextRenderer;

public class RenderableGrid implements Renderable {
    private static final int SUBDIVISIONS = 120;

    private float lonstepDegrees = 13.2f;
    private float latstepDegrees = 20.f;
    private final float scale = 0.8f;
    private Font font;
    private TextRenderer renderer;
    private final int fontsize = 20;
    private final boolean followCamera;
    private final Color firstColor = Color.RED;
    private final Color secondColor = Color.GREEN;
    private final RenderableType renderableType;
    private final Component optionsPanel;
    private final String name = "Grid";
    private boolean isVisible = true;

    public RenderableGrid(RenderableType renderableType, boolean followCamera) {
        this.renderableType = renderableType;
        this.followCamera = followCamera;

        InputStream is = FileUtils.getResourceInputStream("/fonts/RobotoCondensed-Regular.ttf");
        try {
            font = Font.createFont(Font.TRUETYPE_FONT, is);
        } catch (FontFormatException e) {
            Log.warn("Font Not loaded correctly, fallback to default");
            font = new Font("Serif", Font.PLAIN, fontsize);
        } catch (IOException e) {
            Log.warn("Font Not loaded correctly, fallback to default");
            font = new Font("Serif", Font.PLAIN, fontsize);
        }

        optionsPanel = new RenderableGridOptionsPanel(this);
    }

    private float oldrelhi = -1;
    private int positionBufferID;
    private int colorBufferID;

    @Override
    public void render(GL3DState state) {
        if (!isVisible)
            return;
        GL2 gl = state.gl;
        gl.glPushMatrix();
        gl.glMultMatrixd(GL3DState.getActiveCamera().getLocalRotation().toMatrix().transpose().m, 0);
        {
            gl.glColor3d(1., 1., 0.);

            float relhi = (float) (GL3DCamera.INITFOV / (GL3DState.getActiveCamera().getCameraFOV())) * scale;
            if (relhi != oldrelhi) {
                oldrelhi = relhi;

                float cfontsize = this.fontsize * relhi;
                cfontsize = cfontsize < 10.f ? 10.f : cfontsize;
                font = font.deriveFont(cfontsize);

                renderer = new TextRenderer(font, true, false);
                renderer.setUseVertexArrays(true);
                //renderer.setSmoothing(true);
                renderer.setColor(Color.WHITE);
            }

            if (!followCamera) {
                drawText(gl);
            }

            drawCircles(state);
        }
        gl.glPopMatrix();
    }

    private void drawCircles(GL3DState state) {
        GL2 gl = state.gl;

        gl.glColor4f(1.0f, 0.0f, 0.0f, 1.0f);

        gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
        gl.glEnableClientState(GL2.GL_COLOR_ARRAY);
        {
            gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, positionBufferID);
            gl.glVertexPointer(2, GL2.GL_FLOAT, 0, 0);
            gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, colorBufferID);
            gl.glColorPointer(3, GL2.GL_FLOAT, 0, 0);

            gl.glRotatef(90f, 0f, 1f, 0f);
            gl.glPushMatrix();
            {
                float rotation = 0f;
                while (rotation <= 90f) {
                    gl.glDrawArrays(GL2.GL_LINE_STRIP, 0, SUBDIVISIONS);
                    gl.glRotatef(lonstepDegrees, 0f, 1f, 0f);
                    rotation += lonstepDegrees;
                }
            }
            gl.glPopMatrix();
            gl.glPushMatrix();
            {
                float rotation = 0f;
                rotation -= lonstepDegrees;
                gl.glRotatef(-lonstepDegrees, 0f, 1f, 0f);

                while (rotation >= -90f) {
                    gl.glDrawArrays(GL2.GL_LINE_STRIP, 0, SUBDIVISIONS);
                    gl.glRotatef(-lonstepDegrees, 0f, 1f, 0f);
                    rotation -= lonstepDegrees;
                }
            }
            gl.glPopMatrix();
            gl.glPushMatrix();
            {
                float rotation = 0f;
                gl.glRotatef(90.f, 1f, 0f, 0f);

                gl.glDrawArrays(GL2.GL_LINE_STRIP, 0, SUBDIVISIONS);
                while (rotation < 90.) {
                    gl.glPushMatrix();
                    {
                        gl.glTranslatef(0f, 0f, (float) Math.sin(Math.PI / 180. * rotation));
                        float scale = (float) Math.cos(Math.PI / 180. * rotation);
                        gl.glScalef(scale, scale, scale);
                        gl.glDrawArrays(GL2.GL_LINE_STRIP, 0, SUBDIVISIONS);
                    }
                    gl.glPopMatrix();
                    rotation += latstepDegrees;
                }

                rotation = latstepDegrees;
                while (rotation < 90.) {
                    gl.glPushMatrix();
                    {
                        gl.glTranslatef(0f, 0f, -(float) Math.sin(Math.PI / 180. * rotation));
                        float scale = (float) Math.cos(Math.PI / 180. * rotation);
                        gl.glScalef(scale, scale, scale);
                        gl.glDrawArrays(GL2.GL_LINE_STRIP, 0, SUBDIVISIONS);
                    }
                    gl.glPopMatrix();
                    rotation += latstepDegrees;
                }
                gl.glDrawArrays(GL2.GL_LINE_STRIP, 0, SUBDIVISIONS);

            }
            gl.glPopMatrix();
        }
        gl.glDisableClientState(GL2.GL_VERTEX_ARRAY);
        gl.glDisableClientState(GL2.GL_COLOR_ARRAY);
        gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, 0);

    }

    private void drawText(GL2 gl) {
        double size = Constants.SunRadius * 1.06;
        double zdist = 0.0;
        renderer.begin3DRendering();
        for (double phi = 0; phi <= 90; phi = phi + this.getLatstepDegrees()) {
            double angle = (90 - phi) * Math.PI / 180.;
            String txt = String.format("%.1f", phi);
            if (txt.substring(txt.length() - 1, txt.length()).equals("0")) {
                txt = txt.substring(0, txt.length() - 2);
            }
            renderer.draw3D(txt, (float) (Math.sin(angle) * size), (float) (Math.cos(angle) * size - scale * 0.02f * 20. / font.getSize()), (float) zdist, scale * 0.08f / font.getSize());
            if (phi != 90) {
                renderer.draw3D(txt, (float) (-Math.sin(angle) * size - scale * 0.03f * txt.length() * 20. / font.getSize()), (float) (Math.cos(angle) * size - scale * 0.02f * 20. / font.getSize()), (float) zdist, scale * 0.08f / font.getSize());
            }
        }
        for (double phi = -this.getLatstepDegrees(); phi >= -90; phi = phi - this.getLatstepDegrees()) {
            double angle = (90 - phi) * Math.PI / 180.;
            String txt = String.format("%.1f", phi);
            if (txt.substring(txt.length() - 1, txt.length()).equals("0")) {
                txt = txt.substring(0, txt.length() - 2);
            }
            renderer.draw3D(txt, (float) (Math.sin(angle) * size), (float) (Math.cos(angle) * size - scale * 0.02f * 20. / font.getSize()), (float) zdist, scale * 0.08f / font.getSize());
            if (phi != -90) {
                renderer.draw3D(txt, (float) (-Math.sin(angle) * size - scale * 0.03f * txt.length() * 20. / font.getSize()), (float) (Math.cos(angle) * size - scale * 0.02f * 20. / font.getSize()), (float) zdist, scale * 0.08f / font.getSize());
            }
        }
        renderer.end3DRendering();

        size = Constants.SunRadius * 1.02;

        for (double theta = 0; theta <= 180.; theta = theta + this.getLonstepDegrees()) {
            String txt = String.format("%.1f", theta);
            if (txt.substring(txt.length() - 1, txt.length()).equals("0")) {
                txt = txt.substring(0, txt.length() - 2);
            }
            double angle = (90 - theta) * Math.PI / 180.;
            renderer.begin3DRendering();
            gl.glPushMatrix();
            gl.glTranslatef((float) (Math.cos(angle) * size), 0f, (float) (Math.sin(angle) * size));
            gl.glRotated(theta, 0.f, 1.f, 0.f);
            renderer.draw3D(txt, 0.f, 0f, 0.f, scale * 0.08f / font.getSize());
            renderer.flush();
            renderer.end3DRendering();
            gl.glPopMatrix();
        }
        for (double theta = -this.getLonstepDegrees(); theta > -180.; theta = theta - this.getLonstepDegrees()) {
            String txt = String.format("%.1f", theta);
            if (txt.substring(txt.length() - 1, txt.length()).equals("0")) {
                txt = txt.substring(0, txt.length() - 2);
            }
            double angle = (90 - theta) * Math.PI / 180.;
            renderer.begin3DRendering();
            gl.glPushMatrix();
            gl.glTranslatef((float) (Math.cos(angle) * size), 0f, (float) (Math.sin(angle) * size));
            gl.glRotated(theta, 0.f, 1.f, 0.f);
            renderer.draw3D(txt, 0.f, 0f, 0.f, scale * 0.08f / font.getSize());
            renderer.flush();
            renderer.end3DRendering();
            gl.glPopMatrix();
        }
    }

    @Override
    public void init(GL3DState state) {
        FloatBuffer positionBuffer = FloatBuffer.allocate((SUBDIVISIONS + 1) * 2);
        FloatBuffer colorBuffer = FloatBuffer.allocate((SUBDIVISIONS + 1) * 3);

        for (int i = 0; i <= SUBDIVISIONS; i++) {
            positionBuffer.put((float) Math.cos(2 * Math.PI * i / SUBDIVISIONS));
            positionBuffer.put((float) Math.sin(2 * Math.PI * i / SUBDIVISIONS));
            if (i % 2 == 0) {
                colorBuffer.put(this.firstColor.getRed() / 255f);
                colorBuffer.put(this.firstColor.getGreen() / 255f);
                colorBuffer.put(this.firstColor.getBlue() / 255f);
            } else {
                colorBuffer.put(this.secondColor.getRed() / 255f);
                colorBuffer.put(this.secondColor.getGreen() / 255f);
                colorBuffer.put(this.secondColor.getBlue() / 255f);
            }

        }
        positionBuffer.flip();
        colorBuffer.flip();
        int positionBufferSize = positionBuffer.capacity();
        int colorBufferSize = colorBuffer.capacity();

        positionBufferID = generate(state);
        colorBufferID = generate(state);

        state.gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, positionBufferID);
        state.gl.glBufferData(GL2.GL_ARRAY_BUFFER, positionBufferSize * Buffers.SIZEOF_FLOAT, positionBuffer, GL2.GL_STATIC_DRAW);
        state.gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, colorBufferID);
        state.gl.glBufferData(GL2.GL_ARRAY_BUFFER, colorBufferSize * Buffers.SIZEOF_FLOAT, colorBuffer, GL2.GL_STATIC_DRAW);

    }

    private int generate(GL3DState state) {
        int[] tmpId = new int[1];
        state.gl.glGenBuffers(1, tmpId, 0);
        return tmpId[0];
    }

    @Override
    public void remove(GL3DState state) {
        state.gl.glDeleteBuffers(1, new int[] { positionBufferID }, 0);
        state.gl.glDeleteBuffers(1, new int[] { colorBufferID }, 0);
    }

    @Override
    public RenderableType getType() {
        return this.renderableType;
    }

    @Override
    public Component getOptionsPanel() {
        return optionsPanel;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isVisible() {
        return this.isVisible;
    }

    @Override
    public void setVisible(boolean isVisible) {
        this.isVisible = isVisible;
    }

    public double getLonstepDegrees() {
        return lonstepDegrees;
    }

    public void setLonstepDegrees(double lonstepDegrees) {
        this.lonstepDegrees = (float) lonstepDegrees;
    }

    public double getLatstepDegrees() {
        return latstepDegrees;
    }

    public void setLatstepDegrees(double latstepDegrees) {
        this.latstepDegrees = (float) latstepDegrees;
    }

    @Override
    public String getTimeString() {
        return "";
    }

}
