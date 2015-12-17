package org.helioviewer.jhv.opengl;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.nio.ByteBuffer;

import org.helioviewer.jhv.base.scale.GridScale;
import org.helioviewer.jhv.base.scale.Transform;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.components.MainComponent;
import org.helioviewer.jhv.layers.Layers;

import com.jogamp.opengl.FBObject;
import com.jogamp.opengl.FBObject.Attachment.Type;
import com.jogamp.opengl.FBObject.TextureAttachment;
import com.jogamp.opengl.GL2;

public class GLGrab {

    private final FBObject fbo = new FBObject();
    private TextureAttachment fboTex;
    private final int w;
    private final int h;

    public GLGrab(int _w, int _h) {
        w = _w;
        h = _h;
    }

    public void init(GL2 gl) {
        fbo.init(gl, w, h, 0);
        fboTex = fbo.attachTexture2D(gl, 0, true);

        fbo.attachRenderbuffer(gl, Type.DEPTH, FBObject.CHOSEN_BITS);
        fbo.unbind(gl);
    }

    public void dispose(GL2 gl) {
        fbo.detachAll(gl);
        fbo.destroy(gl);
    }

    public BufferedImage renderFrame(GL2 gl) {
        BufferedImage screenshot;

        int _x = Displayer.fullViewport.x;
        int _y = Displayer.fullViewport.y;
        int _w = Displayer.fullViewport.width;
        int _h = Displayer.fullViewport.height;

        Displayer.setGLSize(0, 0, fbo.getWidth(), fbo.getHeight());
        Displayer.reshapeAll();
        {
            fbo.bind(gl);
            Camera camera = Displayer.getCamera();
            if (Displayer.mode == Displayer.DisplayMode.POLAR) {
                MainComponent.renderSceneScale(camera, gl, GLSLShader.polar, new GridScale.GridScaleIdentity(0, 360, 0, 180, Transform.transformpolar));
            } else if (Displayer.mode == Displayer.DisplayMode.LATITUDINAL) {
                MainComponent.renderSceneScale(camera, gl, GLSLShader.lati, new GridScale.GridScaleIdentity(0, 360, 0, Layers.getLargestPhysicalSize() / 2, Transform.transformpolar));
            } else if (Displayer.mode == Displayer.DisplayMode.LOGPOLAR) {
                MainComponent.renderSceneScale(camera, gl, GLSLShader.logpolar, new GridScale.GridScaleIdentity(0, 360, Math.log(0.05), Math.log(Layers.getLargestPhysicalSize() / 2), Transform.transformpolar));
            } else {
                MainComponent.renderScene(camera, gl);
            }
            MainComponent.renderFloatScene(camera, gl);
            fbo.unbind(gl);

            fbo.use(gl, fboTex);

            screenshot = new BufferedImage(fbo.getWidth(), fbo.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
            byte[] array = ((DataBufferByte) screenshot.getRaster().getDataBuffer()).getData();
            ByteBuffer fb = ByteBuffer.wrap(array);

            gl.glBindFramebuffer(GL2.GL_READ_FRAMEBUFFER, fbo.getReadFramebuffer());
            gl.glPixelStorei(GL2.GL_PACK_ALIGNMENT, 1);
            gl.glReadPixels(0, 0, fbo.getWidth(), fbo.getHeight(), GL2.GL_BGR, GL2.GL_UNSIGNED_BYTE, fb);
            gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, 0);

            fbo.unuse(gl);
        }
        Displayer.setGLSize(_x, _y, _w, _h);
        Displayer.reshapeAll();

        return screenshot;
    }
}
