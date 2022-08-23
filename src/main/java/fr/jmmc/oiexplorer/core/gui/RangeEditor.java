/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.oiexplorer.core.gui;

import fr.jmmc.jmal.AbsorptionLineRange;
import fr.jmmc.jmcs.gui.component.Disposable;
import fr.jmmc.jmcs.gui.component.GenericListModel;
import fr.jmmc.jmcs.gui.util.FormatterFactoryUtils;
import fr.jmmc.jmcs.service.RecentValuesManager;
import fr.jmmc.jmcs.util.NumberUtils;
import fr.jmmc.jmcs.util.ObjectUtils;
import fr.jmmc.oiexplorer.core.model.plot.Range;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JPopupMenu;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Range editor widget. has a min field, a max field, a optional list of predefined ranges.
 */
public final class RangeEditor extends javax.swing.JPanel implements Disposable {

    /**
     * Predefined range for EFF_WAVE
     */
    public static final Map<String, double[]> EFF_WAVE_PREDEFINED_RANGES = new HashMap<>(11);

    static {
        for (AbsorptionLineRange r : AbsorptionLineRange.values()) {
            EFF_WAVE_PREDEFINED_RANGES.put(r.getName(), new double[]{r.getMin(), r.getMax()});
        }
    }

    /**
     * default serial UID for Serializable interface
     */
    private static final long serialVersionUID = 1L;
    /** Logger */
    private static final Logger logger = LoggerFactory.getLogger(RangeEditor.class.getName());
    /** undefined value for range list */
    private static final String RANGE_NONE = "[None]";

    /* members */
    /**
     * edited Range
     */
    private Range rangeToEdit = new Range();
    /**
     * predefined ranges
     */
    private final GenericListModel<String> rangeComboBoxModel;
    private final Map<String, double[]> rangeList = new HashMap<>(16);

    /**
     * Flag notification of associated UpdateListener
     */
    private boolean notify = true;
    /** Flag indicating a user input */
    private boolean user_input = true;
    // alias for the range, also used for RecentValuesManager
    private String alias;
    // listener references
    private ActionListener popupListenerMin = null;
    private ActionListener popupListenerMax = null;
    // popup menus in action
    private JPopupMenu popupMenuMin = null;
    private JPopupMenu popupMenuMax = null;

    /**
     * Creates the new RangeEditor form. Use setRange() to change model to edit.
     */
    public RangeEditor() {
        initComponents();

        setAlias(null);
        rangeComboBoxModel = new GenericListModel<String>(new ArrayList<String>(10), true);
        rangeListComboBox.setModel(rangeComboBoxModel);

        jFieldMin.addPropertyChangeListener("value", new java.beans.PropertyChangeListener() {
            @Override
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                if (!ObjectUtils.areEquals(evt.getOldValue(), evt.getNewValue())) {
                    actionPerformed(new ActionEvent(jFieldMin, 0, ""));
                }
            }
        });

        jFieldMax.addPropertyChangeListener("value", new java.beans.PropertyChangeListener() {
            @Override
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                if (!ObjectUtils.areEquals(evt.getOldValue(), evt.getNewValue())) {
                    actionPerformed(new ActionEvent(jFieldMax, 0, ""));
                }
            }
        });
    }

    /**
     * Free any ressource or reference to this instance :
     */
    @Override
    public void dispose() {
        if (logger.isDebugEnabled()) {
            logger.debug("RangeEditor[{}]: dispose");
        }
        for (ChangeListener listener : getChangeListeners()) {
            removeChangeListener(listener);
        }
        setRange(null);
    }

    /**
     * Initialize widgets according to given range. You should call setAlias before calling setRange, because setRange
     * uses the alias.
     *
     * @param range used to initialize widget states
     */
    public void setRange(final Range range) {
        if (range == null) {
            // reset state
            // dispose popup menus:
            jFieldMin.setComponentPopupMenu(null);
            jFieldMax.setComponentPopupMenu(null);
            popupListenerMin = popupListenerMax = null;
            popupMenuMin = popupMenuMax = null;
            return;
        }
        rangeToEdit = range;

        try {
            notify = user_input = false;

            // create new listeners to release previous listeners / popup menus:
            popupListenerMin = new FieldSetter(jFieldMin);
            popupListenerMax = new FieldSetter(jFieldMax);

            popupMenuMin = RecentValuesManager.getMenu(alias + ".min", popupListenerMin);
            popupMenuMax = RecentValuesManager.getMenu(alias + ".max", popupListenerMax);

            // enable or disable popup menus:
            updateRange(range, true);
            updateRangeList(null);

        } finally {
            notify = user_input = true;
        }
    }

    public boolean setRangeFieldValues(final Double min, final Double max) {
        logger.debug("setRangeFieldValues: [{} - {}]", min, max);

        boolean changed = false;
        try {
            notify = user_input = false;

            changed |= setFieldValue(jFieldMin, min);
            changed |= setFieldValue(jFieldMax, max);
        } finally {
            notify = user_input = true;
        }
        return changed;
    }

    private boolean setFieldValue(final JFormattedTextField field, final double value) {
        if (ObjectUtils.areEquals(field.getValue(), value)) {
            return false;
        }
        field.setValue(value);
        return true;
    }

    /**
     * @return true when rangeList is not empty, i.e when there exist some predefined ranges
     */
    private boolean hasRangeList() {
        return !rangeList.isEmpty();
    }

    public void updateRangeList(final Map<String, double[]> predefinedRanges) {
        try {
            this.notify = this.user_input = false;

            this.rangeComboBoxModel.clear();
            this.rangeList.clear();

            if (predefinedRanges == null) {
                rangeListComboBox.setVisible(false);
            } else {
                rangeListComboBox.setVisible(true);

                this.rangeComboBoxModel.add(RANGE_NONE);
                for (String rangeName : predefinedRanges.keySet()) {
                    rangeComboBoxModel.add(rangeName);
                }

                this.rangeList.putAll(predefinedRanges);
            }
        } finally {
            this.notify = this.user_input = true;
        }
    }

    private void handleRangeListSelection() {
        if (hasRangeList()) {
            final String selected = (String) rangeListComboBox.getSelectedItem();

            double min = Double.NaN;
            double max = Double.NaN;

            if (selected != null && !selected.isEmpty() && !RANGE_NONE.equalsIgnoreCase(selected)) {
                double[] rangeValues = rangeList.get(selected);
                min = rangeValues[0];
                max = rangeValues[1];
            }

            if (isFinite(min) || isFinite(max)) {
                try {
                    user_input = false;

                    jFieldMin.setValue(Double.valueOf(min));
                    jFieldMax.setValue(Double.valueOf(max));
                } finally {
                    user_input = true;
                }
            }
        }
    }

    public void updateRange(final Range range, final boolean setRange) {
        if (setRange) {
            if (range == null) {
                jFieldMin.setValue(null);
                jFieldMax.setValue(null);
            } else {
                jFieldMin.setValue(isFinite(range.getMin()) ? Double.valueOf(range.getMin()) : null);
                jFieldMax.setValue(isFinite(range.getMax()) ? Double.valueOf(range.getMax()) : null);
            }
        }

        // enable or disable popup menus:
        enablePopupMenu(jFieldMin, popupMenuMin);
        enablePopupMenu(jFieldMax, popupMenuMax);
    }

    Range getFieldRange() {
        double min = Double.NaN;
        double max = Double.NaN;

        Object value = this.jFieldMin.getValue();

        if (value instanceof Double) {
            min = ((Double) value);
        } else {
            final Double tryMin = NumberUtils.parseDouble(jFieldMin.getText());
            if (tryMin != null) {
                min = tryMin;
            }
        }

        value = this.jFieldMax.getValue();
        if (value instanceof Double) {
            max = ((Double) value);
        } else {
            final Double tryMax = NumberUtils.parseDouble(jFieldMax.getText());
            if (tryMax != null) {
                max = tryMax;
            }
        }

        final boolean minFinite = isFinite(min);
        final boolean maxFinite = isFinite(max);

        Range range = null;

        if ((minFinite != maxFinite)
                || (minFinite && maxFinite && (min < max))) {
            range = new Range();
            range.setMin(min);
            range.setMax(max);
        }

        // Do not store values set programmatically:
        if (user_input) {
            // Update recent values:
            if (popupMenuMin != null) { // only store value if popup menu has been initialized
                RecentValuesManager.addValue(alias + ".min", (minFinite) ? parseField(this.jFieldMin.getFormatter(), min) : null);
            }
            if (popupMenuMax != null) { // only store value if popup menu has been initialized
                RecentValuesManager.addValue(alias + ".max", (maxFinite) ? parseField(this.jFieldMax.getFormatter(), max) : null);
            }
        }
        return range;
    }

    private static String parseField(final JFormattedTextField.AbstractFormatter fmt, final double value) {
        try {
            return fmt.valueToString(value);
        } catch (ParseException pe) {
            logger.info("parseField: value = {}", value, pe);
        }
        return Double.toString(value);
    }

    /**
     * Return the edited Range.
     *
     * @return the edited Range.
     */
    public Range getRange() {
        return rangeToEdit;
    }

    /**
     * Enable or disable the RangeEditor fields
     *
     * @param enabled true to enable
     */
    @Override
    public void setEnabled(boolean enabled) {
        this.jFieldMin.setEnabled(enabled);
        this.jFieldMax.setEnabled(enabled);
        this.rangeListComboBox.setEnabled(enabled);
    }

    /**
     * Sets the alias for the range.
     *
     * @param alias new alias for the range. if null, replaced by empty string.
     */
    public void setAlias(final String alias) {
        this.alias = (alias == null) ? "" : alias;
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

        jFieldMin = new javax.swing.JFormattedTextField();
        jFieldMax = new javax.swing.JFormattedTextField();
        rangeListComboBox = new javax.swing.JComboBox();

        setLayout(new java.awt.GridBagLayout());

        jFieldMin.setColumns(2);
        jFieldMin.setFormatterFactory(FormatterFactoryUtils.getDecimalFormatterFactory());
        jFieldMin.setMinimumSize(new java.awt.Dimension(30, 27));
        jFieldMin.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                RangeEditor.this.actionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 0.3;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 4);
        add(jFieldMin, gridBagConstraints);

        jFieldMax.setColumns(2);
        jFieldMax.setFormatterFactory(FormatterFactoryUtils.getDecimalFormatterFactory());
        jFieldMax.setMinimumSize(new java.awt.Dimension(30, 27));
        jFieldMax.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                RangeEditor.this.actionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 0.3;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 2);
        add(jFieldMax, gridBagConstraints);

        rangeListComboBox.setPrototypeDisplayValue("XXXX");
        rangeListComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rangeListComboBoxActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 0.1;
        gridBagConstraints.insets = new java.awt.Insets(0, 2, 0, 4);
        add(rangeListComboBox, gridBagConstraints);
    }// </editor-fold>//GEN-END:initComponents

    private void actionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_actionPerformed
        if (evt.getSource() == jFieldMin) {
            // only update edited range when it is Enabled:
            if (jFieldMin.isEnabled()) {
                final Range r = getFieldRange();
                // update the Range
                if (r == null) {
                    rangeToEdit.setMin(Double.NaN);
                    jFieldMin.setValue(null);
                } else {
                    rangeToEdit.setMin(r.getMin());
                }
            }
        } else if (evt.getSource() == jFieldMax) {
            // only update edited range when it is Enabled:
            if (jFieldMax.isEnabled()) {
                final Range r = getFieldRange();
                // update the Range
                if (r == null) {
                    rangeToEdit.setMax(Double.NaN);
                    jFieldMax.setValue(null);
                } else {
                    rangeToEdit.setMax(r.getMax());
                }
            }
        } else if (evt.getSource() == rangeListComboBox) {
            handleRangeListSelection();
        } else {
            throw new IllegalStateException("TODO: handle event from " + evt.getSource());
        }

        if (notify) {
            fireStateChanged();
        }
    }//GEN-LAST:event_actionPerformed

    private void rangeListComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rangeListComboBoxActionPerformed
        this.actionPerformed(evt);
    }//GEN-LAST:event_rangeListComboBoxActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JFormattedTextField jFieldMax;
    private javax.swing.JFormattedTextField jFieldMin;
    private javax.swing.JComboBox rangeListComboBox;
    // End of variables declaration//GEN-END:variables

    private static boolean isFinite(final double value) {
        return !(Double.isNaN(value) && !Double.isInfinite(value));
    }

    private static final class FieldSetter implements ActionListener {

        private final JFormattedTextField textField;

        FieldSetter(final JFormattedTextField textField) {
            this.textField = textField;
        }

        @Override
        public void actionPerformed(final ActionEvent ae) {
            final String value = ae.getActionCommand();
            textField.setValue((value != null) ? Double.valueOf(value) : null);
        }
    }

    private static void enablePopupMenu(final JComponent component, final JPopupMenu popupMenu) {
        JPopupMenu enabledPopupMenu = null;
        if (popupMenu != null) {
            if (component.isEnabled() && popupMenu.getComponentCount() != 0) {
                enabledPopupMenu = popupMenu;
            }
        }
        component.setComponentPopupMenu(enabledPopupMenu);
    }

    /**
     * Listen to changes to Range.
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
}
