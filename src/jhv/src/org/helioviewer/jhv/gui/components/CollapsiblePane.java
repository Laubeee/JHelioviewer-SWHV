package org.helioviewer.jhv.gui.components;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import org.helioviewer.jhv.gui.ComponentUtils;
import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.gui.UIGlobals;

/**
 * Panel managing a collapsible area.
 *
 * This panel consists of a toggle button and one arbitrary component. Clicking
 * the toggle button will toggle the visibility of the component.
 */
@SuppressWarnings("serial")
public class CollapsiblePane extends JComponent implements ActionListener {

    private static final ImageIcon expandedIcon = IconBank.getIcon(JHVIcon.DOWN2);
    private static final ImageIcon collapsedIcon = IconBank.getIcon(JHVIcon.RIGHT2);

    final CollapsiblePaneButton toggleButton;
    private final JPanel component;

    public CollapsiblePane(String title, Component managed, boolean startExpanded) {
        setLayout(new BorderLayout());

        toggleButton = new CollapsiblePaneButton(title);
        toggleButton.setBorderPainted(false);
        toggleButton.setHorizontalAlignment(SwingConstants.LEFT);
        toggleButton.setSelected(startExpanded);
        toggleButton.setFont(UIGlobals.UIFontSmallBold);
        if (startExpanded) {
            toggleButton.setIcon(IconBank.getIcon(JHVIcon.DOWN2));
        } else {
            toggleButton.setIcon(IconBank.getIcon(JHVIcon.RIGHT2));
        }
        toggleButton.setPreferredSize(new Dimension(0, UIGlobals.UIFontSmallBold.getSize() + 4));
        toggleButton.addActionListener(this);
        add(toggleButton, BorderLayout.PAGE_START);

        component = new JPanel(new BorderLayout());
        component.add(managed);
        ComponentUtils.setVisible(component, startExpanded);
        add(component, BorderLayout.CENTER);
    }

    public void setTitle(String title) {
        toggleButton.setText(title);
    }

    private void expand() {
        toggleButton.setSelected(true);
        ComponentUtils.setVisible(component, true);
        toggleButton.setIcon(expandedIcon);
    }

    private void collapse() {
        toggleButton.setSelected(false);
        ComponentUtils.setVisible(component, false);
        toggleButton.setIcon(collapsedIcon);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (component.isVisible()) {
            collapse();
        } else {
            expand();
        }
    }

}
