package org.helioviewer.jhv.plugins.swek;

import java.io.File;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.base.plugin.interfaces.Plugin;
import org.helioviewer.jhv.data.container.JHVEventContainer;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.plugins.swek.config.SWEKConfigurationManager;
import org.helioviewer.jhv.plugins.swek.renderable.SWEKData;
import org.helioviewer.jhv.plugins.swek.renderable.SWEKRenderable;
import org.helioviewer.jhv.plugins.swek.request.IncomingRequestManager;
import org.helioviewer.jhv.plugins.swek.settings.SWEKSettings;
import org.helioviewer.jhv.plugins.swek.sources.SWEKSourceManager;
import org.helioviewer.jhv.plugins.swek.view.SWEKPluginPanel;
import org.helioviewer.jhv.threads.JHVWorker;

/**
 * Part of these developments are based on the work done in the HEKPlugin
 * (lp:~jhelioviewer-dev/jhelioviewer/hekplugin) and HEKPlugin 3d
 * (lp:~jhelioviewer-dev/jhelioviewer/hekplugin-3d).
 *
 * @author Bram.Bourgoignie@oma.be
 *
 */
public class SWEKPlugin implements Plugin {

    /** Instance of the SWEKConfiguration */
    private final SWEKConfigurationManager SWEKConfig;

    /** Instance of the SWEKDownloadManager */
    private final SWEKSourceManager SWEKSources;

    /** the incoming request manager */
    private final IncomingRequestManager incomingRequestManager;

    /** instance of the event container */
    private final JHVEventContainer eventContainer;

    private final SWEKRenderable renderable;

    private final boolean loadExternalJars;

    public SWEKPlugin() {
        SWEKConfig = SWEKConfigurationManager.getSingletonInstance();
        SWEKSources = SWEKSourceManager.getSingletonInstance();
        loadExternalJars = true;
        SWEKSources.setPlugin(this);
        SWEKSources.loadExternalJars(loadExternalJars);

        incomingRequestManager = IncomingRequestManager.getSingletonInstance();
        eventContainer = JHVEventContainer.getSingletonInstance();
        renderable = new SWEKRenderable();
    }

    /**
     * Creates a SWEKPlugin that loads or doesn't load the external jars
     *
     * @param loadExternalJars
     *            true is the source jar should be loaded, false if the source
     *            jars should not be loaded.
     */
    public SWEKPlugin(boolean loadExternalJars) {
        SWEKConfig = SWEKConfigurationManager.getSingletonInstance();
        SWEKSources = SWEKSourceManager.getSingletonInstance();
        this.loadExternalJars = loadExternalJars;
        SWEKSources.loadExternalJars(loadExternalJars);

        incomingRequestManager = IncomingRequestManager.getSingletonInstance();
        eventContainer = JHVEventContainer.getSingletonInstance();
        renderable = new SWEKRenderable();
    }

    @Override
    public void installPlugin() {
        createPluginDirectoryStructure();
        JHVWorker<Void, Void> loadPlugin = new JHVWorker<Void, Void>() {

            @Override
            protected Void backgroundWork() {
                SWEKConfig.loadConfiguration();
                SWEKSources.loadSources();
                return null;
            }

            @Override
            protected void done() {
                eventContainer.registerHandler(incomingRequestManager);
                ImageViewerGui.getLeftContentPane().add("Space Weather Event Knowledgebase", SWEKPluginPanel.getSWEKPluginPanelInstance(), true);
                ImageViewerGui.getLeftContentPane().revalidate();

                SWEKData.getSingletonInstance().requestEvents();
                Layers.addLayersListener(SWEKData.getSingletonInstance());
                ImageViewerGui.getRenderableContainer().addRenderable(renderable);
            }

        };
        loadPlugin.setThreadName("SWEK--LoadPlugin");
        JHVGlobals.getExecutorService().execute(loadPlugin);
    }

    @Override
    public void uninstallPlugin() {
        ImageViewerGui.getRenderableContainer().removeRenderable(renderable);
        Layers.removeLayersListener(SWEKData.getSingletonInstance());
        SWEKData.getSingletonInstance().reset();

        ImageViewerGui.getLeftContentPane().remove(SWEKPluginPanel.getSWEKPluginPanelInstance());
        ImageViewerGui.getLeftContentPane().revalidate();
    }

    /*
     * Plugin interface
     */
    @Override
    public String getName() {
        return "Space Weather Event Knowledgebase " + "$Rev$";
    }

    @Override
    public String getDescription() {
        return "Space Weather Event Knowledgebase";
    }

    @Override
    public void setState(String state) {
    }

    @Override
    public String getState() {
        return null;
    }

    @Override
    public String getAboutLicenseText() {
        return "<p>The plugin uses the <a href=\"https://github.com/stleary/JSON-java\">JSON in Java</a> Library, licensed under a custom <a href=\"http://www.json.org/license.html\">License</a>.";
    }

    /**
     * Creates the directory structure in the home directory of the JHelioviewer
     */
    private void createPluginDirectoryStructure() {
        File swekHomeFile = new File(SWEKSettings.SWEK_HOME);
        if (!swekHomeFile.isDirectory()) {
            swekHomeFile.mkdirs();
        }
        File swekSourceJarDirectory = new File(SWEKSettings.SWEK_SOURCES);
        if (!swekSourceJarDirectory.isDirectory()) {
            swekSourceJarDirectory.mkdirs();
        }
    }

}
