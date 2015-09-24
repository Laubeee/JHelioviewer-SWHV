package org.helioviewer.jhv.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.helioviewer.jhv.camera.GL3DCamera;
import org.helioviewer.jhv.display.Displayer;

/**
 * Sets the interaction of the current camera to rotation
 */
@SuppressWarnings("serial")
public class SetRotationInteractionAction extends AbstractAction {

    public SetRotationInteractionAction() {
        super("Rotate");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        GL3DCamera cam = Displayer.getViewport().getCamera();
        cam.setCurrentInteraction(cam.getRotateInteraction());
    }

}
