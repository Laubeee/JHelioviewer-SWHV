package org.helioviewer.jhv.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.KeyEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.interfaces.ShowableDialog;

@SuppressWarnings("serial")
public class NewVersionDialog extends JDialog implements ShowableDialog {

    // setting for check.update.next
    private int nextCheck = 0;
    // suspended startups when clicked remindMeLater
    private static final int suspendedStarts = 5;

    private final JTextPane messagePane = new JTextPane();

    public NewVersionDialog(boolean verbose) {
        super(ImageViewerGui.getMainFrame(), false);
        setLayout(new BorderLayout());
        setResizable(false);
        setTitle("New Version Available");

        messagePane.setContentType("text/html");
        messagePane.setOpaque(false);
        messagePane.setEditable(false);
        messagePane.addHyperlinkListener(JHVGlobals.hyperOpenURL);
        messagePane.putClientProperty(JTextPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        messagePane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        add(messagePane, BorderLayout.CENTER);

        JPanel closeButtonContainer = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton closeButton = new JButton("Close");
        closeButtonContainer.add(closeButton);
        closeButton.addActionListener(e -> setVisible(false));

        if (!verbose) {
            JButton laterButton = new JButton("Remind me later");
            closeButtonContainer.add(laterButton);
            laterButton.addActionListener(e -> {
                setVisible(false);
                nextCheck = suspendedStarts;
            });
        }

        JButton downloadButton = new JButton("Download");
        downloadButton.addActionListener(e -> {
            setVisible(false);
            JHVGlobals.openURL(JHVGlobals.downloadURL);
        });
        closeButtonContainer.add(downloadButton);
        add(closeButtonContainer, BorderLayout.SOUTH);

        getRootPane().registerKeyboardAction(e -> setVisible(false), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        getRootPane().setDefaultButton(downloadButton);
        getRootPane().setFocusable(true);
    }

    public void init(String message) {
        messagePane.setText(message);
    }

    @Override
    public void showDialog() {
        pack();
        setLocationRelativeTo(ImageViewerGui.getMainFrame());
        setVisible(true);
    }

    public int getNextCheck() {
        return nextCheck;
    }

}
