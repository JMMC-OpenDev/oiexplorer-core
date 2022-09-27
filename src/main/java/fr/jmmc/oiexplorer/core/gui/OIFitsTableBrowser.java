/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.jmmc.oiexplorer.core.gui;

import fr.jmmc.jmcs.Bootstrapper;
import fr.jmmc.jmcs.gui.component.GenericListModel;
import fr.jmmc.jmcs.gui.util.SwingUtils;
import fr.jmmc.jmcs.gui.util.WindowUtils;
import fr.jmmc.jmcs.util.ObjectUtils;
import fr.jmmc.oiexplorer.core.gui.selection.DataPointer;
import fr.jmmc.oiexplorer.core.model.OIFitsCollectionManager;
import fr.jmmc.oiexplorer.core.model.OIFitsCollectionManagerEvent;
import fr.jmmc.oiexplorer.core.model.OIFitsCollectionManagerEventListener;
import fr.jmmc.oiexplorer.core.model.OIFitsCollectionManagerEventType;
import fr.jmmc.oitools.fits.FitsHDU;
import fr.jmmc.oitools.image.FitsImageHDU;
import fr.jmmc.oitools.image.ImageOiParam;
import fr.jmmc.oitools.model.DataModel;
import fr.jmmc.oitools.model.OIArray;
import fr.jmmc.oitools.model.OIData;
import fr.jmmc.oitools.model.OIFitsFile;
import fr.jmmc.oitools.model.OIFitsLoader;
import fr.jmmc.oitools.model.OITable;
import fr.jmmc.oitools.processing.SelectorResult;
import fr.nom.tam.fits.FitsException;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.Timer;
import javax.swing.event.ListSelectionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public final class OIFitsTableBrowser extends javax.swing.JPanel implements OIFitsCollectionManagerEventListener {

    private static final long serialVersionUID = 1L;
    /** Class logger */
    private static final Logger logger = LoggerFactory.getLogger(OIFitsTableBrowser.class.getName());

    /** OIFitsCollectionManager singleton */
    private final static OIFitsCollectionManager ocm = OIFitsCollectionManager.getInstance();

    /* members */
    /** table viewer panel */
    private final FitsTableViewerPanel tableViewer;
    /** oifits (weak) file reference */
    private transient WeakReference<OIFitsFile> oiFitsFileRef = null;
    /** selectorResult (weak) reference */
    private transient WeakReference<SelectorResult> selectorResultRef = null;

    /** Creates new form FitsBrowserPanel */
    public OIFitsTableBrowser() {
        initComponents();

        this.tableViewer = new FitsTableViewerPanel();
        postInit();

        ocm.bindSelectionChanged(this);
    }

    /**
     * Free any resource or reference to this instance :
     * remove this instance from OIFitsCollectionManager event notifiers
     * dispose also child components
     */
    @Override
    public void dispose() {
        if (logger.isDebugEnabled()) {
            logger.debug("dispose: {}", ObjectUtils.getObjectInfo(this));
        }
        ocm.unbind(this);

        reset();
    }

    private void postInit() {
        this.jSplitPane.setRightComponent(tableViewer);
    }

    private void reset() {
        this.jListTables.clearSelection();
        this.tableViewer.setHdu(null, null);
        this.oiFitsFileRef = null;
    }

    /*
     * OIFitsCollectionManagerEventListener implementation
     */
    /**
     * Return the optional subject id i.e. related object id that this listener accepts
     * @param type event type
     * @return subject id (null means accept any event) or DISCARDED_SUBJECT_ID to discard event
     */
    @Override
    public String getSubjectId(final OIFitsCollectionManagerEventType type) {
        return DISCARDED_SUBJECT_ID;
    }

    /**
     * Handle the given OIFits collection event
     * @param event OIFits collection event
     */
    @Override
    public void onProcess(final OIFitsCollectionManagerEvent event) {
        logger.debug("onProcess {}", event);

        switch (event.getType()) {
            case SELECTION_CHANGED:
                updateSelection(event.getSelection());
                break;
            default:
        }
        logger.debug("onProcess {} - done", event);
    }

    private void updateSelection(final DataPointer ptr) {
        logger.debug("updateSelection: {}", ptr);

        final OIData oiData = ptr.getOiData();

        // lazy resolve HDU:
        final OIFitsFile oiFitsFile = getOIFitsFile();

        if (oiFitsFile != null) {
            logger.debug("oiFitsFile: {}", oiFitsFile);

            final GenericListModel<HduRef> listModel = (GenericListModel<HduRef>) this.jListTables.getModel();

            for (int i = 0, len = listModel.getSize(); i < len; i++) {
                final HduRef hduRef = listModel.get(i);

                final FitsHDU hdu = getFitsHdu(hduRef, oiFitsFile);

                if (oiData == hdu) {
                    // table found:
                    logger.debug("hdu found: {}", hdu);

                    if (this.jListTables.getSelectedIndex() != i) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("change selected index: {}", i);
                        }
                        this.jListTables.setSelectedIndex(i);
                    }

                    // use invokeLater (if jListTables changed)
                    SwingUtils.invokeLaterEDT(new Runnable() {
                        @Override
                        public void run() {
                            tableViewer.setSelection(ptr.getRow(), ptr.getCol());
                        }
                    });
                    break;
                }
            }
        }
    }

    public void setOiFitsFileRef(final WeakReference<OIFitsFile> oiFitsFileRef) {
        setOiFitsFileRef(oiFitsFileRef, null); // no selector result: no masks given for OIDatas
    }

    public void setOiFitsFileRef(
            final WeakReference<OIFitsFile> oiFitsFileRef, final WeakReference<SelectorResult> selectorResultRef) {

        final OIFitsFile oiFitsFile = (oiFitsFileRef != null) ? oiFitsFileRef.get() : null;
        this.selectorResultRef = selectorResultRef;

        if (this.getOIFitsFile() != oiFitsFile) {
            reset();
            this.oiFitsFileRef = (oiFitsFile != null) ? new WeakReference<OIFitsFile>(oiFitsFile) : null;

            final List<HduRef> hduRefs;

            if (oiFitsFile != null) {
                hduRefs = new ArrayList<HduRef>();

                if (oiFitsFile.getPrimaryImageHDU() != null) {
                    hduRefs.add(getHduRef(0, oiFitsFile.getPrimaryImageHDU()));
                }

                final List<OITable> oiTables = oiFitsFile.getOITableList();

                for (int i = 0, len = oiTables.size(); i < len; i++) {
                    hduRefs.add(getHduRef(i, oiTables.get(i)));
                }

                // Add remaining Image HDUs:
                final int nbImageHDus = oiFitsFile.getImageHDUCount();
                if (nbImageHDus > 1) {
                    final List<FitsImageHDU> fitsImageHDUs = oiFitsFile.getFitsImageHDUs();

                    for (int i = 1; i < nbImageHDus; i++) {
                        final FitsImageHDU fitsImageHDU = fitsImageHDUs.get(i);

                        hduRefs.add(getHduRef(i, fitsImageHDU));
                    }
                }

                // Add IMAGE-OI table if any
                if (oiFitsFile.getExistingImageOiData() != null) {
                    hduRefs.add(getHduRef(0, oiFitsFile.getExistingImageOiData().getInputParam()));

                    if (oiFitsFile.getExistingImageOiData().getExistingOutputParam() != null) {
                        hduRefs.add(getHduRef(1, oiFitsFile.getExistingImageOiData().getExistingOutputParam()));
                    }
                }

            } else {
                hduRefs = new ArrayList<HduRef>(0);
            }

            // no hard reference to FITS HDU:
            this.jListTables.setModel(new GenericListModel<HduRef>(hduRefs));

            if (!hduRefs.isEmpty()) {
                // ensure selection:
                this.jListTables.setSelectedIndex(0);
            }
        }
    }

    private FitsHDU getFitsHdu(final HduRef hduRef, final OIFitsFile oiFitsFile) {
        switch (hduRef.getHduType()) {
            case OI_TABLE:
                return oiFitsFile.getOITableList().get(hduRef.getIndex());
            case IMAGE_OI_PARAM:
                switch (hduRef.getIndex()) {
                    case 0:
                        return oiFitsFile.getExistingImageOiData().getInputParam();
                    case 1:
                        return oiFitsFile.getExistingImageOiData().getExistingOutputParam();
                    default:
                }
                break;
            case HDU:
                return oiFitsFile.getFitsImageHDUs().get(hduRef.getIndex());
            default:
        }
        return null;
    }

    private OIFitsFile getOIFitsFile() {
        if (this.oiFitsFileRef != null) {
            return this.oiFitsFileRef.get();
        }
        return null;
    }

    /**
     * Called whenever the target selection changes.
     * @param e the event that characterizes the change.
     */
    private void processListTableValueChanged(final ListSelectionEvent e) {
        // skip events when the user selection is adjusting :
        if (e.getValueIsAdjusting()) {
            return;
        }

        final HduRef hduRef = (HduRef) this.jListTables.getSelectedValue();

        if (hduRef != null) {
            logger.debug("Selected HDU: {}", hduRef);

            // lazy resolve HDU:
            final OIFitsFile oiFitsFile = getOIFitsFile();
            logger.debug("oiFitsFile: {}", oiFitsFile);

            final FitsHDU hdu;
            if (oiFitsFile == null) {
                hdu = null;
            } else {
                hdu = getFitsHdu(hduRef, oiFitsFile);
            }
            logger.debug("hdu: {}", hdu);

            final SelectorResult selectorResult;
            if (hdu != null && hdu instanceof OIData) {
                selectorResult = (selectorResultRef == null) ? null : selectorResultRef.get();
            } else {
                selectorResult = null;
            }

            final boolean includeDerivedColumns = true;
            final boolean expandRows = isTableRowsExpanded(hdu);

            this.tableViewer.setViewerOptions(includeDerivedColumns, expandRows);
            this.tableViewer.setHdu(hdu, selectorResult);
        }
    }

    private boolean isTableRowsExpanded(final FitsHDU hdu) {
        if (hdu instanceof OIArray) {
            return false;
        }
        return true;
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jSplitPane = new javax.swing.JSplitPane();
        jScrollPaneTables = new javax.swing.JScrollPane();
        jListTables = new javax.swing.JList();

        setLayout(new java.awt.BorderLayout());

        jListTables.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jListTables.setPrototypeCellValue("OI_WAVELENGTH#99");
        jListTables.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                jListTablesValueChanged(evt);
            }
        });
        jScrollPaneTables.setViewportView(jListTables);

        jSplitPane.setLeftComponent(jScrollPaneTables);

        add(jSplitPane, java.awt.BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents

    private void jListTablesValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_jListTablesValueChanged
        processListTableValueChanged(evt);
    }//GEN-LAST:event_jListTablesValueChanged

    private final static HduRef getHduRef(final int index, final FitsHDU hdu) {
        return new HduRef(index, getHduType(hdu), (hdu != null) ? hdu.idToString() : null);
    }

    private final static HduType getHduType(final FitsHDU hdu) {
        if (hdu instanceof OITable) {
            return HduType.OI_TABLE;
        } else if (hdu instanceof ImageOiParam) {
            return HduType.IMAGE_OI_PARAM;
        }
        return HduType.HDU;
    }

    private enum HduType {
        OI_TABLE,
        IMAGE_OI_PARAM,
        HDU
    }

    private final static class HduRef {

        private final int index;
        private final HduType hduType;
        private final String label;

        HduRef(final int index, final HduType hduType, final String label) {
            this.index = index;
            this.hduType = hduType;
            this.label = label;
        }

        public int getIndex() {
            return index;
        }

        public HduType getHduType() {
            return hduType;
        }

        public String getLabel() {
            return label;
        }

        @Override
        public String toString() {
            return getLabel();
        }
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JList jListTables;
    private javax.swing.JScrollPane jScrollPaneTables;
    private javax.swing.JSplitPane jSplitPane;
    // End of variables declaration//GEN-END:variables

    // --- CLI interface ---
    /** hard reference to loaded OIFitsFile (CLI) */
    private static OIFitsFile LOADED_OIFITS_FILE = null;

    private final static boolean DEBUG_MEMORY = false;

    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("No file location given in arguments.");
            System.exit(1);
        }
        // invoke Bootstrapper method to initialize logback now:
        Bootstrapper.getState();

        DataModel.setOiVisComplexSupport(true);
        try {
            final OIFitsFile oiFitsFile = OIFitsLoader.loadOIFits(args[0]);
            oiFitsFile.analyze();

            // pin OIFitsFile reference in memory:
            // disable to test weak references (memory leaks)
            if (!DEBUG_MEMORY) {
                LOADED_OIFITS_FILE = oiFitsFile;
            }
            OIFitsTableBrowser.showFitsBrowser(oiFitsFile);

            if (DEBUG_MEMORY) {
                SwingUtils.invokeEDT(new Runnable() {
                    @Override
                    public void run() {
                        final Timer timer = new Timer(500, new ActionListener() {
                            /**
                             * Handle the timer call
                             * @param ae action event
                             */
                            @Override
                            public void actionPerformed(final ActionEvent ae) {
                                logger.debug("timer called");
                                System.gc();
                            }
                        });
                        timer.start();
                    }
                });
            }

        } catch (IOException ioe) {
            logger.error("IO exception occured:", ioe);
        } catch (FitsException fe) {
            logger.error("Fits exception occured:", fe);
        }
    }

    public static void showFitsBrowser(final OIFitsFile oiFitsFile) {
        final WeakReference<OIFitsFile> oiFitsFileRef = new WeakReference<OIFitsFile>(oiFitsFile);
        final String oiFitsFileName = oiFitsFile.getFileName();

        SwingUtils.invokeEDT(new Runnable() {
            @Override
            public void run() {
                final OIFitsTableBrowser fb = new OIFitsTableBrowser();
                fb.setOiFitsFileRef(oiFitsFileRef);

                final JFrame frame = new JFrame("File: " + oiFitsFileName) {
                    @Override
                    public void dispose() {
                        super.dispose();
                        fb.dispose();
                    }
                };
                frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                frame.setMinimumSize(new Dimension(800, 800));

                frame.add(fb);

                WindowUtils.setClosingKeyboardShortcuts(frame);
                frame.pack();
                WindowUtils.centerOnMainScreen(frame);

                frame.setVisible(true);
            }
        });
    }
}
