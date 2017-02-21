package org.helioviewer.jhv.gui.dialogs.plugins;

import java.awt.BorderLayout;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.base.plugin.controller.PluginContainer;
import org.helioviewer.jhv.base.plugin.controller.PluginManager;
import org.helioviewer.jhv.base.plugin.interfaces.Plugin;
import org.helioviewer.jhv.gui.components.Buttons;
import org.helioviewer.jhv.gui.dialogs.TextDialog;

/**
 * Visual list entry for each plug-in. Provides functions to enable/disable
 * a plug-in and to display additional information about the plug-in.
 * */
@SuppressWarnings("serial")
class PluginsListEntry extends JPanel implements MouseListener, HyperlinkListener {

    private final PluginContainer plugin;
    private final PluginsList list;

    private final JLabel enableLabel = new JLabel();

    public PluginsListEntry(PluginContainer _plugin, PluginsList _list) {
        plugin = _plugin;
        list = _list;

        JTextPane pane = new JTextPane();
        pane.setContentType("text/html");
        pane.setText("<b>" + plugin.getName() + "</b><br>" + (plugin.getDescription() == null ? "" : plugin.getDescription()) + " <a href=''>More...</a>");
        pane.setEditable(false);
        pane.setOpaque(false);
        pane.addHyperlinkListener(this);
        pane.putClientProperty(JTextPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);

        updateEnableLabel();

        // general
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        add(pane, BorderLayout.LINE_START);
        add(enableLabel, BorderLayout.LINE_END);

        pane.addMouseListener(this);
        enableLabel.addMouseListener(this);
    }

    private void updateEnableLabel() {
        if (plugin.isActive()) {
            enableLabel.setText(Buttons.plugOn);
            enableLabel.setToolTipText("Disable plug-in");
        } else {
            enableLabel.setText(Buttons.plugOff);
            enableLabel.setToolTipText("Enable plug-in");
        }
    }

    private void setPluginActive(boolean active) {
        if (plugin.isActive() == active)
            return;

        plugin.setActive(active);
        plugin.changeSettings();
        PluginManager.getSingletonInstance().saveSettings();

        updateEnableLabel();
        list.fireListChanged();
    }

    @Override
    public void hyperlinkUpdate(HyperlinkEvent e) {
        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            if (e.getURL() == null) {
                Plugin p = plugin.getPlugin();
                String name = p.getName() == null ? "Unknown plug-in name" : p.getName();
                String desc = p.getDescription() == null ? "No description available" : p.getDescription();
                String license = p.getAboutLicenseText() == null ? "Unknown license" : p.getAboutLicenseText();
                String text = "<center><p><big><b>" + name + "</b></big></p><p><b>Plug-in description</b><br>" + desc + "</p><p><b>Plug-in license information</b><br>" + license;
                new TextDialog("About", text, false).showDialog();
            } else {
                JHVGlobals.openURL(e.getURL().toString());
            }
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        list.selectItem(plugin.getName());
        if (e.getSource().equals(enableLabel)) {
            setPluginActive(!plugin.isActive());
        }
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

}
