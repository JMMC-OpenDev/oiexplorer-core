/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.oiexplorer.core.gui;

import fr.jmmc.jmcs.util.ObjectUtils;
import fr.jmmc.oiexplorer.core.model.IdentifiableVersion;
import fr.jmmc.oiexplorer.core.model.OIFitsCollectionManager;
import fr.jmmc.oiexplorer.core.model.OIFitsCollectionManagerEvent;
import fr.jmmc.oiexplorer.core.model.OIFitsCollectionManagerEventListener;
import fr.jmmc.oiexplorer.core.model.OIFitsCollectionManagerEventType;
import fr.jmmc.oiexplorer.core.model.oi.SubsetDefinition;
import fr.jmmc.oitools.model.OIFitsFile;
import fr.jmmc.oitools.processing.SelectorResult;
import java.lang.ref.WeakReference;
import javax.swing.JPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Plot view implementation
 * @author mella
 */
public final class PlotView extends javax.swing.JPanel implements OIFitsCollectionManagerEventListener {

    /** default serial UID for Serializable interface */
    private static final long serialVersionUID = 1;
    /** Class logger */
    private static final Logger logger = LoggerFactory.getLogger(PlotView.class.getName());
    /** use new OIFITS table browser instead of HTML viewer */
    private static final boolean USE_OIFITS_BROWSER = true;

    /* members */
    /** OIFitsCollectionManager singleton reference */
    private final static OIFitsCollectionManager ocm = OIFitsCollectionManager.getInstance();
    /** related plot identifier */
    private final String plotId;
    /** last version of the subset of the plot */
    private IdentifiableVersion lastSubsetVersion = null;
    /** former html panel */
    private OIFitsHtmlPanel oiFitsHtmlPanel = null;
    /** former table browser panel */
    private OIFitsTableBrowser oiFitsBrowserPanel = null;

    /**
     * Creates new form PlotView
     * @param plotId plot identifier
     */
    public PlotView(final String plotId) {
        ocm.bindPlotChanged(this);

        // Build GUI
        initComponents();

        this.plotId = plotId;

        // Finish init
        postInit();
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

        // forward dispose() to child components:
        if (plotChartPanel != null) {
            plotChartPanel.dispose();
        }
        if (plotDefinitionEditor != null) {
            plotDefinitionEditor.dispose();
        }
        if (USE_OIFITS_BROWSER) {
            oiFitsBrowserPanel.dispose();
        }
    }

    /**
     * This method is useful to set the models and specific features of initialized swing components :
     */
    private void postInit() {
        plotChartPanel.setPlotId(plotId);
        plotDefinitionEditor.setPlotId(plotId);

        final JPanel dataPanel;

        if (USE_OIFITS_BROWSER) {
            oiFitsBrowserPanel = new OIFitsTableBrowser();
            dataPanel = oiFitsBrowserPanel;
        } else {
            oiFitsHtmlPanel = new OIFitsHtmlPanel();
            dataPanel = oiFitsHtmlPanel;
        }
        jTabbedPaneViews.addTab("data", dataPanel);
    }

    /**
     * Refresh the data view
     * @param subsetDefinition subset definition
     */
    private void updateDataView(final SubsetDefinition subsetDefinition) {
        if (logger.isDebugEnabled()) {
            logger.debug("updateHtmlView: lastSubsetVersion {} vs subsetVersion {}", this.lastSubsetVersion,
                    (subsetDefinition != null) ? subsetDefinition.getIdentifiableVersion() : null);
        }

        // compare last version with the subset itself (see IdentifiableVersion.equals):
        if (!ObjectUtils.areEquals(this.lastSubsetVersion, subsetDefinition)) {

            this.lastSubsetVersion = (subsetDefinition != null) ? subsetDefinition.getIdentifiableVersion() : null;
            logger.debug("subsetVersion changed: {}", this.lastSubsetVersion);

            final OIFitsFile oiFitsFile = (subsetDefinition != null) ? subsetDefinition.getOIFitsSubset() : null;

            if (oiFitsBrowserPanel != null) {

                final SelectorResult selectorResult
                                     = (subsetDefinition == null) ? null : subsetDefinition.getSelectorResult();

                this.oiFitsBrowserPanel.setOiFitsFileRef(
                        new WeakReference<OIFitsFile>(oiFitsFile), new WeakReference<SelectorResult>(selectorResult));
            }
            if (oiFitsHtmlPanel != null) {
                this.oiFitsHtmlPanel.updateOIFits(oiFitsFile);
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

        jTabbedPaneViews = new javax.swing.JTabbedPane();
        plotPanel = new javax.swing.JPanel();
        plotChartPanel = new fr.jmmc.oiexplorer.core.gui.PlotChartPanel();
        plotDefinitionEditor = new fr.jmmc.oiexplorer.core.gui.PlotDefinitionEditor();

        setLayout(new java.awt.BorderLayout());

        jTabbedPaneViews.setTabLayoutPolicy(javax.swing.JTabbedPane.SCROLL_TAB_LAYOUT);

        plotPanel.setLayout(new java.awt.GridBagLayout());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        plotPanel.add(plotChartPanel, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        plotPanel.add(plotDefinitionEditor, gridBagConstraints);

        jTabbedPaneViews.addTab("plot", plotPanel);

        add(jTabbedPaneViews, java.awt.BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTabbedPane jTabbedPaneViews;
    private fr.jmmc.oiexplorer.core.gui.PlotChartPanel plotChartPanel;
    private fr.jmmc.oiexplorer.core.gui.PlotDefinitionEditor plotDefinitionEditor;
    private javax.swing.JPanel plotPanel;
    // End of variables declaration//GEN-END:variables

    /**
     * Return the Plot panel (used by plotView port)
     * @return Plot panel
     */
    public PlotChartPanel getPlotPanel() {
        return plotChartPanel;
    }

    /**
     * Return the related plot identifier
     * @return related plot identifier
     */
    public String getPlotId() {
        return plotId;
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
        switch (type) {
            case PLOT_CHANGED:
                return plotId;
            default:
        }
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
            case PLOT_CHANGED:
                updateDataView(event.getPlot().getSubsetDefinition());
                break;
            default:
        }
        logger.debug("onProcess {} - done", event);
    }
}
