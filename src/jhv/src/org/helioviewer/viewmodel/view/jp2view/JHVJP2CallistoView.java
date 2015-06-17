package org.helioviewer.viewmodel.view.jp2view;

import java.awt.Dimension;
import java.awt.EventQueue;

import org.helioviewer.base.Region;
import org.helioviewer.base.Viewport;
import org.helioviewer.base.datetime.ImmutableDateTime;
import org.helioviewer.viewmodel.view.jp2view.J2KRender.RenderReasons;
import org.helioviewer.viewmodel.view.jp2view.image.JP2ImageParameter;
import org.helioviewer.viewmodel.view.jp2view.image.ResolutionSet.ResolutionLevel;
import org.helioviewer.viewmodel.view.jp2view.image.SubImage;

public class JHVJP2CallistoView extends JHVJP2View {

    private JHVJP2CallistoViewDataHandler dataHandler;

    public JHVJP2CallistoView() {
        region = new Region(0, 0, 86400, 380);
        viewport = new Viewport(2700, 12);
    }

    @Override
    public boolean setViewport(Viewport v) {
        viewport = v;
        renderRequestedSignal.signal(RenderReasons.OTHER);
        return true;
    }

    @Override
    public boolean setRegion(Region r) {
        region = r;
        setImageViewParams(calculateParameter());
        return true;
    }

    @Override
    public void render() {
    }

    public void setJHVJP2CallistoViewDataHandler(JHVJP2CallistoViewDataHandler dataHandler) {
        this.dataHandler = dataHandler;
    }

    @Override
    protected JP2ImageParameter calculateParameter() {
        return this.calculateParameter(imageViewParams.qualityLayers, 0);
    }

    @Override
    protected JP2ImageParameter calculateParameter(int numQualityLayers, int frameNumber) {
        int maxHeight = jp2Image.getResolutionSet().getResolutionLevel(0).getResolutionBounds().height;
        int maxWidth = jp2Image.getResolutionSet().getResolutionLevel(0).getResolutionBounds().width;
        ResolutionLevel res = jp2Image.getResolutionSet().getClosestResolutionLevel(new Dimension((int) Math.ceil(viewport.getWidth() / region.getWidth() * maxWidth), 2 * (int) Math.ceil(viewport.getHeight() / region.getHeight() * maxHeight)));

        SubImage subImage = new SubImage((int) (region.getLowerLeftCorner().x / maxWidth * res.getResolutionBounds().width), (int) (region.getLowerLeftCorner().y / maxHeight * res.getResolutionBounds().height), (int) (region.getWidth() / maxWidth * res.getResolutionBounds().width), (int) (region.getHeight() / maxHeight * res.getResolutionBounds().height));
        return new JP2ImageParameter(subImage, res, numQualityLayers, 0);
    }

    @Override
    protected void fireFrameChanged(JHVJP2View aView, ImmutableDateTime aDateTime) {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (dataHandler != null) {
                    dataHandler.handleData(JHVJP2CallistoView.this);
                }
            }
        });
    }

    public void removeJHVJP2DataHandler() {
        dataHandler = null;
    }

}
