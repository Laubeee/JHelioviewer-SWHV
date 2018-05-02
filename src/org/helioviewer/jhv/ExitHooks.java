package org.helioviewer.jhv;

//import javax.swing.JOptionPane;

import org.helioviewer.jhv.export.Export;
//import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.log.Log;

public class ExitHooks {

    private static final Thread finishMovieThread = new Thread(() -> {
        try {
            Export.getInstance().disposeMovieWriter(false);
        } catch (Exception e) {
            Log.warn("Movie was not shut down properly");
        }
    });

    public static void attach() {
        // At the moment this runs, the EventQueue is blocked (by enforcing to run System.exit on it which is blocking)
        Runtime.getRuntime().addShutdownHook(finishMovieThread);
    }

    public static boolean exitProgram() {
        // return !(JOptionPane.showConfirmDialog(ImageViewerGui.getMainFrame(), "Are you sure you want to quit?", "Confirm", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION);
        return true;
    }

}
