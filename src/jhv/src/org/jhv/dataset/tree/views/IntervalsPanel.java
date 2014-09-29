package org.jhv.dataset.tree.views;

import java.awt.BorderLayout;

import javax.swing.JLabel;

import org.jhv.dataset.tree.models.DatasetIntervals;

public class IntervalsPanel extends DatasetPanel {

    private static final long serialVersionUID = -4980121173310259804L;

    DatasetIntervals model;

    public IntervalsPanel(DatasetIntervals model) {
        super();
        this.model = model;
        setLayout(new BorderLayout());

        JLabel label = new JLabel("All layers");
        label.setOpaque(true);
        add(label, BorderLayout.CENTER);

    }
}
