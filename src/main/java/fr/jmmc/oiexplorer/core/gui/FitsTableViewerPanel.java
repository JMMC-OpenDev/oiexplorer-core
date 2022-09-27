/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.oiexplorer.core.gui;

import fr.jmmc.jmcs.gui.component.BasicTableSorter;
import fr.jmmc.jmcs.gui.util.AutofitTableColumns;
import fr.jmmc.jmcs.gui.util.SwingUtils;
import fr.jmmc.jmcs.util.NumberUtils;
import fr.jmmc.oiexplorer.core.gui.model.ColumnsTableModel;
import static fr.jmmc.oiexplorer.core.gui.model.ColumnsTableModel.COLUMN_COL_INDEX;
import static fr.jmmc.oiexplorer.core.gui.model.ColumnsTableModel.COLUMN_ROW_INDEX;
import fr.jmmc.oiexplorer.core.gui.model.KeywordsTableModel;
import fr.jmmc.oitools.fits.FitsHDU;
import fr.jmmc.oitools.fits.FitsTable;
import fr.jmmc.oitools.model.IndexMask;
import fr.jmmc.oitools.model.OIData;
import fr.jmmc.oitools.processing.SelectorResult;
import java.awt.Color;
import java.awt.Component;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author bourgesl
 */
public final class FitsTableViewerPanel extends javax.swing.JPanel {

    private static final long serialVersionUID = 1L;
    /** Class logger */
    private static final Logger logger = LoggerFactory.getLogger(FitsTableViewerPanel.class.getName());

    // colors for table cell rendering. we need to register the default colors.
    private static final Color COLOR_MASKED = Color.YELLOW;

    private static final Color COLOR_NORMAL = (UIManager.getColor("Table.background") != null)
            ? UIManager.getColor("Table.background") : Color.WHITE;

    /* members */
    private final transient TableCellRenderer RDR_NUM_MASK_INSTANCE = new TableCellNumberMaskRenderer();
    private final KeywordsTableModel keywordsModel;
    private final BasicTableSorter keywordsTableSorter;
    private final ColumnsTableModel columnsModel;
    private final BasicTableSorter columnsTableSorter;

    // optional wavelength mask related to this OIData table:
    // If present, renderer will set the bg color of masked wavelengths.
    // It is set to null when: there is no wavelength for this table, or every wavelength is masked.
    private transient IndexMask maskWavelength = null;
    // optional masks for this OIData table:
    private transient IndexMask maskOIData1D = null;
    private transient IndexMask maskOIData2D = null;

    /* last selected cell */
    private int lastSelRow = -1;
    private int lastSelCol = -1;

    /** Creates new form FitsTableViewer */
    public FitsTableViewerPanel() {
        this.keywordsModel = new KeywordsTableModel();
        this.columnsModel = new ColumnsTableModel();

        initComponents();

        // Configure table sorting
        keywordsTableSorter = new BasicTableSorter(keywordsModel, jTableKeywords.getTableHeader());

        // Process the listeners last to first, so register before jtable, not after:
        keywordsTableSorter.addTableModelListener(new TableModelListener() {

            @Override
            public void tableChanged(final TableModelEvent e) {
                // If the table structure has changed, reapply the custom renderer/editor on columns + auto-fit
                if ((e.getSource() != keywordsTableSorter)
                        || (e.getFirstRow() == TableModelEvent.HEADER_ROW)) {

                    if (jTableKeywords.getRowCount() != 0) {
                        AutofitTableColumns.autoResizeTable(jTableKeywords);
                    }
                }
            }
        });
        jTableKeywords.setModel(keywordsTableSorter);

        columnsTableSorter = new BasicTableSorter(columnsModel, jTableColumns.getTableHeader());

        // Process the listeners last to first, so register before jtable, not after:
        columnsTableSorter.addTableModelListener(new TableModelListener() {

            @Override
            public void tableChanged(final TableModelEvent e) {
                // If the table structure has changed, reapply the custom renderer/editor on columns + auto-fit
                if ((e.getSource() != columnsTableSorter)
                        || (e.getFirstRow() == TableModelEvent.HEADER_ROW)) {

                    if (jTableColumns.getRowCount() != 0) {
                        AutofitTableColumns.autoResizeTable(jTableColumns);
                    }
                }
                if (e.getSource() == columnsTableSorter) {
                    // sorting changed, restore selection:
                    restoreSelection();
                }
            }
        });
        jTableColumns.setModel(columnsTableSorter);

        // Fix row height:
        SwingUtils.adjustRowHeight(jTableKeywords);
        SwingUtils.adjustRowHeight(jTableColumns);

        jTableKeywords.setDefaultRenderer(Boolean.class, RDR_NUM_MASK_INSTANCE);
        jTableKeywords.setDefaultRenderer(Double.class, RDR_NUM_MASK_INSTANCE);
        jTableColumns.setDefaultRenderer(Float.class, RDR_NUM_MASK_INSTANCE);
        jTableColumns.setDefaultRenderer(Double.class, RDR_NUM_MASK_INSTANCE);
        jTableColumns.setDefaultRenderer(Boolean.class, RDR_NUM_MASK_INSTANCE);
        jTableColumns.setDefaultRenderer(Integer.class, RDR_NUM_MASK_INSTANCE);
        jTableColumns.setDefaultRenderer(Short.class, RDR_NUM_MASK_INSTANCE);
        jTableColumns.setDefaultRenderer(String.class, RDR_NUM_MASK_INSTANCE);
    }

    public void setViewerOptions(boolean includeDerivedColumns, boolean expandRows) {
        columnsModel.setIncludeDerivedColumns(includeDerivedColumns);
        columnsModel.setExpandRows(expandRows);
    }

    // Display Table
    public void setHdu(final FitsHDU hdu, final SelectorResult selectorResult) {
        // reset selection:
        lastSelRow = -1;
        lastSelCol = -1;

        if ((selectorResult == null) || !(hdu instanceof OIData)) {
            maskWavelength = null;
            maskOIData1D = null;
            maskOIData2D = null;
        } else {
            final OIData oiData = (OIData) hdu;
            maskWavelength = selectorResult.getWavelengthMask(oiData.getOiWavelength());
            maskOIData1D = selectorResult.getDataMask1D(oiData);
            maskOIData2D = selectorResult.getDataMask2D(oiData);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("maskWavelength: {}", maskWavelength);
            logger.debug("maskOIData1D:   {}", maskOIData1D);
            logger.debug("maskOIData2D:   {}", maskOIData2D);
        }
        // update table models:
        keywordsModel.setFitsHdu(hdu);

        final FitsTable table = (hdu instanceof FitsTable) ? (FitsTable) hdu : null;
        columnsModel.setFitsHdu(table);

        if (table != null) {
            jScrollPaneColumns.setVisible(true);
            jSplitPaneVert.setDividerLocation(0.25);
        } else {
            jScrollPaneColumns.setVisible(false);
            jSplitPaneVert.setDividerLocation(1.0);
        }
    }

    void restoreSelection() {
        if (lastSelRow != -1 && lastSelCol != -1) {
            setSelection(lastSelRow, lastSelCol);
        }
    }

    public void setSelection(final int row, final int col) {
        if (logger.isDebugEnabled()) {
            logger.debug("setSelection (row, col) = ({}, {})", row, col);
        }
        final int nRows = jTableColumns.getRowCount();

        if (nRows != 0) {
            final int rowColIdx = columnsTableSorter.findColumn(COLUMN_ROW_INDEX);
            final int colColIdx = (col != -1) ? columnsTableSorter.findColumn(COLUMN_COL_INDEX) : -1;

            if (logger.isDebugEnabled()) {
                logger.debug("rowColIdx: {}", rowColIdx);
                logger.debug("colColIdx: {}", colColIdx);
            }

            int rowIdx = -1;

            // Iterate on rows:
            for (int i = 0; i < nRows; i++) {
                final Integer rowValue = (Integer) columnsTableSorter.getValueAt(i, rowColIdx);

                // check row first:
                if ((rowValue != null) && (rowValue == row)) {
                    if (colColIdx != -1) {
                        final Integer colValue = (Integer) columnsTableSorter.getValueAt(i, colColIdx);

                        // check col:
                        if ((colValue != null) && (colValue != col)) {
                            // skip row:
                            continue;
                        }
                    }
                    // match row (and optionally col):
                    rowIdx = i;
                    // exit loop (first match)
                    break;
                }
            }

            if (logger.isDebugEnabled()) {
                logger.debug("rowIdx: {}", rowIdx);
            }

            if (rowIdx != -1) {
                // Item found so preserve this selection:
                lastSelRow = row;
                lastSelCol = col;

                jTableColumns.getSelectionModel().setSelectionInterval(rowIdx, rowIdx);

                // Move view to show found row
                jTableColumns.scrollRectToVisible(jTableColumns.getCellRect(rowIdx, 0, true));
            } else {
                jTableColumns.getSelectionModel().clearSelection();
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

        jSplitPaneVert = new javax.swing.JSplitPane();
        jScrollPaneKeywords = new javax.swing.JScrollPane();
        jTableKeywords = new javax.swing.JTable();
        jScrollPaneColumns = new javax.swing.JScrollPane();
        jTableColumns = new javax.swing.JTable();

        setName("Form"); // NOI18N
        setLayout(new java.awt.BorderLayout());

        jSplitPaneVert.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        jSplitPaneVert.setName("jSplitPaneVert"); // NOI18N

        jScrollPaneKeywords.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        jScrollPaneKeywords.setAutoscrolls(true);
        jScrollPaneKeywords.setName("jScrollPaneKeywords"); // NOI18N
        jScrollPaneKeywords.setPreferredSize(new java.awt.Dimension(100, 100));

        jTableKeywords.setModel(keywordsModel);
        jTableKeywords.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_OFF);
        jTableKeywords.setMinimumSize(new java.awt.Dimension(50, 50));
        jTableKeywords.setName("jTableKeywords"); // NOI18N
        jTableKeywords.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        jScrollPaneKeywords.setViewportView(jTableKeywords);

        jSplitPaneVert.setLeftComponent(jScrollPaneKeywords);

        jScrollPaneColumns.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        jScrollPaneColumns.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        jScrollPaneColumns.setName("jScrollPaneColumns"); // NOI18N
        jScrollPaneColumns.setPreferredSize(new java.awt.Dimension(100, 100));

        jTableColumns.setModel(columnsModel);
        jTableColumns.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_OFF);
        jTableColumns.setMinimumSize(new java.awt.Dimension(50, 50));
        jTableColumns.setName("jTableColumns"); // NOI18N
        jTableColumns.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        jScrollPaneColumns.setViewportView(jTableColumns);

        jSplitPaneVert.setRightComponent(jScrollPaneColumns);

        add(jSplitPaneVert, java.awt.BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JScrollPane jScrollPaneColumns;
    private javax.swing.JScrollPane jScrollPaneKeywords;
    private javax.swing.JSplitPane jSplitPaneVert;
    private javax.swing.JTable jTableColumns;
    private javax.swing.JTable jTableKeywords;
    // End of variables declaration//GEN-END:variables

    /**
     * Used to format numbers in cells and highlight masked cells.
     *
     * @warning: No trace log implemented as this is very often called (performance).
     */
    private final class TableCellNumberMaskRenderer extends DefaultTableCellRenderer {

        /** default serial UID for Serializable interface */
        private static final long serialVersionUID = 1;

        /** orange border for selected cell */
        private transient final Border _orangeBorder = BorderFactory.createLineBorder(Color.ORANGE, 2);

        private TableCellNumberMaskRenderer() {
            super();
        }

        /**
         * Sets the <code>String</code> object for the cell being rendered to
         * <code>value</code>.
         *
         * @param value  the string value for this cell; if value is
         *          <code>null</code> it sets the text value to an empty string
         * @see JLabel#setText
         *
         */
        @Override
        public void setValue(final Object value) {
            String text = "";
            if (value != null) {
                if (value instanceof Double) {
                    text = NumberUtils.format(((Double) value).doubleValue());
                } else if (value instanceof Boolean) {
                    text = ((Boolean) value).booleanValue() ? "T" : "F";
                } else {
                    text = value.toString();
                }
            }
            setText(text);
        }

        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            // always use right alignment:
            setHorizontalAlignment(JLabel.RIGHT);

            Color bgColor = COLOR_NORMAL;

            if ((maskOIData1D != null) || (maskOIData2D != null) || (maskWavelength != null)) {
                if (IndexMask.isFull(maskOIData1D) && IndexMask.isFull(maskOIData2D) && IndexMask.isFull(maskWavelength)) {
                    bgColor = COLOR_MASKED;
                } else {
                    final int rowColIdx = columnsTableSorter.findColumn(COLUMN_ROW_INDEX);
                    final int colColIdx = (column != -1) ? columnsTableSorter.findColumn(COLUMN_COL_INDEX) : -1;
                    if (logger.isDebugEnabled()) {
                        logger.debug("rowColIdx: {}", rowColIdx);
                        logger.debug("colColIdx: {}", colColIdx);
                    }

                    final Integer rowValue = (Integer) columnsTableSorter.getValueAt(row, rowColIdx);
                    final Integer colValue = (colColIdx != -1) ? (Integer) columnsTableSorter.getValueAt(row, colColIdx) : null;

                    if (logger.isDebugEnabled()) {
                        logger.debug("ColumnsModel (row, col): ({}, {})", rowValue, colValue);
                    }

                    final int idxNone = (maskOIData2D != null) ? maskOIData2D.getIndexNone() : -1;
                    final int idxFull = (maskOIData2D != null) ? maskOIData2D.getIndexFull() : -1;

                    // check masks:
                    boolean masked = true;

                    // check optional data mask 1D:
                    if (masked && (maskOIData1D != null)) {
                        if (IndexMask.isNotFull(maskOIData1D) && (rowValue != null) && !maskOIData1D.accept(rowValue)) {
                            masked = false;
                        }
                    }
                    // check mask 2D for row None flag:
                    if (masked && (maskOIData2D != null)) {
                        if (IndexMask.isNotFull(maskOIData2D) && (rowValue != null) && maskOIData2D.accept(rowValue, idxNone)) {
                            // row flagged as None:
                            masked = false;
                        }
                    }

                    // check optional wavelength mask:
                    if (masked && (maskWavelength != null)) {
                        if (IndexMask.isNotFull(maskWavelength) && (colValue != null) && !maskWavelength.accept(colValue)) {
                            masked = false;
                        }
                    }

                    // check optional data mask 2D (and its Full flag):
                    if (masked && (maskOIData2D != null)) {
                        if (IndexMask.isNotFull(maskOIData2D)
                                && (rowValue != null) && !maskOIData2D.accept(rowValue, idxFull)
                                && (colValue != null) && !maskOIData2D.accept(rowValue, colValue)) {
                            masked = false;
                        }
                    }

                    if (masked) {
                        bgColor = COLOR_MASKED;
                    }
                }
            }

            if (bgColor == COLOR_MASKED) {
                setBorder(_orangeBorder);
            }
            if (!isSelected) {
                setBackground(bgColor);
            }
            return this;
        }
    }
}
