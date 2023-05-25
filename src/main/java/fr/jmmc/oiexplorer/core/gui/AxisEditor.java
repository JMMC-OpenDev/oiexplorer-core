/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.oiexplorer.core.gui;

import fr.jmmc.jmcs.gui.component.Disposable;
import fr.jmmc.jmcs.gui.component.GenericListModel;
import fr.jmmc.jmcs.gui.util.SwingUtils;
import fr.jmmc.oiexplorer.core.function.ConverterFactory;
import fr.jmmc.oiexplorer.core.model.plot.Axis;
import fr.jmmc.oiexplorer.core.model.plot.AxisRangeMode;
import fr.jmmc.oiexplorer.core.model.plot.Range;
import fr.jmmc.oitools.OIFitsConstants;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Axis editor widget.
 * 
 * @author mella
 */
public class AxisEditor extends javax.swing.JPanel implements Disposable, ChangeListener {

    /** default serial UID for Serializable interface */
    private static final long serialVersionUID = 1L;
    /** Logger */
    private static final Logger logger = LoggerFactory.getLogger(AxisEditor.class.getName());

    /* members */
    /** PlotDefinitionEditor to notify in case of modification */
    private final PlotDefinitionEditor parentToNotify;
    /** Edited axis reference */
    private Axis axisToEdit;
    /** List of available axis names */
    private final GenericListModel<String> nameComboBoxModel;
    /** Flag notification of associated plotDefinitionEditor */
    private boolean notify = true;

    /** 
     * Creates the new AxisEditor form.
     * Use setAxis() to change model to edit.
     * @param parent PlotDefinitionEditor to be notified of changes.
     */
    public AxisEditor(final PlotDefinitionEditor parent) {
        initComponents();
        parentToNotify = parent;

        nameComboBoxModel = new GenericListModel<String>(new ArrayList<String>(25), true);
        nameComboBox.setModel(nameComboBoxModel);

        rangeEditor.addChangeListener(this);

        // hidden until request and valid code to get a correct behaviour
        final JComponent[] components = new JComponent[]{
            includeZeroCheckBox, includeDataRangeCheckBox, jRadioModeAuto, jRadioModeDefault, jRadioModeFixed
        };
        for (JComponent c : components) {
            c.setVisible(c.isEnabled());
        }

        // Adjust fonts:
        final Font fixedFont = new Font(Font.MONOSPACED, Font.PLAIN, SwingUtils.adjustUISize(12));
        this.nameComboBox.setFont(fixedFont);
    }

    /** 
     * Creates new form AxisEditor.
     * This empty constructor leave here for Netbeans GUI builder
     */
    public AxisEditor() {
        this(null);
    }

    /**
     * Free any ressource or reference to this instance :
     */
    @Override
    public void dispose() {
        if (logger.isDebugEnabled()) {
            logger.debug("AxisEditor[{}]: dispose", axisToEdit == null ? null : axisToEdit.getName());
        }
        reset();
        rangeEditor.dispose();
    }

    public void reset() {
        setAxis(null, null);
    }

    /** 
     * Initialize widgets according to given axis 
     * 
     * @param axis used to initialize widget states
     * @param axisChoices column names to display
     */
    public void setAxis(final Axis axis, final List<String> axisChoices) {
        axisToEdit = axis;
        nameComboBoxModel.clear();

        if (axis == null) {
            rangeEditor.setAlias(null);
            rangeEditor.setRange(null);
            return;
        }
        try {
            notify = false;
            final String axisName = axis.getName();

            nameComboBoxModel.addAll(axisChoices);
            nameComboBox.setSelectedItem(axisName);

            invertedCheckBox.setSelected(axis.isInvertedOrDefault());
            logScaleCheckBox.setSelected(axis.isLogScale());

            includeZeroCheckBox.setSelected(axis.isIncludeZero());
            includeDataRangeCheckBox.setSelected(axis.isIncludeDataRangeOrDefault());

            rangeEditor.setAlias(axisName);
            rangeEditor.setRange(axis.getRange());

            // enable or disable popup menus:
            updateRangeEditor(axis.getRange(), axis.getRangeModeOrDefault(), true);
            updateRangeList();

        } finally {
            notify = true;
        }
    }

    public boolean setAxisRange(final double min, final double max) {
        logger.debug("setAxisRange: [{} - {}]", min, max);
        return rangeEditor.setRangeFieldValues(min, max);
    }

    private void updateRangeList() {
        final boolean isWavelengthAxis = isWavelengthAxis();

        // TODO: use a factory to get predefined ranges per column name ?
        if (isWavelengthAxis) {
            rangeEditor.updateRangeList(RangeEditor.EFF_WAVE_PREDEFINED_RANGES);
        } else {
            rangeEditor.updateRangeList(null);
        }
    }

    private boolean isWavelengthAxis() {
        return (OIFitsConstants.COLUMN_EFF_WAVE.equalsIgnoreCase((String) nameComboBox.getSelectedItem()));
    }

    public void updateRangeMode(final AxisRangeMode mode) {
        axisToEdit.setRangeMode(mode);

        if (mode == AxisRangeMode.RANGE) {
            // hack to initialize range from plot values:
            Range r = rangeEditor.getFieldRange();
            // we must have a (NaN,Nan) Range to give to RangeEditor
            if (r == null) {
                r = new Range();
                r.setMin(Double.NaN);
                r.setMax(Double.NaN);
            }
            axisToEdit.setRange(r);
            rangeEditor.setAlias(axisToEdit.getName());
            rangeEditor.setRange(r);
        }
        updateRangeEditor(axisToEdit.getRange(), axisToEdit.getRangeMode());
        updateRangeList();
    }

    private void updateRangeEditor(final Range range, final AxisRangeMode mode) {
        updateRangeEditor(range, mode, false);
    }

    private void updateRangeEditor(final Range range, final AxisRangeMode mode, final boolean setRange) {
        switch (mode) {
            case AUTO:
                jRadioModeAuto.setSelected(true);
                break;
            default:
            case DEFAULT:
                jRadioModeDefault.setSelected(true);
                break;
            case RANGE:
                jRadioModeFixed.setSelected(true);
                break;
        }
        final boolean enable = (mode == AxisRangeMode.RANGE);
        rangeEditor.setEnabled(enable);
        rangeEditor.updateRange(range, setRange);
    }

    /** 
     * Return the edited Axis.
     * @return the edited Axis.
     */
    public Axis getAxis() {
        return axisToEdit;
    }

    @Override
    public void stateChanged(ChangeEvent ce) {
        if (notify) {
            if (ce.getSource() instanceof RangeEditor) {
                parentToNotify.updateModel(false); // false: no need to refresh plot definition names
            }
        }
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

        buttonGroupRangeModes = new javax.swing.ButtonGroup();
        nameComboBox = new javax.swing.JComboBox();
        logScaleCheckBox = new javax.swing.JCheckBox();
        invertedCheckBox = new javax.swing.JCheckBox();
        includeZeroCheckBox = new javax.swing.JCheckBox();
        includeDataRangeCheckBox = new javax.swing.JCheckBox();
        jPanelBounds = new javax.swing.JPanel();
        jRadioModeAuto = new javax.swing.JRadioButton();
        jRadioModeDefault = new javax.swing.JRadioButton();
        jRadioModeFixed = new javax.swing.JRadioButton();
        rangeEditor = new fr.jmmc.oiexplorer.core.gui.RangeEditor();

        setLayout(new java.awt.GridBagLayout());

        nameComboBox.setPrototypeDisplayValue("XXXX");
        nameComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                AxisEditor.this.actionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 0.6;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        add(nameComboBox, gridBagConstraints);

        logScaleCheckBox.setText("log");
        logScaleCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                AxisEditor.this.actionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        add(logScaleCheckBox, gridBagConstraints);

        invertedCheckBox.setText("inv.");
        invertedCheckBox.setToolTipText("Inverse axis orientation ?");
        invertedCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                AxisEditor.this.actionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        add(invertedCheckBox, gridBagConstraints);

        includeZeroCheckBox.setText("inc. 0");
        includeZeroCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                AxisEditor.this.actionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        add(includeZeroCheckBox, gridBagConstraints);

        includeDataRangeCheckBox.setText("def. range");
        includeDataRangeCheckBox.setToolTipText("Include default range ([0;1] for visibility or [-180;180] for phases)");
        includeDataRangeCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                AxisEditor.this.actionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 0;
        add(includeDataRangeCheckBox, gridBagConstraints);

        jPanelBounds.setLayout(new java.awt.GridBagLayout());

        buttonGroupRangeModes.add(jRadioModeAuto);
        jRadioModeAuto.setText("auto");
        jRadioModeAuto.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                AxisEditor.this.actionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        jPanelBounds.add(jRadioModeAuto, gridBagConstraints);

        buttonGroupRangeModes.add(jRadioModeDefault);
        jRadioModeDefault.setSelected(true);
        jRadioModeDefault.setText("default");
        jRadioModeDefault.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                AxisEditor.this.actionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        jPanelBounds.add(jRadioModeDefault, gridBagConstraints);

        buttonGroupRangeModes.add(jRadioModeFixed);
        jRadioModeFixed.setText("fixed");
        jRadioModeFixed.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                AxisEditor.this.actionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        jPanelBounds.add(jRadioModeFixed, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 5;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.insets = new java.awt.Insets(0, 2, 0, 2);
        add(jPanelBounds, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 6;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 0.2;
        gridBagConstraints.insets = new java.awt.Insets(0, 2, 0, 2);
        add(rangeEditor, gridBagConstraints);
    }// </editor-fold>//GEN-END:initComponents

    private void actionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_actionPerformed
        if (axisToEdit == null) {
            // disposed:
            return;
        }
        boolean forceRefreshPlotDefNames = false;

        if (evt.getSource() == includeZeroCheckBox) {
            axisToEdit.setIncludeZero(includeZeroCheckBox.isSelected());
        } else if (evt.getSource() == includeDataRangeCheckBox) {
            axisToEdit.setIncludeDataRange(includeDataRangeCheckBox.isSelected());
        } else if (evt.getSource() == invertedCheckBox) {
            axisToEdit.setInverted(invertedCheckBox.isSelected());
        } else if (evt.getSource() == logScaleCheckBox) {
            axisToEdit.setLogScale(logScaleCheckBox.isSelected());
        } else if (evt.getSource() == nameComboBox) {
            final String columnName = (String) nameComboBox.getSelectedItem();
            axisToEdit.setName(columnName);

            // only modify axis if the user changes the axis, not by swing events due to model changes:
            if (notify) {
                // reset converter and log scale:
                axisToEdit.setConverter(ConverterFactory.getInstance().getDefaultByColumn(columnName));
                axisToEdit.setLogScale(logScaleCheckBox.isSelected());
                // force refresh plot definition names:
                forceRefreshPlotDefNames = true;

                updateRangeList();
            }
        } else if (evt.getSource() == jRadioModeAuto) {
            updateRangeMode(AxisRangeMode.AUTO);
        } else if (evt.getSource() == jRadioModeDefault) {
            updateRangeMode(AxisRangeMode.DEFAULT);
        } else if (evt.getSource() == jRadioModeFixed) {
            updateRangeMode(AxisRangeMode.RANGE);
        } else {
            throw new IllegalStateException("TODO: handle event from " + evt.getSource());
        }

        if (notify) {
            parentToNotify.updateModel(forceRefreshPlotDefNames);
        }
    }//GEN-LAST:event_actionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup buttonGroupRangeModes;
    private javax.swing.JCheckBox includeDataRangeCheckBox;
    private javax.swing.JCheckBox includeZeroCheckBox;
    private javax.swing.JCheckBox invertedCheckBox;
    private javax.swing.JPanel jPanelBounds;
    private javax.swing.JRadioButton jRadioModeAuto;
    private javax.swing.JRadioButton jRadioModeDefault;
    private javax.swing.JRadioButton jRadioModeFixed;
    private javax.swing.JCheckBox logScaleCheckBox;
    private javax.swing.JComboBox nameComboBox;
    private fr.jmmc.oiexplorer.core.gui.RangeEditor rangeEditor;
    // End of variables declaration//GEN-END:variables
}
