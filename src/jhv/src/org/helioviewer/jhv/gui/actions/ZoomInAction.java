package org.helioviewer.jhv.gui.actions;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.input.KeyShortcuts;

@SuppressWarnings("serial")
public class ZoomInAction extends AbstractAction {

    public ZoomInAction(boolean small, boolean useIcon) {
        super("Zoom In", useIcon ? IconBank.getIcon(small ? JHVIcon.ZOOM_IN_SMALL : JHVIcon.ZOOM_IN) : null);
        putValue(SHORT_DESCRIPTION, "Zoom in");

        KeyStroke key = KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
        putValue(ACCELERATOR_KEY, key);
        KeyShortcuts.registerKey(key, this);
    }

    @Override
    public void actionPerformed(ActionEvent arg0) {
        Displayer.getCamera().zoom(-1 * Displayer.CAMERA_ZOOM_MULTIPLIER_BUTTON);
        Displayer.render(1);
    }

}
