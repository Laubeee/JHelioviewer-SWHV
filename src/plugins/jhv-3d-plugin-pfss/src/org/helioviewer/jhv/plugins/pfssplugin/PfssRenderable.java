package org.helioviewer.jhv.plugins.pfssplugin;

import java.awt.Component;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.layers.LayersListener;
import org.helioviewer.jhv.plugins.pfssplugin.data.PfssData;
import org.helioviewer.jhv.plugins.pfssplugin.data.PfssNewDataLoader;
import org.helioviewer.jhv.renderable.gui.Renderable;
import org.helioviewer.jhv.renderable.gui.RenderableType;
import org.helioviewer.viewmodel.view.View;

import com.jogamp.opengl.GL2;

/**
 * @author Stefan Meier (stefan.meier@fhnw.ch)
 * */
public class PfssRenderable implements Renderable, LayersListener {

    private final ScheduledExecutorService pfssNewLoadPool = Executors.newScheduledThreadPool(2);

    private boolean isVisible = false;
    private final RenderableType type;
    private final PfssPluginPanel optionsPanel;
    private PfssData previousPfssData = null;

    /**
     * Default constructor.
     */
    public PfssRenderable() {
        type = new RenderableType("PFSS plugin");
        optionsPanel = new PfssPluginPanel();
    }

    @Override
    public void render(GL2 gl) {
        if (isVisible) {
            Date currentTime = Displayer.getLastUpdatedTimestamp();
            if (currentTime == null)
                return;

            PfssData pfssData;
            long millis = currentTime.getTime();
            if ((pfssData = PfssPlugin.getPfsscache().getData(millis)) != null) {
                if (previousPfssData != null && previousPfssData != pfssData && previousPfssData.isInit()) {
                    previousPfssData.clear(gl);
                }
                if (!pfssData.isInit())
                    pfssData.init(gl);
                if (pfssData.isInit()) {
                    pfssData.display(gl);
                    datetime = pfssData.getDateString();
                    ImageViewerGui.getRenderableContainer().fireTimeUpdated(this);
                }
                previousPfssData = pfssData;
            }
        }
    }

    @Override
    public void remove(GL2 gl) {
        dispose(gl);
    }

    @Override
    public RenderableType getType() {
        return type;
    }

    @Override
    public Component getOptionsPanel() {
        return optionsPanel;
    }

    @Override
    public String getName() {
        return "PFSS model";
    }

    @Override
    public boolean isVisible() {
        return isVisible;
    }

    @Override
    public void setVisible(boolean isVisible) {
        this.isVisible = isVisible;
    }

    private String datetime = null;

    @Override
    public String getTimeString() {
        return datetime;
    }

    @Override
    public void layerAdded(View view) {
        PfssPlugin.getPfsscache().clear();
        Date start = Layers.getFirstDate();
        Date end = Layers.getLastDate();
        Thread pfssThread = new Thread(new PfssNewDataLoader(start, end), "PFFSLoader");
        Future<?> ff = pfssNewLoadPool.submit(pfssThread);
        Runnable abolishPfssThread = new Runnable() {
            private Future<?> ff;

            public Runnable init(Future<?> ff) {
                this.ff = ff;
                return this;
            }

            @Override
            public void run() {
                ff.cancel(true);
            }
        }.init(ff);
        pfssNewLoadPool.schedule(new Thread(abolishPfssThread, "Abolish PFSS"), 60 * 5, TimeUnit.SECONDS);
    };

    @Override
    public void activeLayerChanged(View view) {
    }

    @Override
    public boolean isDeletable() {
        return false;
    }

    @Override
    public boolean isActiveImageLayer() {
        return false;
    }

    @Override
    public void init(GL2 gl) {
    }

    @Override
    public void dispose(GL2 gl) {
        PfssPlugin.getPfsscache().destroy(gl);
    }

}
