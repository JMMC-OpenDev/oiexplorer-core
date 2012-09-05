/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.oiexplorer.core.gui;

import fr.jmmc.jmcs.gui.component.GenericListModel;
import fr.jmmc.oiexplorer.core.model.OIFitsCollectionEventListener;
import fr.jmmc.oiexplorer.core.model.OIFitsCollectionManager;
import fr.jmmc.oiexplorer.core.model.PlotDefinitionFactory;
import fr.jmmc.oiexplorer.core.model.event.GenericEvent;
import fr.jmmc.oiexplorer.core.model.event.OIFitsCollectionEventType;
import fr.jmmc.oiexplorer.core.model.event.SubsetDefinitionEvent;
import fr.jmmc.oiexplorer.core.model.oi.SubsetDefinition;
import fr.jmmc.oiexplorer.core.model.plot.Axis;
import fr.jmmc.oiexplorer.core.model.plot.PlotDefinition;
import fr.jmmc.oitools.meta.ColumnMeta;
import fr.jmmc.oitools.model.OIFitsFile;
import fr.jmmc.oitools.model.OITable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import javax.swing.JComboBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This Panel allow to select data to plot and (optionnaly in the future) plots them just below.
 * @author mella
 */
public class PlotPanelEditor extends javax.swing.JPanel implements ActionListener,
                                                                   OIFitsCollectionEventListener {

    /** default serial UID for Serializable interface */
    private static final long serialVersionUID = 1;
    /** Logger */
    final private Logger logger = LoggerFactory.getLogger(PlotPanelEditor.class);
    /** Custom plot type */
    private final static String customLabel = "Custom...";

    /* members */
    /** OIFitsCollectionManager singleton */
    private OIFitsCollectionManager ocm = OIFitsCollectionManager.getInstance();
    /** subset definition */
    private SubsetDefinition subsetDefinition = null;
    /** Main plot definition */
    private PlotDefinition plotDefinition;
    /* Swing components */
    /** Store all choices available to plot on x axis given to current data to plot */
    private final List<String> xAxisChoices = new LinkedList<String>();
    /** Store all choices available to plot on y axes given to current data to plot */
    private final List<String> yAxisChoices = new LinkedList<String>();
    /** List of comboboxes associated to y axes */
    private final List<JComboBox> yComboBoxes = new LinkedList<JComboBox>();
    /** last selected value on x axis */
    private String lastXComboBoxValue = null;
    /** list of last selected value on y axes */
    private List<String> lastYComboBoxesValues = new LinkedList<String>();
    /** Common listener for y comboboxes */
    private ActionListener ycomboActionListener;

    /** Creates new form PlotPanelEditor */
    public PlotPanelEditor() {
        OIFitsCollectionManager.getInstance().getSubsetDefinitionEventNotifier().register(this);

        initComponents();

        // TODO REMOVE next code line 
        plotAreaPanel.add(new Vis2Panel());
    }

    /** 
     * Init function that must be called after being instanciated and when notification can be performed. 
     */
    public void init() {

        // Comboboxes
        xAxisComboBox.setModel(new GenericListModel<String>(xAxisChoices, true));

        final List<String> plotTypeChoices = new LinkedList<String>();

        plotTypeChoices.addAll(PlotDefinitionFactory.getInstance().getDefaultList());
        plotTypeChoices.add(customLabel);

        plotTypeComboBox.setModel(new GenericListModel<String>(plotTypeChoices, true));
        plotTypeComboBox.setSelectedItem(PlotDefinitionFactory.PLOT_DEFAULT);
        plotTypeComboBox.addActionListener(this);

        // Prepare a common listener to group handling in yAxisComboBoxActionPerformed()
        ycomboActionListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                yAxisComboBoxActionPerformed(e);
            }
        };

        // fill first y axis combobox
        addYAxisButtonActionPerformed(null);
    }

    /**
     * Update widgets and the OIFits structure
     * and request the plotDefinition to be notified if present or modified.
     * @param subsetDefinition subset definition
     */
    public void update(final SubsetDefinition subsetDefinition) {
        logger.warn("update: subset target {}", subsetDefinition.getTarget());

        /* store subset definition */
        this.subsetDefinition = subsetDefinition;

        /* use the manager current plot definition  */
        this.plotDefinition = ocm.getCurrentPlotDefinition();

        /* fill combobox for available columns */
        fillDataSelectors();

        // update GUI  
        actionPerformed(null);
    }

    /** 
     * Return the plotDefinition initialized with user adjusted widgets. 
     */
    private PlotDefinition defineCustomPlotDefinition() {
        // handle xAxis
        Axis xAxis = new Axis();
        xAxis.setName(getxAxis());
        plotDefinition.setXAxis(xAxis);

        // handle yAxes
        List<Axis> yAxes = plotDefinition.getYAxes();
        yAxes.clear();
        for (String yname : getyAxes()) {
            Axis a = new Axis();
            a.setName(yname);
            yAxes.add(a);
        }

        logger.debug("Setting custom plot definition x: {}, y : {}", getxAxis(), getyAxes());
        return plotDefinition;
    }

    /**
     * Fill axes comboboxes with all distinct columns present in the available
     * tables.
     */
    private void fillDataSelectors() {

        // Clear all content
        xAxisChoices.clear();
        yAxisChoices.clear();

        if (subsetDefinition != null && subsetDefinition.getOIFitsSubset() != null) {
            // Get whole available columns
            final Set<String> columns = getDistinctColumns(subsetDefinition.getOIFitsSubset());

            xAxisChoices.addAll(columns);
            yAxisChoices.addAll(columns);
        }

        if (!xAxisChoices.isEmpty()) {
            if (lastXComboBoxValue != null && xAxisChoices.contains(lastXComboBoxValue)) {
                xAxisComboBox.setSelectedItem(lastXComboBoxValue);
            } else {
                xAxisComboBox.setSelectedIndex(0);
            }
        }
        if (!yAxisChoices.isEmpty()) {
            for (int i = 0, len = yComboBoxes.size(); i < len; i++) {
                if (lastYComboBoxesValues.size() > i && yAxisChoices.contains(lastYComboBoxesValues.get(i))) {
                    yComboBoxes.get(i).setSelectedItem(lastYComboBoxesValues.get(i));
                } else {
                    yComboBoxes.get(i).setSelectedIndex(0);
                }
            }
        }
    }

    /**
     * Fill Y axes comboboxes with all distinct columns present in the available
     * tables compatibles with the selected x axis.
     */
    private void fillyDataSelectors() {
        // Clear all content       
        yAxisChoices.clear();

        if (subsetDefinition != null && subsetDefinition.getOIFitsSubset() != null) {
            // Get whole available columns compatible with selected x
            final Set<String> columns = getDistinctColumns(subsetDefinition.getOIFitsSubset(), getxAxis());

            yAxisChoices.addAll(columns);
        }

        if (!yAxisChoices.isEmpty()) {
            for (int i = 0, len = yComboBoxes.size(); i < len; i++) {
                if (lastYComboBoxesValues.size() > i && yAxisChoices.contains(lastYComboBoxesValues.get(i))) {
                    yComboBoxes.get(i).setSelectedItem(lastYComboBoxesValues.get(i));
                } else {
                    yComboBoxes.get(i).setSelectedIndex(0);
                }
            }
        }
    }

    private String getxAxis() {
        lastXComboBoxValue = (String) xAxisComboBox.getSelectedItem();
        return lastXComboBoxValue;
    }

    private List<String> getyAxes() {
        lastYComboBoxesValues.clear();

        for (int i = 0, len = yComboBoxes.size(); i < len; i++) {
            lastYComboBoxesValues.add((String) yComboBoxes.get(i).getSelectedItem());
        }
        return lastYComboBoxesValues;
    }

    /** Return the set of distinct columns available in the table of given OIFitsFile.
     * @param oiFitsFile oifitsFile to search data into
     * @return a Set of Strings with every distinct column names
     */
    private Set<String> getDistinctColumns(final OIFitsFile oiFitsFile) {
        final Set<String> columns = new LinkedHashSet<String>();


        // Add every column of every tables for given target into combomodel sets
        // TODO optimization could be operated walking only on the first element
        for (OITable oiTable : oiFitsFile.getOiVis2()) {
            oiTable.getNumericalColumnsNames(columns);
        }
        for (OITable oiTable : oiFitsFile.getOiVis()) {
            oiTable.getNumericalColumnsNames(columns);
        }
        for (OITable oiTable : oiFitsFile.getOiT3()) {
            oiTable.getNumericalColumnsNames(columns);
        }
        return columns;
    }

    /** Return the set of distinct columns from the tables of given 
     * OIFitsFile and compatible with given column.
     * @param oiFitsFile oifitsFile to search data into
     * @return a Set of Strings with every distinct column names
     */
    private Set<String> getDistinctColumns(final OIFitsFile oiFitsFile, String columnName) {
        final Set<String> columns = new LinkedHashSet<String>();

        // TODO see previous getDistinctColumns() for perf note
        // Add every column of every tables for given target into combomodel sets
        for (OITable oiTable : oiFitsFile.getOiVis2()) {
            ColumnMeta meta = oiTable.getColumnMeta(columnName);
            if (meta != null) {
                oiTable.getNumericalColumnsNames(columns);
            } else {
                logger.warn("Can't use data from '{}' table with column '{}'", oiTable, columnName);
            }
        }
        for (OITable oiTable : oiFitsFile.getOiVis()) {
            ColumnMeta meta = oiTable.getColumnMeta(columnName);
            if (meta != null) {
                oiTable.getNumericalColumnsNames(columns);
            } else {
                logger.warn("Can't use data from '{}' table with column '{}'", oiTable, columnName);
            }
        }
        for (OITable oiTable : oiFitsFile.getOiT3()) {
            ColumnMeta meta = oiTable.getColumnMeta(columnName);
            if (meta != null) {
                oiTable.getNumericalColumnsNames(columns);
            } else {
                logger.warn("Can't use data from '{}' table with column '{}'", oiTable, columnName);
            }
        }
        return columns;
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        plotTypeLabel = new javax.swing.JLabel();
        plotTypeComboBox = new javax.swing.JComboBox();
        yLabel = new javax.swing.JLabel();
        xLabel = new javax.swing.JLabel();
        xAxisComboBox = new javax.swing.JComboBox();
        addYAxisButton = new javax.swing.JButton();
        delYAxisButton = new javax.swing.JButton();
        plotAreaPanel = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        yComboBoxesPanel = new javax.swing.JPanel();

        setLayout(new java.awt.GridBagLayout());

        plotTypeLabel.setText("Plot type");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        gridBagConstraints.insets = new java.awt.Insets(0, 2, 0, 2);
        add(plotTypeLabel, gridBagConstraints);

        plotTypeComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                plotTypeComboBoxActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        add(plotTypeComboBox, gridBagConstraints);

        yLabel.setText("yAxis");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 2);
        add(yLabel, gridBagConstraints);

        xLabel.setText("xAxis");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 2);
        add(xLabel, gridBagConstraints);

        xAxisComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                xAxisComboBoxActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        add(xAxisComboBox, gridBagConstraints);

        addYAxisButton.setText("+");
        addYAxisButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addYAxisButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.PAGE_START;
        add(addYAxisButton, gridBagConstraints);

        delYAxisButton.setText("-");
        delYAxisButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                delYAxisButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.PAGE_START;
        add(delYAxisButton, gridBagConstraints);

        plotAreaPanel.setLayout(new java.awt.BorderLayout());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        add(plotAreaPanel, gridBagConstraints);

        yComboBoxesPanel.setLayout(new javax.swing.BoxLayout(yComboBoxesPanel, javax.swing.BoxLayout.Y_AXIS));
        jScrollPane1.setViewportView(yComboBoxesPanel);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        add(jScrollPane1, gridBagConstraints);
    }// </editor-fold>//GEN-END:initComponents

    private void xAxisComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_xAxisComboBoxActionPerformed
        // Reduce (Update) y combo list with compatible selection
        fillyDataSelectors();
        actionPerformed(null);
    }//GEN-LAST:event_xAxisComboBoxActionPerformed

    private void addYAxisButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addYAxisButtonActionPerformed
        final JComboBox ycombo = new JComboBox(new GenericListModel<String>(yAxisChoices, true));
        addYCombo(ycombo);
    }//GEN-LAST:event_addYAxisButtonActionPerformed

    private void delYAxisButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_delYAxisButtonActionPerformed
        if (yComboBoxes.size() > 1) {
            JComboBox ycombo = yComboBoxes.get(yComboBoxes.size() - 1);
            // TODO replace by removal of the last yCombobox which one has lost the foxus
            delYCombo(ycombo);
        }
    }//GEN-LAST:event_delYAxisButtonActionPerformed

    private void plotTypeComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_plotTypeComboBoxActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_plotTypeComboBoxActionPerformed

    private void yAxisComboBoxActionPerformed(java.awt.event.ActionEvent evt) {
        actionPerformed(null);
    }

    /** Synchronize management for the addition of a given combo and update GUI.
     * @param ycombo ComboBox to add
     */
    private void addYCombo(final JComboBox ycombo) {
        final String selectedType = (String) plotTypeComboBox.getSelectedItem();
        final boolean useCustom = (selectedType == customLabel);

        /* TODO move following lines */
        ycombo.setEnabled(useCustom);
        yComboBoxes.add(ycombo);
        yComboBoxesPanel.add(ycombo);
        ycombo.addActionListener(ycomboActionListener);
        revalidate();
    }

    /** Synchronize management for the addition of a given combo and update GUI. 
     * @param ycombo ComboBox to remove
     */
    private void delYCombo(final JComboBox ycombo) {
        yComboBoxes.remove(ycombo);
        ycombo.removeActionListener(ycomboActionListener);
        yComboBoxesPanel.remove(ycombo);
        revalidate();
    }

    public void actionPerformed(ActionEvent e) {

        final String selectedType = (String) plotTypeComboBox.getSelectedItem();
        final boolean useCustom = (selectedType == customLabel);

        /* TODO move following lines */
        xAxisComboBox.setEnabled(useCustom);
        for (int i = 0, len = yComboBoxes.size(); i < len; i++) {
            yComboBoxes.get(i).setEnabled(useCustom);
        }
        xLabel.setEnabled(useCustom);
        yLabel.setEnabled(useCustom);

        if (useCustom) {
            plotDefinition = defineCustomPlotDefinition();
            logger.warn("Using custom plot {}", plotDefinition);
        } else {
            plotDefinition = PlotDefinitionFactory.getInstance().getDefault(selectedType);
            logger.warn("Using preset plot : {}, {}", selectedType, plotDefinition);
        }

        ocm.updatePlotDefinition(this, plotDefinition);
    }

    /**
     * Handle the given OIFits collection event
     * @param event OIFits collection event
     */
    @Override
    public void onProcess(final GenericEvent<OIFitsCollectionEventType> event) {
        logger.debug("Received event to process {}", event);

        switch (event.getType()) {
            case SUBSET_CHANGED:
                update(((SubsetDefinitionEvent) event).getSubsetDefinition());
                break;
            default:
        }
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addYAxisButton;
    private javax.swing.JButton delYAxisButton;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JPanel plotAreaPanel;
    private javax.swing.JComboBox plotTypeComboBox;
    private javax.swing.JLabel plotTypeLabel;
    private javax.swing.JComboBox xAxisComboBox;
    private javax.swing.JLabel xLabel;
    private javax.swing.JPanel yComboBoxesPanel;
    private javax.swing.JLabel yLabel;
    // End of variables declaration//GEN-END:variables

    // TODO remove it
    // still kept for export pdf action
    public Vis2Panel getPlotPanel() {
        return (Vis2Panel) plotAreaPanel.getComponent(0);
    }
}
