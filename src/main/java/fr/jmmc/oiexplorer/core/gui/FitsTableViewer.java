/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.oiexplorer.core.gui;

import fr.jmmc.jmcs.Bootstrapper;
import fr.jmmc.jmcs.gui.component.BasicTableSorter;
import fr.jmmc.jmcs.gui.util.AutofitTableColumns;
import fr.jmmc.jmcs.gui.util.SwingUtils;
import fr.jmmc.jmcs.util.NumberUtils;
import fr.jmmc.oiexplorer.core.gui.model.ColumnsTableModel;
import fr.jmmc.oiexplorer.core.gui.model.KeywordsTableModel;
import fr.jmmc.oitools.fits.FitsTable;
import fr.jmmc.oitools.model.OIFitsFile;
import fr.jmmc.oitools.model.OIFitsLoader;
import fr.nom.tam.fits.FitsException;
import java.awt.Dimension;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

/**
 *
 * @author bourgesl
 */
public final class FitsTableViewer extends javax.swing.JPanel {

    private static final long serialVersionUID = 1L;

    public static final TableCellRenderer RDR_NUM_INSTANCE = new TableCellNumberRenderer();

    private final KeywordsTableModel keywordsModel;
    private final BasicTableSorter keywordsTableSorter;
    private final ColumnsTableModel columnsModel;
    private final BasicTableSorter columnsTableSorter;

    /** Creates new form FitsTableViewer */
    public FitsTableViewer() {
        this.keywordsModel = new KeywordsTableModel();
        this.columnsModel = new ColumnsTableModel();

        initComponents();

        // Configure table sorting
        keywordsTableSorter = new BasicTableSorter(keywordsModel, jTableKeywords.getTableHeader());
        jTableKeywords.setModel(keywordsTableSorter);

        if (false) {
            columnsTableSorter = null;
        } else {
            columnsTableSorter = new BasicTableSorter(columnsModel, jTableColumns.getTableHeader());
            jTableColumns.setModel(columnsTableSorter);
        }

        // Fix row height:
        SwingUtils.adjustRowHeight(jTableKeywords);
        SwingUtils.adjustRowHeight(jTableColumns);

        jTableKeywords.setDefaultRenderer(Boolean.class, RDR_NUM_INSTANCE);
        jTableKeywords.setDefaultRenderer(Double.class, RDR_NUM_INSTANCE);
        jTableColumns.setDefaultRenderer(Float.class, RDR_NUM_INSTANCE);
        jTableColumns.setDefaultRenderer(Double.class, RDR_NUM_INSTANCE);
    }

    // Display Table
    private FitsTableViewer setTable(final FitsTable table) {
        keywordsModel.setFitsHdu(table);
        columnsModel.setFitsHdu(table);

        if (jTableKeywords.getRowCount() != 0) {
            AutofitTableColumns.autoResizeTable(jTableKeywords);
        }
        if (jTableColumns.getRowCount() != 0) {
            AutofitTableColumns.autoResizeTable(jTableColumns);
        }

        return this;
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

        jScrollPaneKeywords = new javax.swing.JScrollPane();
        jTableKeywords = new javax.swing.JTable();
        jScrollPaneColumns = new javax.swing.JScrollPane();
        jTableColumns = new javax.swing.JTable();

        setName("Form"); // NOI18N
        setLayout(new java.awt.GridBagLayout());

        jScrollPaneKeywords.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        jScrollPaneKeywords.setColumnHeaderView(null);
        jScrollPaneKeywords.setName("jScrollPaneKeywords"); // NOI18N
        jScrollPaneKeywords.setViewportView(null);

        jTableKeywords.setModel(keywordsModel);
        jTableKeywords.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_OFF);
        jTableKeywords.setMinimumSize(new java.awt.Dimension(50, 50));
        jTableKeywords.setName("jTableKeywords"); // NOI18N
        jScrollPaneKeywords.setViewportView(jTableKeywords);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.2;
        add(jScrollPaneKeywords, gridBagConstraints);

        jScrollPaneColumns.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        jScrollPaneColumns.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        jScrollPaneColumns.setName("jScrollPaneColumns"); // NOI18N

        jTableColumns.setModel(columnsModel);
        jTableColumns.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_OFF);
        jTableColumns.setMinimumSize(new java.awt.Dimension(50, 50));
        jTableColumns.setName("jTableColumns"); // NOI18N
        jScrollPaneColumns.setViewportView(jTableColumns);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.8;
        add(jScrollPaneColumns, gridBagConstraints);
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JScrollPane jScrollPaneColumns;
    private javax.swing.JScrollPane jScrollPaneKeywords;
    private javax.swing.JTable jTableColumns;
    private javax.swing.JTable jTableKeywords;
    // End of variables declaration//GEN-END:variables

    public static void main(String[] args) {
        // invoke Bootstrapper method to initialize logback now:
        Bootstrapper.getState();
        try {
            OIFitsFile oiFitsFile = OIFitsLoader.loadOIFits("/home/bourgesl/dev/oitools-dev/src/test/resources/oifits/GRAVI.2016-06-23T03:10:17.458_singlesciviscalibrated.fits");
            oiFitsFile.analyze();

            for (FitsTable table : oiFitsFile.getOITableList()) {
                final JFrame frame = new JFrame("Table: " + table.toString());
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

                frame.setMinimumSize(new Dimension(800, 800));

                frame.add(new FitsTableViewer().setTable(table));
                frame.pack();
                frame.setVisible(true);
            }

        } catch (IOException ex) {
            Logger.getLogger(FitsTableViewer.class.getName()).log(Level.SEVERE, null, ex);
        } catch (FitsException ex) {
            Logger.getLogger(FitsTableViewer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Used to format numbers in cells.
     *
     * @warning: No trace log implemented as this is very often called (performance).
     */
    private final static class TableCellNumberRenderer extends DefaultTableCellRenderer {

        /** default serial UID for Serializable interface */
        private static final long serialVersionUID = 1;

        /**
         * Constructor
         */
        private TableCellNumberRenderer() {
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
    }
}
