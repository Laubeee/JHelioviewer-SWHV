package org.helioviewer.jhv.plugins.eveplugin.view.linedataselector;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.MatteBorder;

import org.helioviewer.jhv.gui.ComponentUtils;
import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.dialogs.observation.ObservationDialog;
import org.helioviewer.jhv.plugins.eveplugin.EVEPlugin;
import org.helioviewer.jhv.plugins.eveplugin.draw.DrawController;
import org.helioviewer.jhv.plugins.eveplugin.view.linedataselector.cellrenderer.LineColorRenderer;
import org.helioviewer.jhv.plugins.eveplugin.view.linedataselector.cellrenderer.LineDataSelectorElementRenderer;
import org.helioviewer.jhv.plugins.eveplugin.view.linedataselector.cellrenderer.LineDataVisibleCellRenderer;
import org.helioviewer.jhv.plugins.eveplugin.view.linedataselector.cellrenderer.LoadingCellRenderer;
import org.helioviewer.jhv.plugins.eveplugin.view.linedataselector.cellrenderer.RemoveCellRenderer;

@SuppressWarnings("serial")
public class LineDataSelectorTablePanel extends JPanel {

    public static final Border commonBorder = new MatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY);

    private static final int ICON_WIDTH = 12;
    private static final int VISIBLE_COL = 0;
    private static final int TITLE_COL = 1;
    private static final int LOADING_COL = 2;
    private static final int LINECOLOR_COL = 3;
    private static final int REMOVE_COL = 4;

    private final JPanel optionsPanelWrapper;

    public LineDataSelectorTablePanel() {
        setLayout(new GridBagLayout());

        JTable grid = new JTable(EVEPlugin.ldsm) {

            @Override
            public void changeSelection(int rowIndex, int columnIndex, boolean toggle, boolean extend) {
                if (columnIndex == VISIBLE_COL || columnIndex == REMOVE_COL) {
                    // prevent changing selection
                    return;
                }
                super.changeSelection(rowIndex, columnIndex, toggle, extend);
            }

            @Override
            public void clearSelection() {
                // prevent losing selection
            }

        };

        LineDataSelectorModel.addLineDataSelectorModelListener(new LineDataSelectorModelListener() {

            @Override
            public void lineDataAdded(LineDataSelectorElement element) {
                int i = LineDataSelectorModel.getRowIndex(element);
                grid.getSelectionModel().setSelectionInterval(i, i);
            }

            @Override
            public void lineDataRemoved(LineDataSelectorElement element) {
                int i = EVEPlugin.ldsm.getRowCount() - 1;
                if (i >= 0)
                    grid.getSelectionModel().setSelectionInterval(i, i);
            }

            @Override
            public void lineDataVisibility(LineDataSelectorElement element, boolean flag) {
            }

        });

        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.gridy = 0;
        gc.weightx = 1;
        gc.weighty = 0;
        gc.fill = GridBagConstraints.BOTH;

        JScrollPane jsp = new JScrollPane(grid, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        jsp.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY));
        jsp.getViewport().setBackground(Color.WHITE);

        JButton addLayerButton = new JButton("New Layer", IconBank.getIcon(JHVIcon.ADD));
        addLayerButton.addActionListener(e -> ObservationDialog.getInstance().showDialog(true, null, EVEPlugin.op));
        addLayerButton.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        addLayerButton.setHorizontalTextPosition(SwingConstants.TRAILING);
        addLayerButton.setBorderPainted(false);
        addLayerButton.setFocusPainted(false);
        addLayerButton.setContentAreaFilled(false);

        JPanel addLayerButtonWrapper = new JPanel(new BorderLayout());
        addLayerButtonWrapper.add(addLayerButton, BorderLayout.WEST);
        addLayerButtonWrapper.add(DrawController.getOptionsPanel(), BorderLayout.EAST);

        JPanel jspContainer = new JPanel(new BorderLayout());
        jspContainer.add(addLayerButtonWrapper, BorderLayout.CENTER);
        jspContainer.add(jsp, BorderLayout.SOUTH);
        add(jspContainer, gc);

        grid.setTableHeader(null);
        grid.setShowGrid(false);
        grid.setRowSelectionAllowed(true);
        grid.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        grid.setColumnSelectionAllowed(false);
        grid.setIntercellSpacing(new Dimension(0, 0));

        grid.setBackground(Color.white);
        grid.getColumnModel().getColumn(VISIBLE_COL).setCellRenderer(new LineDataVisibleCellRenderer());
        grid.getColumnModel().getColumn(VISIBLE_COL).setPreferredWidth(ICON_WIDTH + 2);
        grid.getColumnModel().getColumn(VISIBLE_COL).setMaxWidth(ICON_WIDTH + 2);

        grid.getColumnModel().getColumn(TITLE_COL).setCellRenderer(new LineDataSelectorElementRenderer());
        // grid.getColumnModel().getColumn(TITLE_COL).setPreferredWidth(80);
        // grid.getColumnModel().getColumn(TITLE_COL).setMaxWidth(80);

        grid.getColumnModel().getColumn(LINECOLOR_COL).setCellRenderer(new LineColorRenderer());
        grid.getColumnModel().getColumn(LINECOLOR_COL).setPreferredWidth(20);
        grid.getColumnModel().getColumn(LINECOLOR_COL).setMaxWidth(20);

        grid.getColumnModel().getColumn(LOADING_COL).setCellRenderer(new LoadingCellRenderer());
        grid.getColumnModel().getColumn(LOADING_COL).setPreferredWidth(20);
        grid.getColumnModel().getColumn(LOADING_COL).setMaxWidth(20);

        grid.getColumnModel().getColumn(REMOVE_COL).setCellRenderer(new RemoveCellRenderer(ICON_WIDTH));
        grid.getColumnModel().getColumn(REMOVE_COL).setPreferredWidth(ICON_WIDTH + 2);
        grid.getColumnModel().getColumn(REMOVE_COL).setMaxWidth(ICON_WIDTH + 2);

        grid.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    handlePopup(e);
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    handlePopup(e);
                }
            }

            /**
             * Handle with right-click menus
             *
             * @param e
             */
            public void handlePopup(MouseEvent e) {
            }

            /**
             * Handle with clicks on hide/show/remove layer icons
             */
            @Override
            public void mouseClicked(MouseEvent e) {
                Point pt = e.getPoint();
                int row = grid.rowAtPoint(pt);
                int col = grid.columnAtPoint(pt);
                if (row < 0 || col < 0)
                    return;

                LineDataSelectorModel model = (LineDataSelectorModel) grid.getModel();

                if (col == VISIBLE_COL) {
                    LineDataSelectorElement lineDataElement = (LineDataSelectorElement) model.getValueAt(row, col);
                    boolean visible = !lineDataElement.isVisible();

                    lineDataElement.setVisibility(visible);
                    LineDataSelectorModel.fireLineDataSelectorElementVisibility(lineDataElement, visible);
                }
                if (col == TITLE_COL || col == LOADING_COL || col == LINECOLOR_COL) {
                    LineDataSelectorElement lineDataElement = (LineDataSelectorElement) model.getValueAt(row, col);
                    setOptionsPanel(lineDataElement);
                }
                if (col == REMOVE_COL) {
                    LineDataSelectorModel.removeRow(row);
                }
                revalidate();
                repaint();
            }
        });
        // grid.setDragEnabled(true);
        // grid.setDropMode(DropMode.INSERT_ROWS);
        // grid.setTransferHandler(new TableRowTransferHandler(grid));

        grid.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                setOptionsPanel((LineDataSelectorElement) grid.getValueAt(grid.getSelectedRow(), 0));
            }
        });

        int h = getGridRowHeight(grid);
        jsp.setPreferredSize(new Dimension(ImageViewerGui.SIDE_PANEL_WIDTH, h * 4 + 1));
        grid.setRowHeight(h);

        optionsPanelWrapper = new JPanel(new BorderLayout());

        gc.gridy = 1;
        add(optionsPanelWrapper, gc);
    }

    private int rowHeight = -1;

    private int getGridRowHeight(JTable table) {
        if (rowHeight == -1) {
            rowHeight = table.getRowHeight() + 4;
        }
        return rowHeight;
    }

    private void setOptionsPanel(LineDataSelectorElement lineDataElement) {
        optionsPanelWrapper.removeAll();
        Component optionsPanel = lineDataElement.getOptionsPanel();
        if (optionsPanel != null) {
            ComponentUtils.setEnabled(optionsPanel, lineDataElement.isVisible());
            optionsPanelWrapper.add(optionsPanel, BorderLayout.CENTER);
        }
        revalidate();
        repaint();
    }

}
