/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.oiexplorer.core.gui;

import fr.jmmc.jmcs.gui.component.Disposable;
import fr.jmmc.jmcs.gui.component.GenericListModel;
import fr.jmmc.jmcs.gui.util.ResourceImage;
import fr.jmmc.jmcs.gui.util.SwingUtils;
import fr.jmmc.jmcs.gui.util.SwingUtils.ComponentSizeVariant;
import fr.jmmc.oiexplorer.core.function.Converter;
import fr.jmmc.oiexplorer.core.function.ConverterFactory;
import fr.jmmc.oiexplorer.core.model.OIFitsCollectionManager;
import fr.jmmc.oiexplorer.core.model.oi.GenericFilter;
import fr.jmmc.oiexplorer.core.model.plot.Range;
import static fr.jmmc.oitools.OIFitsConstants.COLUMN_EFF_WAVE;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * GUI Editor for a Generic Filter. Uses some swing widgets and RangeEditor. Handles the sync between the edited generic
 * filter and the GUI. Can notify a ChangeListener.
 */
public final class GenericFilterEditor extends javax.swing.JPanel
        implements Disposable, ChangeListener, ListSelectionListener, ActionListener {

    /** default serial UID for Serializable interface */
    private static final long serialVersionUID = 1;

    /** Logger */
    private static final Logger logger = LoggerFactory.getLogger(GenericFilterEditor.class);

    private static final ConverterFactory CONVERTER_FACTORY = ConverterFactory.getInstance();

    /** OIFitsCollectionManager singleton reference. used for reset button. */
    private final static OIFitsCollectionManager ocm = OIFitsCollectionManager.getInstance();

    /** Map giving a list of predefined ranges for a column name. Some column name don't have predefined ranges and
     * receive a null value. TODO: move this to RangeEditor ?
     */
    private static final Map<String, Map<String, double[]>> predefinedRangesByColumnName;

    static {
        predefinedRangesByColumnName = new HashMap<>(8);
        predefinedRangesByColumnName.put(COLUMN_EFF_WAVE, RangeEditor.EFF_WAVE_PREDEFINED_RANGES);
    }

    private static final String BUTTON_PROP_INDEX = "rangeIndex";

    // members:
    /* related GenericFilter */
    private transient GenericFilter genericFilter;
    /* associated Range editors */
    private final transient List<RangeEditor> rangeEditors = new ArrayList<>();

    /* for DataType.STRING: list of checkboxes for accepted values */
    private final transient GenericListModel<String> checkBoxListValuesModel;

    /** converter associated to the column name of the generic filter. It allows us to make the range editor use a
     * different unit more user friendly. */
    private transient Converter converter;

    /**
     * updatingGUI true disables the handlers and the listeners on the widgets. useful when updating the widgets
     */
    private boolean updatingGUI;

    /** Creates new form GenericFilterEditor */
    public GenericFilterEditor() {
        initComponents();
        this.checkBoxListValuesModel = new GenericListModel<String>(new ArrayList<>(16));
        postInit();
    }

    /**
     * This method is useful to set the models and specific features of initialized swing components :
     */
    private void postInit() {
        checkBoxListValues.setModel(checkBoxListValuesModel);
        checkBoxListValues.getCheckBoxListSelectionModel().addListSelectionListener(this);

        updatingGUI = false;

        // use small variant:
        SwingUtils.adjustSize(this.jCheckBoxExclusive, ComponentSizeVariant.small);
        SwingUtils.adjustSize(this.jButtonReset, ComponentSizeVariant.small);

        // update button UI:
        SwingUtilities.updateComponentTreeUI(this);
    }

    @Override
    public void dispose() {
        genericFilter = null;
        for (ChangeListener listener : getChangeListeners()) {
            removeChangeListener(listener);
        }
        rangeEditors.forEach(RangeEditor::dispose);
        checkBoxListValues.getCheckBoxListSelectionModel().removeListSelectionListener(this);
    }

    public void setGenericFilter(final GenericFilter genericFilter) {
        this.genericFilter = genericFilter;

        try {
            updatingGUI = true;

            rangeEditors.forEach(RangeEditor::dispose);
            rangeEditors.clear();
            jPanelRanges.removeAll();
            checkBoxListValuesModel.clear();
            checkBoxListValues.clearCheckBoxListSelection();
            jPanelRangesOrValues.removeAll();

            final String columnName = genericFilter.getColumnName();

            // Set column name as checkbox's label:
            jCheckBoxEnabled.setSelected(genericFilter.isEnabled());
            jCheckBoxEnabled.setText(columnName);

            // Set not:
            jCheckBoxExclusive.setSelected(!genericFilter.isInclusive());
            jCheckBoxExclusive.setEnabled(genericFilter.isEnabled());
            jButtonReset.setEnabled(genericFilter.isEnabled());

            switch (genericFilter.getDataType()) {
                case NUMERIC:
                    converter = CONVERTER_FACTORY.getDefault(CONVERTER_FACTORY.getDefaultByColumn(columnName));

                    int rangeIndex = 0;
                    for (Range range : genericFilter.getAcceptedRanges()) {
                        newRangeEditor(range, rangeIndex++);
                    }

                    jPanelRangesOrValues.add(jPanelRanges);
                    break;
                case STRING:
                    // generating check box list possible values
                    final List<String> initValues = ocm.getOIFitsCollection().getDistinctValues(columnName);
                    if (initValues != null) {
                        checkBoxListValuesModel.addAll(initValues);
                    }

                    // selecting values in the check box list
                    for (String value : genericFilter.getAcceptedValues()) {
                        if (!checkBoxListValuesModel.contains(value)) {
                            // in case the generic filter has a selected value that does not
                            // exist in the checkboxlist alternatives, we add it
                            checkBoxListValuesModel.add(value);
                        }
                        checkBoxListValues.addCheckBoxListSelectedValue(value, false);
                    }

                    jPanelRangesOrValues.add(jScrollPaneValues);
                    break;
                default:
            }
        } finally {
            revalidate();
            updatingGUI = false;
        }
    }

    public GenericFilter getGenericFilter() {
        return genericFilter;
    }

    // private utility to create a Range Editor for a Range. handles conversion, and delete button with grid bag layout.
    private void newRangeEditor(final Range modelRange, final int rangeIndex) {
        final String columnName = this.genericFilter.getColumnName();

        final RangeEditor rangeEditor = new RangeEditor();
        rangeEditor.setAlias(columnName);
        rangeEditor.addChangeListener(this);

        // we create a new Range so it can have a different unit:
        final Range guiRange = (Range) modelRange.clone();
        final double[] convertedMinMax = convertRangeForGUI(modelRange.getMin(), modelRange.getMax());
        guiRange.setMin(convertedMinMax[0]);
        guiRange.setMax(convertedMinMax[1]);

        rangeEditor.setRange(guiRange);
        rangeEditor.updateRange(guiRange, true);
        rangeEditor.updateRangeList(predefinedRangesByColumnName.get(columnName));
        rangeEditor.setEnabled(genericFilter.isEnabled());
        rangeEditors.add(rangeEditor);

        // adding to GUI with add/delete button
        final JPanel panel = new JPanel(new GridBagLayout());
        final GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 0.9;
        panel.add(rangeEditor, gridBagConstraints);

        final JButton button = new JButton();
        button.setIcon((rangeIndex == 0) ? ResourceImage.LIST_ADD.icon() : ResourceImage.LIST_DEL.icon());
        button.setActionCommand((rangeIndex == 0) ? "add" : "del");
        button.setToolTipText((rangeIndex == 0) ? "Add another range editor" : "Delete this range editor");
        button.putClientProperty(BUTTON_PROP_INDEX, rangeIndex);
        button.addActionListener(this);
        button.setEnabled(genericFilter.isEnabled());

        // use small variant:
        SwingUtils.adjustSize(button, ComponentSizeVariant.small);

        gridBagConstraints.gridx = 1;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.weightx = 0;
        gridBagConstraints.insets = new Insets(0, 2, 0, 2);
        panel.add(button, gridBagConstraints);
        // method `removeRange` relies on the fact that `button` is the component with index 1 in `panel`

        jPanelRanges.add(panel);
        // update button UI:
        SwingUtilities.updateComponentTreeUI(jPanelRanges);
    }

    /**
     * Adds a new range. Uses OCM for initial values. Adds the new range editor to `jPanelRanges` and to `rangeEditors`.
     * Adds the new range to `genericFilter`.
     * You should call `fireStateChanged` after calling this function.
     */
    private void addNewRange() {
        // getting initial values for new range
        double[] minMax = getInitialRangeOCM();

        final Range range = new Range();
        range.setMin(minMax[0]);
        range.setMax(minMax[1]);

        // add range to generic filter
        genericFilter.getAcceptedRanges().add(range);

        // add converted range editor to `jPanelRanges` and `rangeEditors`
        final int rangeIndex = rangeEditors.size();
        newRangeEditor(range, rangeIndex);
    }

    /**
     * Removes a range. Deletes the range editor from `jPanelRanges` and from `rangeEditors`.
     * Deletes the corresponding range in `genericFilter̀r`.
     * You should call `fireStateChanged` after calling this function.
     * @param rangeIndex index of range to delete
     */
    private void removeRange(final int rangeIndex) {
        if ((rangeIndex < 0) || (rangeIndex >= rangeEditors.size())) {
            logger.error("Index out of bounds: attempting to delete range {}/{}", rangeIndex, rangeEditors.size());
            return;
        }
        if (rangeIndex == 0) {
            logger.error("Cannot delete the first range.");
            return;
        }

        RangeEditor rangeEditorToDel = rangeEditors.get(rangeIndex);

        // remove from `jPanelRanges`
        jPanelRanges.remove(rangeIndex);

        // remove range editor from the list
        rangeEditorToDel.dispose();
        rangeEditors.remove(rangeEditorToDel);

        // remove range from generic filter
        genericFilter.getAcceptedRanges().remove(rangeIndex);

        // updating index in all other buttons:
        final Component[] components = jPanelRanges.getComponents();

        for (int i = rangeIndex; i < components.length; i++) {
            final JPanel panel = (JPanel) components[i];
            final JButton button = (JButton) panel.getComponent(1);
            button.putClientProperty(BUTTON_PROP_INDEX, i); // update index
        }
    }

    /**
     * @return initial min and max range values from OCM, depending on column name and current subset definition.
     */
    private double[] getInitialRangeOCM() {
        double[] minmax = new double[2];

        final fr.jmmc.oitools.model.range.Range oitoolsRange
                                                = ocm.getOIFitsCollection().getColumnRange(genericFilter.getColumnName());

        minmax[0] = Double.isFinite(oitoolsRange.getMin()) ? oitoolsRange.getMin() : Double.NaN;
        minmax[1] = Double.isFinite(oitoolsRange.getMax()) ? oitoolsRange.getMax() : Double.NaN;

        return minmax;
    }

    /* Converts a model's range to another unit that is more GUI friendly. */
    private double[] convertRangeForGUI(final double min, final double max) {
        final double[] convertedMinMax = {min, max};
        if (converter != null) {
            try {
                convertedMinMax[0] = converter.evaluate(min);
                convertedMinMax[1] = converter.evaluate(max);
            } catch (IllegalArgumentException iae) {
                logger.error("conversion failed: ", iae);
            }
        }
        return convertedMinMax;
    }

    /* Inverse of convertRangeForGUI. */
    private double[] convertRangeForModel(final double min, final double max) {
        final double[] convertedMinMax = {min, max};
        if (converter != null) {
            try {
                convertedMinMax[0] = converter.invert(min);
                convertedMinMax[1] = converter.invert(max);
            } catch (IllegalArgumentException iae) {
                logger.error("conversion failed: ", iae);
            }
        }
        return convertedMinMax;
    }

    /**
     * handler for the checkbox enabling or disabling the generic filter.
     */
    private void handleEnabled() {
        if (!updatingGUI && genericFilter != null) {
            final boolean enabled = jCheckBoxEnabled.isSelected();
            genericFilter.setEnabled(enabled);

            jCheckBoxExclusive.setEnabled(enabled);
            jButtonReset.setEnabled(enabled);

            for (RangeEditor rangeEditor : rangeEditors) {
                rangeEditor.setEnabled(enabled);
            }

            checkBoxListValues.setEnabled(enabled);

            // updating all other buttons
            for (Component component : jPanelRanges.getComponents()) {
                final JPanel panel = (JPanel) component;
                final JButton button = (JButton) panel.getComponent(1);
                button.setEnabled(enabled);
            }

            fireStateChanged();
        }
    }

    /**
     * handler for the checkbox enabling or disabling the generic filter.
     */
    private void handleNot() {
        if (!updatingGUI && genericFilter != null) {
            final boolean exclusive = jCheckBoxExclusive.isSelected();
            genericFilter.setNot(exclusive ? Boolean.TRUE : null);

            fireStateChanged();
        }
    }

    /**
     * handler for the reset button. For DataType.NUMERIC, removes all but first range, and sets first range (if any) to
     * current initial values for this column. For DataType.STRING, resets all choices to current initial values, and
     * selects every choice.
     */
    private void handleReset() {
        if (!updatingGUI && genericFilter != null) {
            final String columnName = genericFilter.getColumnName();

            switch (genericFilter.getDataType()) {
                case NUMERIC:
                    // removing all but first range (if any)
                    for (int i = 1, s = genericFilter.getAcceptedRanges().size(); i < s; i++) {
                        removeRange(1); // always the number 1, because indexes change after every remove
                    }

                    // updating generic filter (that has zero or one range)
                    final double[] minMax = getInitialRangeOCM();
                    for (Range range : genericFilter.getAcceptedRanges()) {
                        range.setMin(minMax[0]);
                        range.setMax(minMax[1]);
                    }

                    // updating range editors with converted new min and max
                    double[] convertedMinMax = convertRangeForGUI(minMax[0], minMax[1]);
                    for (RangeEditor rangeEditor : rangeEditors) {
                        rangeEditor.setRangeFieldValues(convertedMinMax[0], convertedMinMax[1]);
                    }
                    break;

                case STRING:
                    // update gui widget
                    checkBoxListValuesModel.clear();
                    final List<String> initValues = ocm.getOIFitsCollection().getDistinctValues(columnName);
                    if (initValues != null) {
                        checkBoxListValuesModel.addAll(initValues);
                    }
                    checkBoxListValues.selectAll();

                    // update generic filter
                    genericFilter.getAcceptedValues().clear();
                    if (initValues != null) {
                        genericFilter.getAcceptedValues().addAll(initValues);
                    }
                    break;
                default:
            }

            repaint();
            fireStateChanged();
        }
    }

    /** handler for changes in range editors.
     *
     * @param ce Event */
    @Override
    public void stateChanged(ChangeEvent ce) {
        if (!updatingGUI) {

            // some rangeEditor has changed. we need to update the corresponding range in genericFilter
            // and we need to convert the unit
            final RangeEditor rangeEditorChanged = (RangeEditor) ce.getSource();
            final Range guiRangeChanged = rangeEditorChanged.getRange();

            int index = 0;
            for (RangeEditor rangeEditor : rangeEditors) {
                if (rangeEditor == rangeEditorChanged) {

                    // update generic filter's range with modified range, but convert values to model's unit
                    final Range modelRangeToChange = genericFilter.getAcceptedRanges().get(index);
                    modelRangeToChange.copy(guiRangeChanged);
                    final double[] convertedMinMax
                                   = convertRangeForModel(guiRangeChanged.getMin(), guiRangeChanged.getMax());
                    modelRangeToChange.setMin(convertedMinMax[0]);
                    modelRangeToChange.setMax(convertedMinMax[1]);

                    break;
                }
                index++;
            }

            fireStateChanged();
        }
    }

    /** handler for changes in check box list values.
     *
     * @param lse Event
     */
    @Override
    public void valueChanged(ListSelectionEvent lse) {
        if (!updatingGUI && genericFilter != null) {
            final List<String> genericFilterValues = genericFilter.getAcceptedValues();
            genericFilterValues.clear();
            for (Object selected : checkBoxListValues.getCheckBoxListSelectedValues()) {
                genericFilterValues.add((String) selected);
            }
            fireStateChanged();
        }
    }

    /** handler for clicks on "-" and "+" buttons (for ranges).
     * @param event event
     */
    @Override
    public void actionPerformed(ActionEvent event) {
        if (!updatingGUI && genericFilter != null) {
            if (event.getSource() instanceof JButton) {
                final JButton button = (JButton) event.getSource();
                final String cmd = button.getActionCommand();
                switch ((cmd == null) ? "" : cmd) {
                    case "add":
                        addNewRange();
                        fireStateChanged();
                        break;
                    case "del": {
                        final Object idx = button.getClientProperty(BUTTON_PROP_INDEX);
                        if (idx instanceof Integer) {
                            final int rangeIndex = (Integer) idx;
                            removeRange(rangeIndex);
                            fireStateChanged();
                        }
                    }
                    break;
                    default:
                        logger.info("Action on a button with unknown actionCommand '{}'", cmd);
                        break;
                }
            }
        }
    }

    /**
     * Listen to changes to Range.
     *
     * @param listener listener to notify when changes occur
     */
    public void addChangeListener(ChangeListener listener) {
        listenerList.add(ChangeListener.class, listener);
    }

    /**
     * Stop listening to changes to Range.
     *
     * @param listener to stop notifying when changes occur
     */
    public void removeChangeListener(ChangeListener listener) {
        listenerList.remove(ChangeListener.class, listener);
    }

    /**
     * @return the list of ChangeListeners
     */
    private ChangeListener[] getChangeListeners() {
        return listenerList.getListeners(ChangeListener.class);
    }

    /**
     * Notify listeners that changes occured to Range
     */
    void fireStateChanged() {
        final ChangeEvent event = new ChangeEvent(this);
        for (ChangeListener changeListener : getChangeListeners()) {
            changeListener.stateChanged(event);
        }
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The
     * content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        jPanelRanges = new javax.swing.JPanel();
        jScrollPaneValues = new javax.swing.JScrollPane();
        checkBoxListValues = new com.jidesoft.swing.CheckBoxList();
        jCheckBoxEnabled = new javax.swing.JCheckBox();
        jCheckBoxExclusive = new javax.swing.JCheckBox();
        jPanelRangesOrValues = new javax.swing.JPanel();
        jButtonReset = new javax.swing.JButton();

        jPanelRanges.setLayout(new javax.swing.BoxLayout(jPanelRanges, javax.swing.BoxLayout.Y_AXIS));

        jScrollPaneValues.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

        checkBoxListValues.setClickInCheckBoxOnly(false);
        checkBoxListValues.setPrototypeCellValue("XX-XX-XX");
        checkBoxListValues.setVisibleRowCount(4);
        jScrollPaneValues.setViewportView(checkBoxListValues);

        setBorder(javax.swing.BorderFactory.createEtchedBorder());
        setLayout(new java.awt.GridBagLayout());

        jCheckBoxEnabled.setSelected(true);
        jCheckBoxEnabled.setText("COLUMN NAME");
        jCheckBoxEnabled.setToolTipText("enable / disable filter");
        jCheckBoxEnabled.setMargin(new java.awt.Insets(0, 0, 0, 0));
        jCheckBoxEnabled.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxEnabledActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.weightx = 0.1;
        gridBagConstraints.insets = new java.awt.Insets(0, 2, 0, 0);
        add(jCheckBoxEnabled, gridBagConstraints);

        jCheckBoxExclusive.setText("X");
        jCheckBoxExclusive.setToolTipText("If checked, the filter is exclusive; inclusive otherwise");
        jCheckBoxExclusive.setMargin(new java.awt.Insets(0, 0, 0, 0));
        jCheckBoxExclusive.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxExclusiveActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(0, 6, 0, 0);
        add(jCheckBoxExclusive, gridBagConstraints);

        jPanelRangesOrValues.setLayout(new javax.swing.BoxLayout(jPanelRangesOrValues, javax.swing.BoxLayout.PAGE_AXIS));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(1, 2, 1, 2);
        add(jPanelRangesOrValues, gridBagConstraints);

        jButtonReset.setIcon(fr.jmmc.jmcs.gui.util.ResourceImage.REFRESH_ICON.icon());
        jButtonReset.setToolTipText("Reset filter to the column's min/max values or select all items");
        jButtonReset.setMargin(new java.awt.Insets(0, 0, 0, 0));
        jButtonReset.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonResetActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(0, 6, 0, 2);
        add(jButtonReset, gridBagConstraints);
    }// </editor-fold>//GEN-END:initComponents

    private void jCheckBoxEnabledActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxEnabledActionPerformed
        handleEnabled();
    }//GEN-LAST:event_jCheckBoxEnabledActionPerformed

    private void jButtonResetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonResetActionPerformed
        handleReset();
    }//GEN-LAST:event_jButtonResetActionPerformed

    private void jCheckBoxExclusiveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxExclusiveActionPerformed
        handleNot();
    }//GEN-LAST:event_jCheckBoxExclusiveActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private com.jidesoft.swing.CheckBoxList checkBoxListValues;
    private javax.swing.JButton jButtonReset;
    private javax.swing.JCheckBox jCheckBoxEnabled;
    private javax.swing.JCheckBox jCheckBoxExclusive;
    private javax.swing.JPanel jPanelRanges;
    private javax.swing.JPanel jPanelRangesOrValues;
    private javax.swing.JScrollPane jScrollPaneValues;
    // End of variables declaration//GEN-END:variables
}
