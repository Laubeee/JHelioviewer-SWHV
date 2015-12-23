package org.helioviewer.jhv.viewmodel.view.jp2view;

import java.awt.EventQueue;
import java.awt.Rectangle;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import kdu_jni.KduException;
import kdu_jni.Kdu_compositor_buf;
import kdu_jni.Kdu_coords;
import kdu_jni.Kdu_dims;
import kdu_jni.Kdu_global;
import kdu_jni.Kdu_ilayer_ref;
import kdu_jni.Kdu_region_compositor;
import kdu_jni.Kdu_thread_env;

import org.helioviewer.jhv.viewmodel.imagecache.ImageCacheStatus;
import org.helioviewer.jhv.viewmodel.imagecache.ImageCacheStatus.CacheStatus;
import org.helioviewer.jhv.viewmodel.imagedata.ARGBInt32ImageData;
import org.helioviewer.jhv.viewmodel.imagedata.ImageData;
import org.helioviewer.jhv.viewmodel.imagedata.SingleChannelByte8ImageData;
import org.helioviewer.jhv.viewmodel.metadata.HelioviewerMetaData;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;
import org.helioviewer.jhv.viewmodel.view.jp2view.image.JP2ImageParameter;
import org.helioviewer.jhv.viewmodel.view.jp2view.kakadu.KakaduConstants;
import org.helioviewer.jhv.viewmodel.view.jp2view.kakadu.KakaduUtils;

class J2KRender implements Runnable {

    private static final ThreadLocal<int[]> bufferLocal = new ThreadLocal<int[]>() {
        @Override
        protected int[] initialValue() {
            return new int[KakaduConstants.MAX_RENDER_SAMPLES];
        }
    };

    private static final int[] firstComponent = new int[] { 0 };

    /** A reference to the JP2Image this object is owned by. */
    private final JP2Image parentImageRef;

    private final ImageCacheStatus cacheStatusRef;

    /** A reference to the JP2View this object is owned by. */
    private final JP2View parentViewRef;

    private final JP2ImageParameter params;

    private final float scaleFactor;

    J2KRender(JP2View _parentViewRef, JP2ImageParameter _currParams, float _scaleFactor) {
        parentViewRef = _parentViewRef;
        params = _currParams;
        parentImageRef = params.jp2Image;
        cacheStatusRef = parentImageRef.getImageCacheStatus();
        scaleFactor = _scaleFactor;
    }

    private void renderLayer(Kdu_region_compositor compositor) throws KduException {
        int numLayer = params.compositionLayer;

        CacheStatus status = cacheStatusRef.getImageStatus(numLayer);
        if (status != CacheStatus.COMPLETE && status != CacheStatus.PARTIAL)
            return;

        // compositor.Refresh();
        // compositor.Remove_ilayer(new Kdu_ilayer_ref(), true);

        Kdu_ilayer_ref ilayer;
        Kdu_dims dimsRef1 = new Kdu_dims(), dimsRef2 = new Kdu_dims();
        int numComponents = parentImageRef.getNumComponents();
        // parentImageRef.deactivateColorLookupTable(numLayer);
        if (numComponents < 3) {
            // alpha tbd
            ilayer = compositor.Add_primitive_ilayer(numLayer, firstComponent, KakaduConstants.KDU_WANT_CODESTREAM_COMPONENTS, dimsRef1, dimsRef2);
        } else {
            ilayer = compositor.Add_ilayer(numLayer, dimsRef1, dimsRef2);
        }

        compositor.Set_scale(false, false, false, params.resolution.getZoomPercent(), scaleFactor);
        Kdu_dims requestedRegion = KakaduUtils.roiToKdu_dims(params.subImage);
        compositor.Set_buffer_surface(requestedRegion);

        Kdu_compositor_buf compositorBuf = compositor.Get_composition_buffer(new Kdu_dims());
        Kdu_dims actualRenderedRegion = compositorBuf.Get_rendering_region();

        // avoid gc
        Kdu_coords actualOffset = new Kdu_coords(actualRenderedRegion.Access_pos().Get_x(), actualRenderedRegion.Access_pos().Get_y());
        Kdu_coords actualSize = actualRenderedRegion.Access_size();
        int aWidth = actualSize.Get_x(), aHeight = actualSize.Get_y();

        Kdu_dims newRegion = new Kdu_dims();

        int[] intBuffer = null;
        byte[] byteBuffer = null;

        if (numComponents < 3) {
            byteBuffer = new byte[aWidth * aHeight];
        } else {
            intBuffer = new int[aWidth * aHeight];
        }

        int[] localIntBuffer = bufferLocal.get();
        while (!compositor.Is_processing_complete()) {
            compositor.Process(KakaduConstants.MAX_RENDER_SAMPLES, newRegion);
            Kdu_coords newOffset = newRegion.Access_pos();
            Kdu_coords newSize = newRegion.Access_size();

            newOffset.Subtract(actualOffset);

            int newWidth = newSize.Get_x();
            int newHeight = newSize.Get_y();
            int newPixels = newWidth * newHeight;
            if (newPixels == 0) {
                continue;
            }

            localIntBuffer = newPixels > localIntBuffer.length ? new int[newPixels << 1] : localIntBuffer;
            compositorBuf.Get_region(newRegion, localIntBuffer);

            int srcIdx = 0;
            int destIdx = newOffset.Get_x() + newOffset.Get_y() * aWidth;

            if (numComponents < 3) {
                for (int row = 0; row < newHeight; row++, destIdx += aWidth, srcIdx += newWidth) {
                    for (int col = 0; col < newWidth; ++col) {
                        byteBuffer[destIdx + col] = (byte) (localIntBuffer[srcIdx + col] & 0xFF);
                    }
                }
            } else {
                for (int row = 0; row < newHeight; row++, destIdx += aWidth, srcIdx += newWidth) {
                    System.arraycopy(localIntBuffer, srcIdx, intBuffer, destIdx, newWidth);
                }
            }
        }

        compositorBuf.Native_destroy();
        compositor.Remove_ilayer(ilayer, status == CacheStatus.COMPLETE ? false : true);

        ImageData imdata = null;
        if (numComponents < 3) {
            imdata = new SingleChannelByte8ImageData(aWidth, aHeight, ByteBuffer.wrap(byteBuffer));
        } else {
            imdata = new ARGBInt32ImageData(false, aWidth, aHeight, IntBuffer.wrap(intBuffer));
        }
        setImageData(imdata);
    }

    private void setImageData(ImageData newImageData) {
        MetaData metaData = params.jp2Image.metaDataList[params.compositionLayer];

        newImageData.setMetaData(metaData);
        newImageData.setViewpoint(params.viewpoint);
        newImageData.setROI(new Rectangle(params.subImage.x, params.subImage.y, params.subImage.width, params.subImage.height));

        if (metaData instanceof HelioviewerMetaData) {
            newImageData.setRegion(((HelioviewerMetaData) metaData).roiToRegion(params.subImage, params.resolution.scaleX, params.resolution.scaleY));
        }

        EventQueue.invokeLater(new Runnable() {
            private ImageData theImageData;

            @Override
            public void run() {
                parentViewRef.setImageData(theImageData);
            }

            public Runnable init(ImageData imagedata) {
                theImageData = imagedata;
                return this;
            }
        }.init(newImageData));
    }

    @Override
    public void run() {
        try {
            renderLayer(parentImageRef.getCompositor(threadEnv.get()));
        } catch (Exception e) {
            // reboot the compositor
            try {
                parentImageRef.destroyEngine();
            } catch (KduException ex) {
                ex.printStackTrace();
            }
            e.printStackTrace();
        }
    }

    static final ThreadEnvLocal threadEnv = new ThreadEnvLocal();

    static class ThreadEnvLocal extends ThreadLocal<Kdu_thread_env> {
        @Override
        protected Kdu_thread_env initialValue() {
            try {
                return createThreadEnv();
            } catch (KduException e) {
                e.printStackTrace();
            }
            return null;
        }

        public void destroy() {
            destroyThreadEnv(get());
            set(null);
        }
    }

    private static Kdu_thread_env createThreadEnv() throws KduException {
        Kdu_thread_env threadEnv = new Kdu_thread_env();
        // System.out.println(">>>> threadEnv create " + threadEnv);
        threadEnv.Create();
        int numThreads = Kdu_global.Kdu_get_num_processors();
        for (int i = 1; i < numThreads; i++)
            threadEnv.Add_thread();
        return threadEnv;
    }

    private static void destroyThreadEnv(Kdu_thread_env threadEnv) {
        if (threadEnv != null) {
            // System.out.println(">>>> threadEnv destroy " + threadEnv);
            threadEnv.Native_destroy();
        }
    }

}
