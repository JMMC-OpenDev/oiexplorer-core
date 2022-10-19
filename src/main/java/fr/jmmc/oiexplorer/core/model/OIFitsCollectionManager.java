/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.oiexplorer.core.model;

import fr.jmmc.jmcs.data.MimeType;
import fr.jmmc.jmcs.data.preference.SessionSettingsPreferences;
import fr.jmmc.jmcs.gui.component.MessagePane;
import fr.jmmc.jmcs.gui.component.StatusBar;
import fr.jmmc.jmcs.gui.task.HttpTaskSwingWorker;
import fr.jmmc.jmcs.gui.task.TaskSwingWorkerExecutor;
import fr.jmmc.jmcs.service.RecentFilesManager;
import fr.jmmc.jmcs.util.FileUtils;
import fr.jmmc.jmcs.util.ObjectUtils;
import fr.jmmc.jmcs.util.StringUtils;
import fr.jmmc.jmcs.util.jaxb.JAXBFactory;
import fr.jmmc.jmcs.util.jaxb.JAXBUtils;
import fr.jmmc.jmcs.util.jaxb.XmlBindException;
import fr.jmmc.oiexplorer.core.gui.OIExplorerTaskRegistry;
import fr.jmmc.oiexplorer.core.gui.PlotInfosData;
import fr.jmmc.oiexplorer.core.gui.chart.dataset.SharedSeriesAttributes;
import fr.jmmc.oiexplorer.core.gui.selection.DataPointer;
import fr.jmmc.oiexplorer.core.model.event.EventNotifier;
import fr.jmmc.oiexplorer.core.model.oi.GenericFilter;
import fr.jmmc.oiexplorer.core.model.oi.Identifiable;
import fr.jmmc.oiexplorer.core.model.oi.OIDataFile;
import fr.jmmc.oiexplorer.core.model.oi.OiDataCollection;
import fr.jmmc.oiexplorer.core.model.oi.Plot;
import fr.jmmc.oiexplorer.core.model.oi.SubsetDefinition;
import fr.jmmc.oiexplorer.core.model.oi.SubsetFilter;
import fr.jmmc.oiexplorer.core.model.oi.TableUID;
import fr.jmmc.oiexplorer.core.model.plot.PlotDefinition;
import fr.jmmc.oitools.model.OIData;
import fr.jmmc.oitools.model.OIFitsChecker;
import fr.jmmc.oitools.model.OIFitsCollection;
import fr.jmmc.oitools.model.OIFitsFile;
import fr.jmmc.oitools.model.OIFitsLoader;
import fr.jmmc.oitools.model.OIFlux;
import fr.jmmc.oitools.model.OIT3;
import fr.jmmc.oitools.model.OIVis;
import fr.jmmc.oitools.model.OIVis2;
import fr.jmmc.oitools.model.range.Range;
import fr.jmmc.oitools.processing.Merger;
import fr.jmmc.oitools.processing.Selector;
import fr.jmmc.oitools.processing.Selector.FilterValues;
import fr.jmmc.oitools.processing.SelectorResult;
import fr.nom.tam.fits.FitsException;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.httpclient.auth.AuthenticationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO: remove StatusBar / MessagePane (UI)
/**
 * Handle the oifits files collection.
 * @author mella, bourgesl
 */
public final class OIFitsCollectionManager implements OIFitsCollectionManagerEventListener {

    /** Logger */
    private static final Logger logger = LoggerFactory.getLogger(OIFitsCollectionManager.class);
    /** package name for JAXB generated code */
    private final static String OIFITS_EXPLORER_MODEL_JAXB_PATH = OiDataCollection.class.getPackage().getName();
    /** Current key for SubsetDefinition */
    public final static String CURRENT_SUBSET_DEFINITION = "SUBSET_0";
    /** Current key for PlotDefinition */
    public final static String CURRENT_PLOT_DEFINITION = "PLOT_DEF_0";
    /** Current key for View */
    public final static String CURRENT_VIEW = "VIEW_0";
    /** Plot Definition factory singleton */
    private final static PlotDefinitionFactory plotDefFactory = PlotDefinitionFactory.getInstance();
    /** Singleton pattern (after plotDefFactory) */
    private final static OIFitsCollectionManager INSTANCE = new OIFitsCollectionManager();
    /* members */
    /** internal JAXB Factory */
    private final JAXBFactory jf;
    /** flag to enable/disable firing events during startup (before calling start) */
    private boolean enableEvents = false;
    /** OIFits explorer collection structure (session) */
    private OiDataCollection userCollection = null;
    /** initial OIFits explorer collection structure (session) (loaded or saved state) */
    private OiDataCollection userCollectionInitial = null;
    /** associated file to the OIFits explorer collection */
    private File oiFitsCollectionFile = null;
    /** OIFits collection */
    private OIFitsCollection oiFitsCollection = null;
    /** data selection */
    private DataPointer selectedDataPointer = null;
    /** plot Infos */
    private PlotInfosData plotInfosData = null;
    /* event dispatchers */
    /** OIFitsCollectionManagerEventType event notifier map */
    private final EnumMap<OIFitsCollectionManagerEventType, EventNotifier<OIFitsCollectionManagerEvent, OIFitsCollectionManagerEventType, Object>> oiFitsCollectionManagerEventNotifierMap;

    /**
     * Return the Manager singleton
     * @return singleton instance
     */
    public static OIFitsCollectionManager getInstance() {
        return INSTANCE;
    }

    /**
     * Prevent instanciation of singleton.
     * Manager instance should be obtained using getInstance().
     */
    private OIFitsCollectionManager() {
        super();

        this.jf = JAXBFactory.getInstance(OIFITS_EXPLORER_MODEL_JAXB_PATH);

        logger.debug("OIFitsCollectionManager: JAXBFactory: {}", this.jf);

        this.oiFitsCollectionManagerEventNotifierMap = new EnumMap<OIFitsCollectionManagerEventType, EventNotifier<OIFitsCollectionManagerEvent, OIFitsCollectionManagerEventType, Object>>(OIFitsCollectionManagerEventType.class);

        int priority = 0;
        EventNotifier<OIFitsCollectionManagerEvent, OIFitsCollectionManagerEventType, Object> eventNotifier;

        for (OIFitsCollectionManagerEventType eventType : OIFitsCollectionManagerEventType.values()) {
            // false argument means allow self notification:
            final boolean skipSourceListener
                          = (eventType != OIFitsCollectionManagerEventType.COLLECTION_CHANGED) /* OIFitsCollectionManager post-process */
                    && (eventType != OIFitsCollectionManagerEventType.READY) /* OIFitsCollectionManager post-process */
                    && (eventType != OIFitsCollectionManagerEventType.SUBSET_CHANGED) /* GenericFiltersPanel handles its own reentrance */;

            eventNotifier = new EventNotifier<OIFitsCollectionManagerEvent, OIFitsCollectionManagerEventType, Object>(eventType.name(), priority, skipSourceListener);

            this.oiFitsCollectionManagerEventNotifierMap.put(eventType, eventNotifier);
            priority += 10;
        }

        // listen for COLLECTION_CHANGED event to analyze collection and fire initial events:
        getOiFitsCollectionChangedEventNotifier().register(this);
        // listen for READY event to handle initial collection state:
        getReadyEventNotifier().register(this);

        // reset anyway:
        reset();
    }

    /**
     * Free any resource or reference to this instance :
     * throw an IllegalStateException as it is invalid
     */
    @Override
    public void dispose() {
        if (logger.isDebugEnabled()) {
            logger.debug("dispose: {}", ObjectUtils.getObjectInfo(this));
        }

        throw new IllegalStateException("Using OIFitsCollectionManager.dispose() is invalid !");
    }

    /* --- OIFits file collection handling ------------------------------------- */
    /**
     * Load the OIFits collection at given URL
     * @param file OIFits explorer collection file file to load
     * @param checker optional OIFits checker instance (may be null)
     * @param listener progress listener
     * @throws IOException if an I/O exception occurred
     * @throws IllegalStateException if an unexpected exception occurred
     * @throws XmlBindException if a JAXBException was caught while creating an unmarshaller
     */
    public void loadOIFitsCollection(final File file, final OIFitsChecker checker,
                                     final LoadOIFitsListener listener) throws IOException, IllegalStateException, XmlBindException {
        loadOIFitsCollection(file, checker, listener, false);
    }

    /**
     * Load the OIFits collection at given URL or onl the include OIFits file references.
     * @param file OIFits explorer collection file file to load
     * @param checker optional OIFits checker instance (may be null)
     * @param listener progress listener
     * @param appendOIFitsFilesOnly load only OIFits and skip plot+subset if true, else reset and load whole collection content
     * @throws IOException if an I/O exception occurred
     * @throws IllegalStateException if an unexpected exception occurred
     * @throws XmlBindException if a JAXBException was caught while creating an unmarshaller
     */
    public void loadOIFitsCollection(final File file, final OIFitsChecker checker,
                                     final LoadOIFitsListener listener, final boolean appendOIFitsFilesOnly) throws IOException, IllegalStateException, XmlBindException {

        final OiDataCollection loadedUserCollection = (OiDataCollection) JAXBUtils.loadObject(file.toURI().toURL(), this.jf);

        OIDataCollectionFileProcessor.onLoad(loadedUserCollection);

        loadOIDataCollection(file, loadedUserCollection, checker, listener, appendOIFitsFilesOnly);
    }

    private void postLoadOIFitsCollection(final File file, final OiDataCollection oiDataCollection, final OIFitsChecker checker) {
        // refreshUI called addOIFitsFile(files)
        
        System.out.println("oiDataCollection:\n" + oiDataCollection);

        // add all SubsetDefinitions:
        for (SubsetDefinition subsetDefinition : oiDataCollection.getSubsetDefinitions()) {
            addSubsetDefinitionRef(subsetDefinition);
        }

        // add all PlotDefinitions:
        for (PlotDefinition plotDefinition : oiDataCollection.getPlotDefinitions()) {
            addPlotDefinitionRef(plotDefinition);
        }

        // add all Plots:
        for (Plot plot : oiDataCollection.getPlots()) {
            this.addPlotRef(plot);
        }

        if (logger.isDebugEnabled()) {
            logger.debug("subsetDefinitions: {}", getSubsetDefinitionList());
            logger.debug("plotDefinitions:   {}", getPlotDefinitionList());
            logger.debug("plots:             {}", getPlotList());
        }

        logger.info("subsetDefinitions: {}", getSubsetDefinitionList());
        logger.info("plotDefinitions:   {}", getPlotDefinitionList());
        logger.info("plots:             {}", getPlotList());

        // check and update references in current OiDataCollection:
        // initialize current objects: subsetDefinition, plotDefinition, plot if NOT PRESENT:
        checkReferences();
        
        System.out.println("userCollection:\n" + userCollection);

        // Fire the Ready event to any listener:
        fireReady(this, null);

        // after loadOIDataCollection as it calls reset():
        setOiFitsCollectionFile(file);

        // add given file to Open recent menu
        RecentFilesManager.addFile(file);
    }

    /**
     * Load the OIFits collection at given URL
     * @param file OIFits explorer collection file file to load
     * @throws IOException if an I/O exception occurred
     * @throws IllegalStateException if an unexpected exception occurred
     */
    public void saveOIFitsCollection(final File file) throws IOException, IllegalStateException {
        final long startTime = System.nanoTime();

        final OiDataCollection savedUserCollection = getUserCollection();

        OIDataCollectionFileProcessor.onSave(savedUserCollection);

        // TODO: may also save OIFits file copies into zip archive (xml + OIFits files) ??
        JAXBUtils.saveObject(file, savedUserCollection, this.jf);

        // finally: set the initial state of the main observation (as saved) 
        this.defineInitialUserCollection();

        setOiFitsCollectionFile(file);

        logger.info("saveOIFitsCollection: duration = {} ms.", 1e-6d * (System.nanoTime() - startTime));

        // add given file to Open recent menu
        RecentFilesManager.addFile(file);
    }

    public String dumpOIFitsCollection() throws IllegalStateException {

        final OiDataCollection savedUserCollection = getUserCollection();

        OIDataCollectionFileProcessor.onSave(savedUserCollection);

        final StringWriter w = new StringWriter(4096);
        JAXBUtils.saveObject(w, savedUserCollection, this.jf);

        return w.toString();
    }

    /**
     * Load OIFits files from the loaded OIDataCollection file using an async LoadOIFits task
     * @param file loaded OIFits explorer collection file
     * @param oiDataCollection OiDataCollection to look for
     * @param checker to report validation information
     * @param listener progress listener
     * @param appendOIFitsFilesOnly load only OIFits and skip plot+subset if true, else reset and load whole collection content
     */
    private void loadOIDataCollection(final File file, final OiDataCollection oiDataCollection, final OIFitsChecker checker,
                                      final LoadOIFitsListener listener, final boolean appendOIFitsFilesOnly) {

        final List<OIDataFile> oidataFiles = oiDataCollection.getFiles();
        final List<String> fileLocations = new ArrayList<String>(oidataFiles.size());

        final String parentPath = file.getParent();

        for (OIDataFile oidataFile : oidataFiles) {
            String oiFile = oidataFile.getFile();
            if (oiFile != null && oiFile.startsWith("./")) {
                oiFile = parentPath + oiFile.substring(1);
            }
            fileLocations.add(oiFile);
        }

        new LoadOIFitsFilesSwingWorker(fileLocations, checker, listener) {
            /**
             * Refresh GUI invoked by the Swing Event Dispatcher Thread (Swing EDT)
             * Called by @see #done()
             * @param oifitsFiles computed data
             */
            @Override
            public void refreshUI(final List<OIFitsFile> oifitsFiles) {
                // first reset if this we do not add files only:
                if (!appendOIFitsFilesOnly) {
                    reset();
                }

                // add OIFits files to collection = fire OIFitsCollectionChanged:
                super.refreshUI(oifitsFiles);

                if (!appendOIFitsFilesOnly) {
                    postLoadOIFitsCollection(file, oiDataCollection, checker);
                }

                listener.done(false);
            }

            @Override
            public void refreshNoData(final boolean cancelled) {
                listener.done(cancelled);
            }

        }.executeTask();
    }

    /**
     * Load the given OI Fits Files with the given checker component using an async LoadOIFits task
     * and add it to the OIFits collection
     * @param files files to load
     * @param checker checker component
     * @param listener progress listener
     */
    public void loadOIFitsFiles(final File[] files, final OIFitsChecker checker, final LoadOIFitsListener listener) {
        if (files != null) {
            final List<String> fileLocations = new ArrayList<String>(files.length);
            for (File file : files) {
                fileLocations.add(file.getAbsolutePath());
            }

            new LoadOIFitsFilesSwingWorker(fileLocations, checker, listener) {
                /**
                 * Refresh GUI invoked by the Swing Event Dispatcher Thread (Swing EDT)
                 * Called by @see #done()
                 * @param oifitsFiles computed data
                 */
                @Override
                public void refreshUI(final List<OIFitsFile> oifitsFiles) {
                    // add OIFits files to collection = fire OIFitsCollectionChanged:
                    super.refreshUI(oifitsFiles);

                    listener.done(false);
                }

                @Override
                public void refreshNoData(final boolean cancelled) {
                    listener.done(cancelled);
                }

            }.executeTask();
        }
    }

    /**
     * Cancel any running LoadOIFits task
     */
    public static void cancelTaskLoadOIFits() {
        // cancel any running task:
        TaskSwingWorkerExecutor.cancelTask(OIExplorerTaskRegistry.TASK_LOAD_OIFITS);
    }

    /**
     * TaskSwingWorker child class to download and load OIFits files in background
     * @author bourgesl
     */
    class LoadOIFitsFilesSwingWorker extends HttpTaskSwingWorker<List<OIFitsFile>> {

        private final List<String> fileLocations;
        private final OIFitsChecker checker;

        LoadOIFitsFilesSwingWorker(final List<String> fileLocations, final OIFitsChecker checker,
                                   final LoadOIFitsListener listener) {
            super(OIExplorerTaskRegistry.TASK_LOAD_OIFITS);
            this.fileLocations = fileLocations;
            this.checker = checker;
            this.addPropertyChangeListener(listener);
        }

        @Override
        public List<OIFitsFile> computeInBackground() {
            final int size = fileLocations.size();

            final List<OIFitsFile> oiFitsFiles = new ArrayList<OIFitsFile>(size);

            final long startTime = System.nanoTime();

            int n = 0;
            for (int i = 0; i < size; i++) {
                // fast interrupt :
                if (Thread.currentThread().isInterrupted()) {
                    // Update status bar:
                    StatusBar.show("Loading file(s) cancelled.");
                    return null;
                }

                final String fileLocation = fileLocations.get(i);
                try {
                    oiFitsFiles.add(loadOIFits(fileLocation, checker));
                    n++;
                } catch (IOException ioe) {
                    logger.info("Error reading file: {}", fileLocation, ioe.getCause());
                    // Update status bar:
                    StatusBar.show("Could not load the file : " + fileLocation);
                }
                // publish progress:
                setProgress(Math.round((100f * i) / size));
            }

            logger.info("loadOIFitsFiles: duration = {} ms.", 1e-6d * (System.nanoTime() - startTime));

            // Update status bar:
            StatusBar.show(n + " loaded file(s).");

            return oiFitsFiles;
        }

        /**
         * Refresh GUI invoked by the Swing Event Dispatcher Thread (Swing EDT)
         * Called by @see #done()
         * @param oifitsFiles computed data
         */
        @Override
        public void refreshUI(final List<OIFitsFile> oifitsFiles) {
            for (OIFitsFile oifitsFile : oifitsFiles) {
                // fire OIFitsCollectionChanged:
                addOIFitsFile(oifitsFile);
            }
        }
    }

    /**
     * Load the given OI Fits File with the given checker component
     * and add it to the OIFits collection
     * @param fileLocation absolute File Path or remote URL
     * @param checker checker component
     * @throws IOException if a fits file can not be loaded
     */
    public void loadOIFitsFile(final String fileLocation, final OIFitsChecker checker) throws IOException {
        cancelTaskLoadOIFits();
        addOIFitsFile(loadOIFits(fileLocation, checker));
    }

    /**
     * (Download) and load the given OI Fits File with the given checker component
     * @param fileLocation absolute File Path or remote URL
     * @param checker checker component
     * @return loaded OIFits File
     * @throws IOException if a fits file can not be loaded
     */
    private static OIFitsFile loadOIFits(final String fileLocation, final OIFitsChecker checker) throws IOException {
        //@todo test if file has already been loaded before going further ??

        final OIFitsFile oifitsFile;
        try {
            // retrieve oifits if remote or use local one
            if (FileUtils.isRemote(fileLocation)) {
                // TODO let the user customize the application file storage preference:
                final String parentPath = SessionSettingsPreferences.getApplicationFileStorage();

                final File localCopy = FileUtils.retrieveRemoteFile(fileLocation, parentPath, MimeType.OIFITS);

                if (localCopy != null) {
                    // TODO: remove StatusBar !
                    StatusBar.show("loading file: " + fileLocation + " ( local copy: " + localCopy.getAbsolutePath() + " )");

                    oifitsFile = OIFitsLoader.loadOIFits(checker, localCopy.getAbsolutePath());
                    oifitsFile.setSourceURI(new URI(fileLocation));
                } else {
                    // download failed:
                    oifitsFile = null;
                }
            } else {
                // TODO: remove StatusBar !
                StatusBar.show("loading file: " + fileLocation);

                oifitsFile = OIFitsLoader.loadOIFits(checker, fileLocation);
            }
        } catch (AuthenticationException ae) {
            throw new IOException("Could not load the file : " + fileLocation, ae);
        } catch (IOException ioe) {
            throw new IOException("Could not load the file : " + fileLocation, ioe);
        } catch (FitsException fe) {
            throw new IOException("Could not load the file : " + fileLocation, fe);
        } catch (URISyntaxException use) {
            throw new IOException("Could not load the file : " + fileLocation, use);
        }

        if (oifitsFile == null) {
            throw new IOException("Could not load the file : " + fileLocation);
        }
        return oifitsFile;
    }

    /**
     * Return the current OIFits explorer collection file
     * @return the current OIFits explorer collection file or null if undefined
     */
    public File getOiFitsCollectionFile() {
        return this.oiFitsCollectionFile;
    }

    /**
     * Private : define the current OIFits explorer collection file
     * @param file new OIFits explorer collection file to use
     */
    private void setOiFitsCollectionFile(final File file) {
        this.oiFitsCollectionFile = file;
    }

    // TODO: save / merge ... (elsewhere)
    /**
     * Reset the OIFits file collection and start firing events
     */
    public void start() {
        if (!enableEvents) {
            enableEvents = true;

            // Register SharedSeriesAttributes listener:
            SharedSeriesAttributes.INSTANCE_OIXP.register();

            reset(true);
        }
    }

    /**
     * Reset the OIFits file collection
     */
    public void reset() {
        reset(false);
    }

    /**
     * Reset the OIFits file collection 
     * @param doFireReady true to fire Ready event; false otherwise
     */
    public void reset(final boolean doFireReady) {
        cancelTaskLoadOIFits();

        userCollection = new OiDataCollection();
        userCollectionInitial = null;
        oiFitsCollection = new OIFitsCollection();
        oiFitsCollectionFile = null;
        selectedDataPointer = null;

        if (enableEvents) {
            fireOIFitsCollectionChanged();

            if (doFireReady) {
                // Fire the Ready event to any listener:
                fireReady(this, null);
            }
        }
    }

    /**
     * Private : define the initial user collection as the current one (deep clone)
     */
    private void defineInitialUserCollection() {
        this.userCollectionInitial = (OiDataCollection) this.userCollection.clone();
        // check and update references :
        this.userCollectionInitial.checkReferences();
    }

    /**
     * @return true if the user collection was modified since its initial state 
     */
    public boolean isUserCollectionChanged() {
        // check and update references (removes null and empty collections):
        this.userCollection.checkReferences();
        
        // perform the complete graph comparison (but version):
        return ((userCollectionInitial != null) && !userCollectionInitial.equals(this.userCollection, false));
    }

    /**
     * Add an OIDataFile given its corresponding OIFits structure
     * @param oiFitsFile OIFits structure
     * @return true if an OIDataFile was added
     */
    public boolean addOIFitsFile(final OIFitsFile oiFitsFile) {
        if (oiFitsFile != null) {
            // check if already present in collection:
            if (oiFitsCollection.addOIFitsFile(oiFitsFile) == null) {

                // Add new OIDataFile in collection
                final OIDataFile dataFile = new OIDataFile();

                String id = StringUtils.replaceNonAlphaNumericCharsByUnderscore(oiFitsFile.getFileName());

                // make the id unique with a _bisN suffix
                final String idSuffix = "_bis";

                while (Identifiable.hasIdentifiable(id, getOIDataFileList())) {
                    int index = id.lastIndexOf(idSuffix);
                    int number = 1;
                    if (index != -1) {
                        String strNumber = id.substring(index + idSuffix.length());
                        id = id.substring(0, index);
                        try {
                            number = Integer.parseInt(strNumber);
                            number++;
                        } catch (NumberFormatException nfe) {
                            logger.debug("Unable to parse '{}'", strNumber);
                        }
                    }
                    id += idSuffix + number;
                }

                dataFile.setId(id);
                dataFile.setName(oiFitsFile.getFileName());
                dataFile.setFile(OIFitsCollection.getFilePath(oiFitsFile));
                // checksum !

                // store oiFitsFile reference:
                dataFile.setOIFitsFile(oiFitsFile);

                addOIDataFileRef(dataFile);
            } else {
                // OIFitsFile updated:
                final OIDataFile dataFile = getOIDataFile(oiFitsFile);
                if (dataFile != null) {
                    // checksum !

                    // update oiFitsFile reference:
                    dataFile.setOIFitsFile(oiFitsFile);
                }
            }

            fireOIFitsCollectionChanged();

            return true;
        }
        return false;
    }

    /**
     * Remove the OIDataFile given its corresponding OIFits structure (filePath matching)
     * @param oiFitsFile OIFits structure
     * @param fireEvent if true, an event will be fired
     * @return removed OIDataFile or null if not found
     */
    private OIFitsFile removeOIFitsFile(final OIFitsFile oiFitsFile, final boolean fireEvent) {
        final OIFitsFile previous = this.oiFitsCollection.removeOIFitsFile(oiFitsFile);

        if (previous != null) {
            // Remove OiDataFile from user collection
            final OIDataFile dataFile = getOIDataFile(oiFitsFile);
            if (dataFile != null) {
                // reset oiFitsFile reference:
                dataFile.setOIFitsFile(null);

                removeOIDataFile(dataFile.getId());
            }

            if (fireEvent) {
                // collection changed event will remove remaining OIDataFile references:
                fireOIFitsCollectionChanged();
            }
        }
        return previous;
    }

    /**
     * alias of removeOIFitsFile with fireEvent set to true. Remove the OIDataFile given its corresponding OIFits
     * structure (filePath matching)
     *
     * @param oiFitsFile OIFits structure
     * @return removed OIDataFile or null if not found
     */
    public OIFitsFile removeOIFitsFile(final OIFitsFile oiFitsFile) {
        return removeOIFitsFile(oiFitsFile, true);
    }

    /**
     * removes from collection every OIFits file of the given list. Then fires an event.
     *
     * @param listOIfitsfiles list of OIFits files to remove
     * @return list of effectively deleted OIFits files
     */
    public List<OIFitsFile> removeOIFitsFileList(final List<OIFitsFile> listOIfitsfiles) {
        final List<OIFitsFile> listPrevious = new ArrayList<>(listOIfitsfiles.size());

        for (OIFitsFile oiFitsFile : listOIfitsfiles) {
            final OIFitsFile removed = removeOIFitsFile(oiFitsFile, false);
            if (removed != null) {
                listPrevious.add(removed);
            }
        }

        if (!listPrevious.isEmpty()) {
            // collection changed event will remove remaining OIDataFile references:
            fireOIFitsCollectionChanged();
        }
        return listPrevious;
    }

    /**
     * Remove all OIDataFiles
     */
    public void removeAllOIFitsFiles() {
        this.oiFitsCollection.clear();

        getOIDataFileList().clear();

        fireOIFitsCollectionChanged();
    }

    /**
     * Protected: Return the OIFits explorer collection structure
     * @return OIFits explorer collection structure
     */
    OiDataCollection getUserCollection() {
        return userCollection;
    }

    /**
     * Protected: return the OIFits collection
     * // TODO try to make method private back and replace by event handling for datatreepanel update ( see MainPanel.updateDataTree() )
     * @return OIFits collection
     */
    public OIFitsCollection getOIFitsCollection() {
        return oiFitsCollection;
    }

    /* --- Expression interpretation on the OIFitsCollection ----------- */
    /**
     * Make the creation or modification of a column given its name and expression
     * @param name name of the column
     * @param expression expression of the column
     */
    public void updateExprColumnInOIFitsCollection(final String name, final String expression) {
        updateExprColumnInOIFitsCollection(name, expression, false);

        // fire Collection changed to force updating subset's result data model:
        // it will clear oifits collection cache too:
        fireOIFitsCollectionChanged();
    }

    /**
     * Remove the column given its name.
     * @param name name of the column
     */
    public void removeExprColumnInOIFitsCollection(final String name) {
        updateExprColumnInOIFitsCollection(name, null, true);

        // fire Collection changed to force updating subset's result data model:
        fireOIFitsCollectionChanged();
    }

    /**
     * Update or remove the column given its name.
     * Note: for updates, it will verify the expression
     * and perform computation on all tables present in all OIFitsCollections
     * @param name name of the column
     * @param expression expression of the column
     * @param remove true to remove the column; false to update the column
     */
    private void updateExprColumnInOIFitsCollection(final String name, final String expression,
                                                    final boolean remove) {

        final String realName = "[" + name + "]";
        logger.debug("updateExprColumnInOIFitsCollection: name = {} remove = {}", realName, remove);

        final int nTableTypes = 4;

        final boolean[] working = new boolean[nTableTypes];

        if (!remove) {
            // Check expression:
            int n = 0;
            int nOk = 0;
            int nKo = 0;

            OIVis vis = null;
            OIVis2 vis2 = null;
            OIT3 t3 = null;
            OIFlux flux = null;

            final String[] messages = new String[nTableTypes];

            for (OIFitsFile oiFitsFile : oiFitsCollection.getOIFitsFiles()) {
                logger.debug("oiFitsFile: {}", oiFitsFile);

                // try OI_VIS:
                if ((vis == null) && (oiFitsFile.hasOiVis())) {
                    vis = oiFitsFile.getOiVis()[0];
                    n++;
                    try {
                        vis.checkExpression(realName, expression);
                        working[0] = true;
                        nOk++;
                    } catch (IllegalStateException ise) {
                        // fatal error:
                        throw ise;
                    } catch (RuntimeException re) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("exception OI_VIS: {}", re.getMessage(), re);
                        }
                        messages[0] = re.getMessage();
                        nKo++;
                    }
                }
                // try OI_VIS2:
                if ((vis2 == null) && (oiFitsFile.hasOiVis2())) {
                    vis2 = oiFitsFile.getOiVis2()[0];
                    n++;
                    try {
                        vis2.checkExpression(realName, expression);
                        working[1] = true;
                        nOk++;
                    } catch (IllegalStateException ise) {
                        // fatal error:
                        throw ise;
                    } catch (RuntimeException re) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("exception OI_VIS2: {}", re.getMessage(), re);
                        }
                        messages[1] = re.getMessage();
                        nKo++;
                    }
                }
                // try OI_T3:
                if ((t3 == null) && (oiFitsFile.hasOiT3())) {
                    t3 = oiFitsFile.getOiT3()[0];
                    n++;
                    try {
                        t3.checkExpression(realName, expression);
                        working[2] = true;
                        nOk++;
                    } catch (IllegalStateException ise) {
                        // fatal error:
                        throw ise;
                    } catch (RuntimeException re) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("exception OI_T3: {}", re.getMessage(), re);
                        }
                        messages[2] = re.getMessage();
                        nKo++;
                    }
                }
                // try OI_FLUX:
                if ((flux == null) && (oiFitsFile.hasOiFlux())) {
                    flux = oiFitsFile.getOiFlux()[0];
                    n++;
                    try {
                        flux.checkExpression(realName, expression);
                        working[3] = true;
                        nOk++;
                    } catch (IllegalStateException ise) {
                        // fatal error:
                        throw ise;
                    } catch (RuntimeException re) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("exception OI_T3: {}", re.getMessage(), re);
                        }
                        messages[3] = re.getMessage();
                        nKo++;
                    }
                }
                if (n == nTableTypes) {
                    break;
                }
            }

            // Bilan des courses:
            if (nKo != 0 && nOk == 0) {

                final Map<String, List<Integer>> mapError = new HashMap< String, List<Integer>>(8);

                for (int i = 0; i < 3; i++) {
                    if (messages[i] != null) {
                        final List<Integer> indices;
                        if (mapError.containsKey(messages[i])) {
                            indices = mapError.get(messages[i]);
                        } else {
                            indices = new ArrayList<Integer>(3);
                            mapError.put(messages[i], indices);
                        }
                        indices.add(i);
                    }
                }

                final StringBuilder sb = new StringBuilder(256);
                sb.append("Unable to evaluate the expression: '").append(expression);
                sb.append("'\n\n");

                if (mapError.size() == 1) {
                    sb.append(mapError.keySet().iterator().next());
                } else {
                    for (Map.Entry<String, List<Integer>> entry : mapError.entrySet()) {
                        sb.append(entry.getKey());
                        sb.append(" in table : ");
                        for (Integer j : entry.getValue()) {
                            switch (j) {
                                case 0:
                                    sb.append(" OI_VIS ");
                                    break;
                                case 1:
                                    sb.append(" OI_VIS2 ");
                                    break;
                                case 2:
                                    sb.append(" OI_T3 ");
                                    break;
                                case 3:
                                    sb.append(" OI_FLUX ");
                                    break;
                                default:
                                    break;
                            }
                            sb.append(" \n");
                        }
                    }
                }
                sb.append("\n");

                MessagePane.showErrorMessage(sb.toString());
                return;
            }
        } // test expression

        final long startTime = System.nanoTime();

        // TODO: what OIData should be processed ? current subset or all data ?
        for (OIFitsFile oiFitsFile : oiFitsCollection.getOIFitsFiles()) {
            logger.debug("oiFitsFile: {}", oiFitsFile);

            for (OIData oiData : oiFitsFile.getOiDataList()) {
                logger.debug("oiData: {}", oiData);

                if (oiData != null) {
                    if (remove) {
                        oiData.removeExpressionColumn(realName);
                    } else {
                        // only compute expression on working tables:
                        if ((working[0] && (oiData instanceof OIVis))
                                || (working[1] && (oiData instanceof OIVis2))
                                || (working[2] && (oiData instanceof OIT3))
                                || (working[3] && (oiData instanceof OIFlux))) {
                            oiData.updateExpressionColumn(realName, expression);
                        } else {
                            oiData.removeExpressionColumn(realName);
                        }
                    }
                }
            }
        }

        if (!remove) {
            logger.info("updateExprColumnInOIFitsCollection[{}] computation time = {} ms.",
                    expression, 1e-6d * (System.nanoTime() - startTime));
        }
        logger.debug("updateExprColumnInOIFitsCollection: done.");
    }

    /* --- file handling ------------------------------------- */
    /**
     * Return the OIDataFile list (reference)
     * @return OIDataFile list (reference)
     */
    List<OIDataFile> getOIDataFileList() {
        return this.userCollection.getFiles();
    }

    /**
     * Return the OIDataFile identifiers
     * @return identifiers
     */
    public List<String> getOIDataFileIds() {
        return Identifiable.getIds(getOIDataFileList());
    }

    /**
     * Return an OIDataFile given its identifier
     * @param id OIDataFile identifier
     * @return OIDataFile or null if not found
     */
    public OIDataFile getOIDataFile(final String id) {
        return Identifiable.getIdentifiable(id, getOIDataFileList());
    }

    /**
     * Return an OIDataFile given its related OIFitsFile
     * @param oiFitsFile OIFitsFile to find
     * @return OIDataFile or null if not found
     */
    public OIDataFile getOIDataFile(final OIFitsFile oiFitsFile) {
        final String filePath = OIFitsCollection.getFilePath(oiFitsFile);

        for (OIDataFile dataFile : getOIDataFileList()) {
            if (filePath.equals(dataFile.getFile())) {
                return dataFile;
            }
        }
        return null;
    }

    /**
     * Add the given OIDataFile
     * @param dataFile OIDataFile to add
     * @return true if the given OIDataFile was added
     */
    private boolean addOIDataFileRef(final OIDataFile dataFile) {
        if (logger.isDebugEnabled()) {
            logger.debug("addOIDataFileRef: {}", dataFile);
        }
        return Identifiable.addIdentifiable(dataFile, getOIDataFileList());
    }

    /**
     * Remove the OIDataFile given its identifier
     * @param id OIDataFile identifier
     * @return removed OIDataFile instance or null if the identifier was not found
     */
    private OIDataFile removeOIDataFile(final String id) {
        return Identifiable.removeIdentifiable(id, getOIDataFileList());
    }

    /* --- subset definition handling ------------------------------------- */
    /**
     * Return the subset definition list (reference)
     * @return subset definition list (reference)
     */
    public List<SubsetDefinition> getSubsetDefinitionList() {
        return this.userCollection.getSubsetDefinitions();
    }

    /**
     * Return the subset definition identifiers
     * @return identifiers
     */
    public List<String> getSubsetDefinitionIds() {
        return Identifiable.getIds(getSubsetDefinitionList());
    }

    /**
     * Return the current subset definition (copy)
     * @return subset definition (copy)
     */
    public SubsetDefinition getCurrentSubsetDefinition() {
        final SubsetDefinition subsetDefinition = Identifiable.clone(getCurrentSubsetDefinitionRef());

        if (logger.isDebugEnabled()) {
            logger.debug("getCurrentSubsetDefinition {}", subsetDefinition);
        }
        return subsetDefinition;
    }

    /**
     * Return the current subset definition (reference)
     * @return subset definition (reference)
     */
    public SubsetDefinition getCurrentSubsetDefinitionRef() {
        SubsetDefinition subsetDefinition = getSubsetDefinitionRef(CURRENT_SUBSET_DEFINITION);
        if (subsetDefinition == null) {
            subsetDefinition = new SubsetDefinition();
            subsetDefinition.setId(CURRENT_SUBSET_DEFINITION);

            addSubsetDefinitionRef(subsetDefinition);
        }

        if (logger.isDebugEnabled()) {
            logger.debug("getCurrentSubsetDefinitionRef {}", subsetDefinition);
        }
        return subsetDefinition;
    }

    /**
     * Add the given SubsetDefinition
     * @param subsetDefinition SubsetDefinition to add
     * @return true if the given SubsetDefinition was added
     */
    public boolean addSubsetDefinition(final SubsetDefinition subsetDefinition) {
        if (logger.isDebugEnabled()) {
            logger.debug("addSubsetDefinition: {}", subsetDefinition);
        }

        if (addSubsetDefinitionRef(subsetDefinition)) {
            // update subset reference and fire events (SubsetDefinitionChanged, PlotChanged):
            updateSubsetDefinitionRef(this, subsetDefinition);
            return true;
        }
        return false;
    }

    /**
     * Add the given SubsetDefinition
     * @param subsetDefinition SubsetDefinition to add
     * @return true if the given SubsetDefinition was added
     */
    private boolean addSubsetDefinitionRef(final SubsetDefinition subsetDefinition) {
        if (logger.isDebugEnabled()) {
            logger.debug("addSubsetDefinitionRef: {}", subsetDefinition);
        }
        if (Identifiable.addIdentifiable(subsetDefinition, getSubsetDefinitionList())) {
            fireSubsetDefinitionListChanged();
            return true;
        }
        return false;
    }

    /**
     * Remove the SubsetDefinition given its identifier
     * @param id SubsetDefinition identifier
     * @return removed SubsetDefinition instance or null if the identifier was not found
     */
    private SubsetDefinition removeSubsetDefinition(final String id) {
        final SubsetDefinition subsetDefinition = Identifiable.removeIdentifiable(id, getSubsetDefinitionList());
        if (subsetDefinition != null) {
            fireSubsetDefinitionListChanged();
        }
        return subsetDefinition;
    }

    /**
     * Return a subset definition (copy) by its identifier
     * @param id subset definition id
     * @return subset definition (copy) or null if not found
     */
    public SubsetDefinition getSubsetDefinition(final String id) {
        final SubsetDefinition subsetDefinition = Identifiable.clone(getSubsetDefinitionRef(id));

        if (logger.isDebugEnabled()) {
            logger.debug("getSubsetDefinition {}", subsetDefinition);
        }
        return subsetDefinition;
    }

    /**
     * Return a subset definition (reference) by its identifier
     * @param id subsetDefinition identifier
     * @return subset definition (reference) or null if not found
     */
    public SubsetDefinition getSubsetDefinitionRef(final String id) {
        return Identifiable.getIdentifiable(id, getSubsetDefinitionList());
    }

    /**
     * Return true if this subset definition exists in this data collection given its identifier
     * @param id subset definition identifier
     * @return true if this subset definition exists in this data collection given its identifier
     */
    public boolean hasSubsetDefinition(final String id) {
        return getSubsetDefinitionRef(id) != null;
    }

    /**
     * Update the subset definition corresponding to the same name
     * @param source event source
     * @param subsetDefinition subset definition with updated values
     */
    public void updateSubsetDefinition(final Object source, final SubsetDefinition subsetDefinition) {
        final SubsetDefinition subset = getSubsetDefinitionRef(subsetDefinition.getId());

        if (subset == null) {
            throw new IllegalStateException("subset not found : " + subsetDefinition);
        }

        boolean changed = false;

        if (subset != subsetDefinition) {
            changed = !ObjectUtils.areEquals(subset, subsetDefinition);
        } else {
            throw new IllegalStateException("equal subset references : " + subset);
        }

        if (logger.isDebugEnabled()) {
            logger.debug("updateSubsetDefinition: {}", subsetDefinition);
            logger.debug("updateSubsetDefinition: changed: {}", changed);
        }

        if (changed) {
            subset.copy(subsetDefinition); // full copy

            // update subset reference and fire events (SubsetDefinitionChanged, PlotChanged):
            updateSubsetDefinitionRef(source, subset);
        }
    }

    /**
     * Update the given subset definition (reference) and fire events
     * @param source event source
     * @param subsetDefinition subset definition (reference)
     */
    private void updateSubsetDefinitionRef(final Object source, final SubsetDefinition subsetDefinition) {
        if (logger.isDebugEnabled()) {
            logger.debug("updateSubsetDefinitionRef: subsetDefinition: {}", subsetDefinition);
        }

        final SelectorResult result = findOIData(subsetDefinition);

        if (result != null) {
            // Copy used StaNames (all files):
            result.setUsedStaNamesMap(oiFitsCollection.getUsedStaNamesMap());
        }

        if (logger.isDebugEnabled()) {
            logger.debug("updateSubsetDefinitionRef: result: {}", result);
        }

        // store selector result in the subset:
        subsetDefinition.setSelectorResult(result);
        subsetDefinition.incVersion();

        fireSubsetDefinitionChanged(source, subsetDefinition.getId());

        // find dependencies:
        for (Plot plot : getPlotList()) {
            if (plot.getSubsetDefinition() != null && plot.getSubsetDefinition().getId().equals(subsetDefinition.getId())) {
                // update subset definition reference:
                plot.setSubsetDefinition(subsetDefinition);

                // update plot version and fire events (PlotChanged):
                updatePlotRef(source, plot);
            }
        }
    }

    /**
     * Export current subset as a single OIFITS file (Merger in action)
     * @return 
     */
    public OIFitsFile createOIFitsFromCurrentSubsetDefinition() {
        final SubsetDefinition subsetDefinition = getCurrentSubsetDefinitionRef();

        // reuse selector result from current subset definition:
        final SelectorResult result = subsetDefinition.getSelectorResult();

        final OIFitsFile oiFitsFile = Merger.process(result);
        if (oiFitsFile != null) {
            oiFitsFile.analyze();
        }
        return oiFitsFile;
    }

    private SelectorResult findOIData(final SubsetDefinition subsetDefinition) {
        final List<GenericFilter> filters = subsetDefinition.getGenericFilters();

        final int len = filters.size();

        final FilterValues<Range> wavelengthValues;
        final FilterValues<Range> mjdValues;

        final Map<String, FilterValues<Range>> filtersRangeValuesExtra;
        final Map<String, FilterValues<String>> filtersStrValuesExtra;

        if (len > 0) {
            // translate SubsetDefinition's generic filters into Selector's wavelength and mjd ranges
            wavelengthValues = getFilterRangeValues(filters, Selector.FILTER_EFFWAVE);
            mjdValues = getFilterRangeValues(filters, Selector.FILTER_MJD);

            filtersRangeValuesExtra = new LinkedHashMap<>(len * 2);
            filtersStrValuesExtra = new LinkedHashMap<>(4);

            for (GenericFilter genericFilter : filters) {
                final String columnName = genericFilter.getColumnName();

                switch (genericFilter.getDataType()) {
                    case NUMERIC:
                        final FilterValues<Range> rangeValues = getFilterRangeValues(filters, columnName);
                        if (rangeValues != null) {
                            filtersRangeValuesExtra.put(columnName, rangeValues);
                        }
                        break;
                    case STRING:
                        final FilterValues<String> strValues = getFilterStringValues(filters, columnName);
                        if (strValues != null) {
                            filtersStrValuesExtra.put(columnName, strValues);
                        }
                        break;
                    default:
                }
            }
            logger.debug("filtersRangesExtra : {}", filtersRangeValuesExtra);
            logger.debug("filtersValuesExtra : {}", filtersStrValuesExtra);
        } else {
            wavelengthValues = null;
            mjdValues = null;
            filtersRangeValuesExtra = null;
            filtersStrValuesExtra = null;
        }

        SelectorResult result = null;
        final Selector selector = new Selector();

        for (SubsetFilter filter : subsetDefinition.getFilters()) {
            selector.reset();

            // Target:
            selector.setTargetUID(filter.getTargetUID());

            // InstrumentModes:
            selector.setInsModeUIDs(filter.getInsModeUIDs());

            // NightIds:
            selector.setNightIDs(filter.getNightIDs());

            // Tables:
            if (!filter.getTables().isEmpty()) {
                for (TableUID tableUID : filter.getTables()) {
                    selector.addTable(tableUID.getFile().getFile(), tableUID.getExtNb());
                }
            }

            // Extra filters from generic filters:
            if (wavelengthValues != null) {
                selector.addFilter(Selector.FILTER_EFFWAVE, wavelengthValues);
            }
            if (mjdValues != null) {
                selector.addFilter(Selector.FILTER_MJD, mjdValues);
            }
            if ((filtersRangeValuesExtra != null) && !filtersRangeValuesExtra.isEmpty()) {
                for (Map.Entry<String, FilterValues<Range>> e : filtersRangeValuesExtra.entrySet()) {
                    selector.addFilter(e.getKey(), e.getValue());
                }
            }
            if ((filtersStrValuesExtra != null) && !filtersStrValuesExtra.isEmpty()) {
                for (Map.Entry<String, FilterValues<String>> e : filtersStrValuesExtra.entrySet()) {
                    selector.addFilter(e.getKey(), e.getValue());
                }
            }

            // Query OIData matching criteria:
            result = this.oiFitsCollection.findOIData(selector, result);
        }
        return result;
    }

    private static FilterValues<Range> getFilterRangeValues(final List<GenericFilter> filters, final String columnName) {
        FilterValues<Range> filterValues = null;

        for (GenericFilter filter : filters) {
            if (!filter.isEnabled()) {
                continue; // skip disabled filter
            }

            if (columnName.equals(filter.getColumnName())) {
                if (filterValues == null) {
                    filterValues = new FilterValues<Range>(columnName);
                }
                final List<Range> list = (filter.isInclusive()) ? filterValues.getOrCreateIncludeValues()
                        : filterValues.getOrCreateExcludeValues();

                // we convert every generic filter's ranges into oitools' ranges
                for (fr.jmmc.oiexplorer.core.model.plot.Range range : filter.getAcceptedRanges()) {
                    list.add(new Range(range.getMin(), range.getMax()));
                }
            }
        }
        return filterValues;
    }

    /* merging values of all filter of the column name `key` */
    private static FilterValues<String> getFilterStringValues(final List<GenericFilter> filters, final String columnName) {
        FilterValues<String> filterValues = null;

        for (GenericFilter filter : filters) {
            if (!filter.isEnabled()) {
                continue; // skip disabled generic filters
            }

            if (columnName.equals(filter.getColumnName())) {
                if (filterValues == null) {
                    filterValues = new FilterValues<String>(columnName);
                }
                final List<String> list = (filter.isInclusive()) ? filterValues.getOrCreateIncludeValues()
                        : filterValues.getOrCreateExcludeValues();

                list.addAll(filter.getAcceptedValues());
            }
        }
        return filterValues;
    }

    /* --- plot definition handling --------- ---------------------------- */
    /**
     * Return the plot definition list (reference)
     * @return plot definition list (reference)
     */
    List<PlotDefinition> getPlotDefinitionList() {
        return this.userCollection.getPlotDefinitions();
    }

    /**
     * Return the plot definition identifiers
     * @return identifiers
     */
    public List<String> getPlotDefinitionIds() {
        return Identifiable.getIds(getPlotDefinitionList());
    }

    /**
     * Return the current plot definition (copy)
     * @return plot definition (copy)
     */
    public PlotDefinition getCurrentPlotDefinition() {
        final PlotDefinition plotDefinition = Identifiable.clone(getCurrentPlotDefinitionRef());

        if (logger.isDebugEnabled()) {
            logger.debug("getCurrentPlotDefinition {}", plotDefinition);
        }
        return plotDefinition;
    }

    /**
     * Return the current plot definition (reference)
     * @return plot definition (reference)
     */
    public PlotDefinition getCurrentPlotDefinitionRef() {
        PlotDefinition plotDefinition = getPlotDefinitionRef(CURRENT_PLOT_DEFINITION);
        if (plotDefinition == null) {
            plotDefinition = new PlotDefinition();
            plotDefinition.setId(CURRENT_PLOT_DEFINITION);

            // copy values:
            plotDefinition.copyValues(plotDefFactory.getDefault(PlotDefinitionFactory.PLOT_DEFAULT));

            addPlotDefinitionRef(plotDefinition);
        }
        return plotDefinition;
    }

    /**
     * Add the given PlotDefinition
     * @param plotDefinition PlotDefinition to add
     * @return true if the given PlotDefinition was added
     */
    public boolean addPlotDefinition(final PlotDefinition plotDefinition) {
        if (logger.isDebugEnabled()) {
            logger.debug("addPlotDefinition: {}", plotDefinition);
        }

        if (addPlotDefinitionRef(plotDefinition)) {
            // update plot definition version and fire events (PlotDefinitionChanged, PlotChanged):
            updatePlotDefinitionRef(this, plotDefinition);
            return true;
        }
        return false;
    }

    /**
     * Add the given PlotDefinition
     * @param plotDefinition PlotDefinition to add
     * @return true if the given PlotDefinition was added
     */
    private boolean addPlotDefinitionRef(final PlotDefinition plotDefinition) {
        if (logger.isDebugEnabled()) {
            logger.debug("addPlotDefinitionRef: {}", plotDefinition);
        }
        if (Identifiable.addIdentifiable(plotDefinition, getPlotDefinitionList())) {
            firePlotDefinitionListChanged();
            return true;
        }
        return false;
    }

    /**
     * Remove the PlotDefinition given its identifier
     * @param id PlotDefinition identifier
     * @return removed PlotDefinition instance or null if the identifier was not found
     */
    private PlotDefinition removePlotDefinition(final String id) {
        return Identifiable.removeIdentifiable(id, getPlotDefinitionList());
    }

    /**
     * Return a plot definition (copy) by its identifier
     * @param id plot identifier
     * @return plot definition (copy) or null if not found
     */
    public PlotDefinition getPlotDefinition(final String id) {
        final PlotDefinition plotDefinition = Identifiable.clone(getPlotDefinitionRef(id));

        if (logger.isDebugEnabled()) {
            logger.debug("getPlotDefinition {}", plotDefinition);
        }
        return plotDefinition;
    }

    /**
     * Return a plot definition (reference) by its identifier
     * @param id plot definition identifier
     * @return plot definition (reference) or null if not found
     */
    public PlotDefinition getPlotDefinitionRef(final String id) {
        return Identifiable.getIdentifiable(id, getPlotDefinitionList());
    }

    /**
     * Return true if this plot definition exists in this data collection given its identifier
     * @param id plot definition identifier
     * @return true if this plot definition exists in this data collection given its identifier
     */
    public boolean hasPlotDefinition(final String id) {
        return getPlotDefinitionRef(id) != null;
    }

    /**
     * Update the plot definition corresponding to the same name
     * @param source event source
     * @param plotDefinition plot definition with updated values
     */
    public void updatePlotDefinition(final Object source, final PlotDefinition plotDefinition) {
        final PlotDefinition plotDef = getPlotDefinitionRef(plotDefinition.getId());

        if (plotDef == null) {
            throw new IllegalStateException("plot definition not found : " + plotDefinition);
        }

        boolean changed = false;

        if (plotDef != plotDefinition) {
            changed = !ObjectUtils.areEquals(plotDef, plotDefinition);
        } else {
            throw new IllegalStateException("equal plot definition references : " + plotDef);
        }

        if (logger.isDebugEnabled()) {
            logger.debug("updatePlotDefinition: {}", plotDefinition);
            logger.debug("updatePlotDefinition: changed: {}", changed);
        }

        if (changed) {
            plotDef.copy(plotDefinition); // full copy

            // update plot definition version and fire events (PlotDefinitionChanged, PlotChanged):
            updatePlotDefinitionRef(source, plotDefinition);
        }
    }

    /**
     * Update the given plot definition (reference) and fire events
     * @param source event source
     * @param plotDefinition plot definition (reference)
     */
    private void updatePlotDefinitionRef(final Object source, final PlotDefinition plotDefinition) {
        if (logger.isDebugEnabled()) {
            logger.debug("updatePlotDefinitionRef: plotDefinition: {}", plotDefinition);
        }

        plotDefinition.incVersion();
        firePlotDefinitionChanged(source, plotDefinition.getId());

        // find dependencies:
        for (Plot plot : getPlotList()) {
            if (plot.getPlotDefinition() != null && plot.getPlotDefinition().getId().equals(plotDefinition.getId())) {
                // update plot definition reference:
                plot.setPlotDefinition(plotDefinition);

                // update plot version and fire events (PlotChanged):
                updatePlotRef(source, plot);
            }
        }
    }

    /* --- plot handling --------- ---------------------------- */
    /**
     * Return the plot list (reference)
     * @return plot list (reference)
     */
    List<Plot> getPlotList() {
        return this.userCollection.getPlots();
    }

    /**
     * Return the plot identifiers
     * @return identifiers
     */
    public List<String> getPlotIds() {
        return Identifiable.getIds(getPlotList());
    }

    /**
     * Return the current plot (copy)
     * @return plot (copy)
     */
    public Plot getCurrentPlot() {
        final Plot plot = Identifiable.clone(getCurrentPlotRef());

        if (logger.isDebugEnabled()) {
            logger.debug("getCurrentPlot {}", plot);
        }
        return plot;
    }

    /**
     * Return the current plot (reference)
     * @return plot (reference)
     */
    public Plot getCurrentPlotRef() {
        Plot plot = getPlotRef(CURRENT_VIEW);
        if (plot == null) {
            plot = new Plot();
            plot.setId(CURRENT_VIEW);

            // define current pointers:
            plot.setSubsetDefinition(getCurrentSubsetDefinitionRef());
            plot.setPlotDefinition(getCurrentPlotDefinitionRef());

            addPlotRef(plot);
        }
        return plot;
    }

    /**
     * Add the given Plot
     * @param plot Plot to add
     * @return true if the given Plot was added
     */
    public boolean addPlot(final Plot plot) {
        if (logger.isDebugEnabled()) {
            logger.debug("addPlot: {}", plot);
        }

        if (addPlotRef(plot)) {
            // update plot version and fire events (PlotChanged):
            updatePlotRef(this, plot);
            return true;
        }
        return false;
    }

    /**
     * Add the given Plot
     * @param plot Plot to add
     * @return true if the given Plot was added
     */
    private boolean addPlotRef(final Plot plot) {
        if (logger.isDebugEnabled()) {
            logger.debug("addPlotRef: {}", plot);
        }
        if (Identifiable.addIdentifiable(plot, getPlotList())) {
            firePlotListChanged();
            return true;
        }
        return false;
    }

    /**
     * Remove the Plot given its identifier
     * @param id Plot identifier
     * @return true if the given Plot was removed
     */
    public boolean removePlot(final String id) {
        if (logger.isDebugEnabled()) {
            logger.debug("removePlot: {}", id);
        }
        Plot p = Identifiable.removeIdentifiable(id, getPlotList());
        if (p != null) {
            // try to cleanup associated elements
            final SubsetDefinition subsetDefinition = p.getSubsetDefinition();

            if (subsetDefinition != null) {
                boolean found = false;
                // find dependencies:
                for (Plot plot : getPlotList()) {
                    if (plot.getSubsetDefinition() != null && plot.getSubsetDefinition().getId().equals(subsetDefinition.getId())) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    // remove orphans:
                    removeSubsetDefinition(subsetDefinition.getId());
                }
            }
            final PlotDefinition plotDefinition = p.getPlotDefinition();

            if (plotDefinition != null) {
                boolean found = false;
                // find dependencies:
                for (Plot plot : getPlotList()) {
                    if (plot.getPlotDefinition() != null && plot.getPlotDefinition().getId().equals(plotDefinition.getId())) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    // remove orphans:
                    removePlotDefinition(plotDefinition.getId());
                }
            }

            // check and update references in current OiDataCollection:
            // initialize current objects: subsetDefinition, plotDefinition, plot if NOT PRESENT:
            checkReferences();

            firePlotListChanged();
            return true;
        }
        return false;
    }

    /**
     * Return a plot (copy) by its identifier
     * @param id plot identifier
     * @return plot (copy) or null if not found
     */
    public Plot getPlot(final String id) {
        final Plot plot = Identifiable.clone(getPlotRef(id));

        if (logger.isDebugEnabled()) {
            logger.debug("getPlot {}", plot);
        }
        return plot;
    }

    /**
     * Return a plot (reference) by its identifier
     * @param id plot identifier
     * @return plot (reference) or null if not found
     */
    public Plot getPlotRef(final String id) {
        return Identifiable.getIdentifiable(id, getPlotList());
    }

    /**
     * Return true if this plot exists in this data collection given its identifier
     * @param id plot identifier
     * @return true if this plot exists in this data collection given its identifier
     */
    public boolean hasPlot(final String id) {
        return getPlotRef(id) != null;
    }

    /**
     * Update the plot corresponding to the same name
     * @param source event source
     * @param plot plot with updated values
     */
    public void updatePlot(final Object source, final Plot plot) {
        final Plot plotRef = getPlotRef(plot.getId());

        if (plotRef == null) {
            throw new IllegalStateException("plot not found : " + plot);
        }

        boolean changed = false;

        if (plotRef != plot) {
            changed = !ObjectUtils.areEquals(plotRef, plot);
        } else {
            throw new IllegalStateException("equal plot references : " + plotRef);
        }

        if (logger.isDebugEnabled()) {
            logger.debug("updatePlot: {}", plot);
            logger.debug("updatePlot: changed: {}", changed);
        }

        if (changed) {
            plotRef.copy(plot); // full copy

            // update plot version and fire events (PlotChanged):
            updatePlotRef(source, plot);
        }
    }

    /**
     * Update the given plot (reference) and fire events
     * @param source event source
     * @param plot plot (reference)
     */
    private void updatePlotRef(final Object source, final Plot plot) {
        if (logger.isDebugEnabled()) {
            logger.debug("updatePlotRef: plot: {}", plot);
        }

        plot.incVersion();
        firePlotChanged(source, plot.getId());
    }

    /* --- selection handling --------- ---------------------------- */
    /**
     * Define the selection
     * @param source event source
     * @param ptr data pointer
     * @return true if the selection changed
     */
    public boolean setSelection(final Object source, final DataPointer ptr) {
        if (logger.isDebugEnabled()) {
            logger.debug("setSelection: {}", ptr);
        }

        if (!ObjectUtils.areEquals(ptr, selectedDataPointer)) {
            // Maybe leak ?
            this.selectedDataPointer = ptr;

            // Fire to all listeners:
            fireSelectionChanged(source, null);
            return true;
        }
        return false;
    }

    public DataPointer getSelection() {
        return this.selectedDataPointer;
    }

    /* --- plot infos handling --------- ---------------------------- */
    /**
     * Define the PlotInfos data
     * @param source event source
     * @param data PlotInfos data
     * @return true if the selection changed
     */
    public boolean setPlotInfosData(final Object source, final PlotInfosData data) {
        if (logger.isDebugEnabled()) {
            logger.debug("setPlotInfos: {}", data);
        }
        this.plotInfosData = data;

        // Fire to all listeners:
        firePlotViewportChanged(source, null);
        return true;
    }

    public PlotInfosData getPlotInfosData() {
        final PlotInfosData data = this.plotInfosData;
        this.plotInfosData = null; // gc
        return data;
    }

    // --- EVENTS ----------------------------------------------------------------
    /**
     * Unbind the given listener to ANY event
     * @param listener listener to unbind
     */
    public void unbind(final OIFitsCollectionManagerEventListener listener) {
        for (final EventNotifier<OIFitsCollectionManagerEvent, OIFitsCollectionManagerEventType, Object> eventNotifier : this.oiFitsCollectionManagerEventNotifierMap.values()) {
            eventNotifier.unregister(listener);
        }
    }

    /**
     * Bind the given listener to COLLECTION_CHANGED event and fire such event to initialize the listener properly
     * @param listener listener to bind
     */
    public void bindCollectionChanged(final OIFitsCollectionManagerEventListener listener) {
        getOiFitsCollectionChangedEventNotifier().register(listener);

        // Note: no fire COLLECTION_CHANGED event because first call to reset() fires it (at the right time i.e. not too early):
        // force fire COLLECTION_CHANGED event to initialize the listener ASAP:
        fireOIFitsCollectionChanged(null, listener);
    }

    /**
     * Return the COLLECTION_CHANGED event notifier
     * @return COLLECTION_CHANGED event notifier
     */
    private EventNotifier<OIFitsCollectionManagerEvent, OIFitsCollectionManagerEventType, Object> getOiFitsCollectionChangedEventNotifier() {
        return this.oiFitsCollectionManagerEventNotifierMap.get(OIFitsCollectionManagerEventType.COLLECTION_CHANGED);
    }

    /**
     * Bind the given listener to SUBSET_LIST_CHANGED event and fire such event to initialize the listener properly
     * @param listener listener to bind
     */
    public void bindSubsetDefinitionListChanged(final OIFitsCollectionManagerEventListener listener) {
        getSubsetDefinitionListChangedEventNotifier().register(listener);

        // force fire SUBSET_LIST_CHANGED event to initialize the listener ASAP:
        fireSubsetDefinitionListChanged(null, listener);
    }

    /**
     * Return the SUBSET_LIST_CHANGED event notifier
     * @return SUBSET_LIST_CHANGED event notifier
     */
    private EventNotifier<OIFitsCollectionManagerEvent, OIFitsCollectionManagerEventType, Object> getSubsetDefinitionListChangedEventNotifier() {
        return this.oiFitsCollectionManagerEventNotifierMap.get(OIFitsCollectionManagerEventType.SUBSET_LIST_CHANGED);
    }

    /**
     * Bind the given listener to PLOT_DEFINITION_LIST_CHANGED event and fire such event to initialize the listener properly
     * @param listener listener to bind
     */
    public void bindPlotDefinitionListChanged(final OIFitsCollectionManagerEventListener listener) {
        getPlotDefinitionListChangedEventNotifier().register(listener);

        // force fire PLOT_DEFINITION_LIST_CHANGED event to initialize the listener ASAP:
        firePlotDefinitionListChanged(null, listener);
    }

    /**
     * Return the PLOT_DEFINITION_LIST_CHANGED event notifier
     * @return PLOT_DEFINITION_LIST_CHANGED event notifier
     */
    private EventNotifier<OIFitsCollectionManagerEvent, OIFitsCollectionManagerEventType, Object> getPlotDefinitionListChangedEventNotifier() {
        return this.oiFitsCollectionManagerEventNotifierMap.get(OIFitsCollectionManagerEventType.PLOT_DEFINITION_LIST_CHANGED);
    }

    /**
     * Bind the given listener to PLOT_LIST_CHANGED event and fire such event to initialize the listener properly
     * @param listener listener to bind
     */
    public void bindPlotListChanged(final OIFitsCollectionManagerEventListener listener) {
        getPlotListChangedEventNotifier().register(listener);

        // force fire PLOT_LIST_CHANGED event to initialize the listener with current OIFitsCollection ASAP:
        firePlotListChanged(null, listener);
    }

    /**
     * Return the PLOT_LIST_CHANGED event notifier
     * @return PLOT_LIST_CHANGED event notifier
     */
    private EventNotifier<OIFitsCollectionManagerEvent, OIFitsCollectionManagerEventType, Object> getPlotListChangedEventNotifier() {
        return this.oiFitsCollectionManagerEventNotifierMap.get(OIFitsCollectionManagerEventType.PLOT_LIST_CHANGED);
    }

    /**
     * Bind the given listener to SUBSET_CHANGED event
     * @param listener listener to bind
     */
    public void bindSubsetDefinitionChanged(final OIFitsCollectionManagerEventListener listener) {
        getSubsetDefinitionChangedEventNotifier().register(listener);
    }

    /**
     * Return the SUBSET_CHANGED event notifier
     * @return SUBSET_CHANGED event notifier
     */
    private EventNotifier<OIFitsCollectionManagerEvent, OIFitsCollectionManagerEventType, Object> getSubsetDefinitionChangedEventNotifier() {
        return this.oiFitsCollectionManagerEventNotifierMap.get(OIFitsCollectionManagerEventType.SUBSET_CHANGED);
    }

    /**
     * Bind the given listener to PLOT_DEFINITION_CHANGED event
     * @param listener listener to bind
     */
    public void bindPlotDefinitionChanged(final OIFitsCollectionManagerEventListener listener) {
        getPlotDefinitionChangedEventNotifier().register(listener);
    }

    /**
     * Return the PLOT_DEFINITION_CHANGED event notifier
     * @return PLOT_DEFINITION_CHANGED event notifier
     */
    private EventNotifier<OIFitsCollectionManagerEvent, OIFitsCollectionManagerEventType, Object> getPlotDefinitionChangedEventNotifier() {
        return this.oiFitsCollectionManagerEventNotifierMap.get(OIFitsCollectionManagerEventType.PLOT_DEFINITION_CHANGED);
    }

    /**
     * Bind the given listener to PLOT_CHANGED event
     * @param listener listener to bind
     */
    public void bindPlotChanged(final OIFitsCollectionManagerEventListener listener) {
        getPlotChangedEventNotifier().register(listener);
    }

    /**
     * Return the PLOT_CHANGED event notifier
     * @return PLOT_CHANGED event notifier
     */
    private EventNotifier<OIFitsCollectionManagerEvent, OIFitsCollectionManagerEventType, Object> getPlotChangedEventNotifier() {
        return this.oiFitsCollectionManagerEventNotifierMap.get(OIFitsCollectionManagerEventType.PLOT_CHANGED);
    }

    /**
     * Bind the given listener to ACTIVE_PLOT_CHANGED event
     * @param listener listener to bind
     */
    public void bindActivePlotChanged(final OIFitsCollectionManagerEventListener listener) {
        getActivePlotChangedEventNotifier().register(listener);
    }

    /**
     * Return the ACTIVE_PLOT_CHANGED event notifier
     * @return ACTIVE_PLOT_CHANGED event notifier
     */
    private EventNotifier<OIFitsCollectionManagerEvent, OIFitsCollectionManagerEventType, Object> getActivePlotChangedEventNotifier() {
        return this.oiFitsCollectionManagerEventNotifierMap.get(OIFitsCollectionManagerEventType.ACTIVE_PLOT_CHANGED);
    }

    /**
     * Bind the given listener to SELECTION_CHANGED event
     * @param listener listener to bind
     */
    public void bindSelectionChanged(final OIFitsCollectionManagerEventListener listener) {
        getSelectionChangedEventNotifier().register(listener);
    }

    /**
     * Return the SELECTION_CHANGED event notifier
     * @return SELECTION_CHANGED event notifier
     */
    private EventNotifier<OIFitsCollectionManagerEvent, OIFitsCollectionManagerEventType, Object> getSelectionChangedEventNotifier() {
        return this.oiFitsCollectionManagerEventNotifierMap.get(OIFitsCollectionManagerEventType.SELECTION_CHANGED);
    }

    /**
     * Bind the given listener to PLOT_VIEWPORT_CHANGED event
     * @param listener listener to bind
     */
    public void bindPlotViewportChanged(final OIFitsCollectionManagerEventListener listener) {
        getPlotViewportChangedEventNotifier().register(listener);
    }

    /**
     * Return the PLOT_VIEWPORT_CHANGED event notifier
     * @return PLOT_VIEWPORT_CHANGED event notifier
     */
    private EventNotifier<OIFitsCollectionManagerEvent, OIFitsCollectionManagerEventType, Object> getPlotViewportChangedEventNotifier() {
        return this.oiFitsCollectionManagerEventNotifierMap.get(OIFitsCollectionManagerEventType.PLOT_VIEWPORT_CHANGED);
    }

    /**
     * Bind the given listener to READY event
     * @param listener listener to bind
     */
    public void bindReadyEvent(final OIFitsCollectionManagerEventListener listener) {
        getReadyEventNotifier().register(listener);
    }

    /**
     * Return the READY event notifier
     * @return READY event notifier
     */
    private EventNotifier<OIFitsCollectionManagerEvent, OIFitsCollectionManagerEventType, Object> getReadyEventNotifier() {
        return this.oiFitsCollectionManagerEventNotifierMap.get(OIFitsCollectionManagerEventType.READY);
    }

    /**
     * This fires an COLLECTION_CHANGED event to given registered listener ASYNCHRONOUSLY !
     *
     * Note: this is ONLY useful to initialize new registered listeners properly !
     *
     * @param source event source
     * @param destination destination listener (null means all)
     */
    public void fireOIFitsCollectionChanged(final Object source, final OIFitsCollectionManagerEventListener destination) {
        if (enableEvents) {
            if (logger.isDebugEnabled()) {
                logger.debug("fireOIFitsCollectionChanged TO {}", (destination != null) ? destination : "ALL");
            }
            getOiFitsCollectionChangedEventNotifier().queueEvent((source != null) ? source : this,
                    new OIFitsCollectionManagerEvent(OIFitsCollectionManagerEventType.COLLECTION_CHANGED, null), destination);
        }
    }

    /**
     * This fires an COLLECTION_CHANGED event to given registered listeners ASYNCHRONOUSLY !
     */
    public void fireOIFitsCollectionChanged() {
        fireOIFitsCollectionChanged(this, null);
    }

    /**
     * This fires a SUBSET_LIST_CHANGED event to given registered listener ASYNCHRONOUSLY !
     *
     * Note: this is ONLY useful to initialize new registered listeners properly !
     *
     * @param source event source
     * @param destination destination listener (null means all)
     */
    public void fireSubsetDefinitionListChanged(final Object source, final OIFitsCollectionManagerEventListener destination) {
        if (enableEvents) {
            if (logger.isDebugEnabled()) {
                logger.debug("fireSubsetDefinitionListChanged");
            }
            getSubsetDefinitionListChangedEventNotifier().queueEvent((source != null) ? source : this,
                    new OIFitsCollectionManagerEvent(OIFitsCollectionManagerEventType.SUBSET_LIST_CHANGED, null), destination);
        }
    }

    /**
     * This fires an SUBSET_LIST_CHANGED event to given registered listeners ASYNCHRONOUSLY !
     */
    private void fireSubsetDefinitionListChanged() {
        fireSubsetDefinitionListChanged(this, null);
    }

    /**
     * This fires a PLOT_DEFINITION_LIST_CHANGED event to given registered listener ASYNCHRONOUSLY !
     *
     * Note: this is ONLY useful to initialize new registered listeners properly !
     *
     * @param source event source
     * @param destination destination listener (null means all)
     */
    public void firePlotDefinitionListChanged(final Object source, final OIFitsCollectionManagerEventListener destination) {
        if (enableEvents) {
            if (logger.isDebugEnabled()) {
                logger.debug("firePlotDefinitionListChanged");
            }
            getPlotDefinitionListChangedEventNotifier().queueEvent((source != null) ? source : this,
                    new OIFitsCollectionManagerEvent(OIFitsCollectionManagerEventType.PLOT_DEFINITION_LIST_CHANGED, null), destination);
        }
    }

    /**
     * This fires a PLOT_DEFINITION_LIST_CHANGED event to all registered listeners ASYNCHRONOUSLY !
     */
    private void firePlotDefinitionListChanged() {
        firePlotDefinitionListChanged(this, null);
    }

    /**
     * This fires a PLOT_LIST_CHANGED event to given registered listener ASYNCHRONOUSLY !
     *
     * Note: this is ONLY useful to initialize new registered listeners properly !
     *
     * @param source event source
     * @param destination destination listener (null means all)
     */
    public void firePlotListChanged(final Object source, final OIFitsCollectionManagerEventListener destination) {
        if (enableEvents) {
            if (logger.isDebugEnabled()) {
                logger.debug("firePlotListChanged");
            }
            getPlotListChangedEventNotifier().queueEvent((source != null) ? source : this,
                    new OIFitsCollectionManagerEvent(OIFitsCollectionManagerEventType.PLOT_LIST_CHANGED, null), destination);
        }
    }

    /**
     * This fires a PLOT_LIST_CHANGED event to all registered listeners ASYNCHRONOUSLY !
     */
    private void firePlotListChanged() {
        firePlotListChanged(this, null);
    }

    /**
     * This fires a SUBSET_CHANGED event to given registered listener ASYNCHRONOUSLY !
     * @param source event source
     * @param subsetId subset definition identifier
     * @param destination destination listener (null means all)
     */
    public void fireSubsetDefinitionChanged(final Object source, final String subsetId, final OIFitsCollectionManagerEventListener destination) {
        if (enableEvents) {
            if (logger.isDebugEnabled()) {
                logger.debug("fireSubsetDefinitionChanged [{}] TO {}", subsetId, (destination != null) ? destination : "ALL");
            }
            getSubsetDefinitionChangedEventNotifier().queueEvent((source != null) ? source : this,
                    new OIFitsCollectionManagerEvent(OIFitsCollectionManagerEventType.SUBSET_CHANGED, subsetId), destination);
        }
    }

    /**
     * This fires a SUBSET_CHANGED event to all registered listeners ASYNCHRONOUSLY !
     * @param source event source
     * @param subsetId subset definition identifier
     */
    private void fireSubsetDefinitionChanged(final Object source, final String subsetId) {
        fireSubsetDefinitionChanged(source, subsetId, null);
    }

    /**
     * This fires a PLOT_DEFINITION_CHANGED event to given registered listener ASYNCHRONOUSLY !
     * @param source event source
     * @param plotDefId plot definition identifier
     * @param destination destination listener (null means all)
     */
    public void firePlotDefinitionChanged(final Object source, final String plotDefId, final OIFitsCollectionManagerEventListener destination) {
        if (enableEvents) {
            if (logger.isDebugEnabled()) {
                logger.debug("firePlotDefinitionChanged [{}] TO {}", plotDefId, (destination != null) ? destination : "ALL");
            }
            getPlotDefinitionChangedEventNotifier().queueEvent((source != null) ? source : this,
                    new OIFitsCollectionManagerEvent(OIFitsCollectionManagerEventType.PLOT_DEFINITION_CHANGED, plotDefId), destination);
        }
    }

    /**
     * This fires a PLOT_DEFINITION_CHANGED event to all registered listeners ASYNCHRONOUSLY !
     * @param source event source
     * @param plotDefId plot definition identifier
     */
    private void firePlotDefinitionChanged(final Object source, final String plotDefId) {
        firePlotDefinitionChanged(source, plotDefId, null);
    }

    /**
     * This fires a PLOT_CHANGED event to given registered listener ASYNCHRONOUSLY !
     * @param source event source
     * @param plotId plot identifier
     * @param destination destination listener (null means all)
     */
    public void firePlotChanged(final Object source, final String plotId, final OIFitsCollectionManagerEventListener destination) {
        if (enableEvents) {
            if (logger.isDebugEnabled()) {
                logger.debug("firePlotChanged [{}] TO {}", plotId, (destination != null) ? destination : "ALL");
            }
            getPlotChangedEventNotifier().queueEvent((source != null) ? source : this,
                    new OIFitsCollectionManagerEvent(OIFitsCollectionManagerEventType.PLOT_CHANGED, plotId), destination);
        }
    }

    /**
     * This fires a PLOT_CHANGED event to all registered listeners ASYNCHRONOUSLY !
     * @param source event source
     * @param plotId plot identifier
     */
    private void firePlotChanged(final Object source, final String plotId) {
        firePlotChanged(source, plotId, null);
    }

    /**
     * This fires a ACTIVE_PLOT_CHANGED event to given registered listener ASYNCHRONOUSLY !
     * @param source event source
     * @param plotId plot identifier
     * @param destination destination listener (null means all)
     */
    public void fireActivePlotChanged(final Object source, final String plotId, final OIFitsCollectionManagerEventListener destination) {
        if (enableEvents) {
            if (logger.isDebugEnabled()) {
                logger.debug("fireActivePlotChanged [{}] TO {}", plotId, (destination != null) ? destination : "ALL");
            }
            getActivePlotChangedEventNotifier().queueEvent((source != null) ? source : this,
                    new OIFitsCollectionManagerEvent(OIFitsCollectionManagerEventType.ACTIVE_PLOT_CHANGED, plotId), destination);
        }
    }

    /**
     * This fires a SELECTION_CHANGED event to given registered listener ASYNCHRONOUSLY !
     * @param source event source
     * @param destination destination listener (null means all)
     */
    public void fireSelectionChanged(final Object source, final OIFitsCollectionManagerEventListener destination) {
        if (enableEvents) {
            if (logger.isDebugEnabled()) {
                logger.debug("fireSelectionChanged TO {}", (destination != null) ? destination : "ALL");
            }
            getSelectionChangedEventNotifier().queueEvent((source != null) ? source : this,
                    new OIFitsCollectionManagerEvent(OIFitsCollectionManagerEventType.SELECTION_CHANGED, null), destination);
        }
    }

    /**
     * This fires a PLOT_VIEWPORT_CHANGED event to given registered listener ASYNCHRONOUSLY !
     * @param source event source
     * @param destination destination listener (null means all)
     */
    public void firePlotViewportChanged(final Object source, final OIFitsCollectionManagerEventListener destination) {
        if (enableEvents) {
            if (logger.isDebugEnabled()) {
                logger.debug("firePlotViewportChanged TO {}", (destination != null) ? destination : "ALL");
            }
            getPlotViewportChangedEventNotifier().queueEvent((source != null) ? source : this,
                    new OIFitsCollectionManagerEvent(OIFitsCollectionManagerEventType.PLOT_VIEWPORT_CHANGED, null), destination);
        }
    }

    /**
     * This fires a READY event to given registered listener ASYNCHRONOUSLY !
     * @param source event source
     * @param destination destination listener (null means all)
     */
    public void fireReady(final Object source, final OIFitsCollectionManagerEventListener destination) {
        if (enableEvents) {
            if (logger.isDebugEnabled()) {
                logger.debug("fireReadyChanged TO {}", (destination != null) ? destination : "ALL");
            }
            getReadyEventNotifier().queueEvent((source != null) ? source : this,
                    new OIFitsCollectionManagerEvent(OIFitsCollectionManagerEventType.READY, null), destination);
        }
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
        // accept all
        return null;
    }

    /**
     * Handle the given OIFits collection event
     * @param event OIFits collection event
     */
    @Override
    public void onProcess(final OIFitsCollectionManagerEvent event) {
        logger.debug("onProcess {}", event);

        switch (event.getType()) {
            case COLLECTION_CHANGED:
                // update collection analysis:
                oiFitsCollection.analyzeCollection();

                // check and update references in current OiDataCollection:
                // initialize current objects: subsetDefinition, plotDefinition, plot if NOT PRESENT:
                checkReferences();

                // CASCADE EVENTS:
                // SubsetDefinition:
                for (SubsetDefinition subsetDefinition : getSubsetDefinitionList()) {
                    // force fireSubsetChanged, update plot reference and firePlotChanged:
                    updateSubsetDefinitionRef(this, subsetDefinition);
                }

                // PlotDefinition:
                for (PlotDefinition plotDefinition : getPlotDefinitionList()) {
                    // force PlotDefinitionChanged, update plot reference and firePlotChanged:
                    updatePlotDefinitionRef(this, plotDefinition);
                }

                // Note: no explicit firePlotChanged event fired as done in updateSubsetDefinitionRef and updatePlotDefinitionRef
                break;
            case READY:
                System.out.println("userCollectionInitial");
                defineInitialUserCollection();
                break;
            default:
        }
        logger.debug("onProcess {} - done", event);
    }

    /*

        // check and update references in current OiDataCollection:
        // initialize current objects: subsetDefinition, plotDefinition, plot if NOT PRESENT:
        checkReferences();

        // finally: set the initial state of the user collection (after GUI updates => potentially modified) 
        this.defineInitialUserCollection();
    
    */
    
    /**
     * Check bad references
     */
    private void checkReferences() {

        // check and update references in OiDataCollection:
        this.userCollection.checkReferences();

        // TODO: see if the "GUI" manager decide to create objects itself ?
        // TODO: remove ASAP:
        // initialize current objects: subsetDefinition, plotDefinition, plot if NOT PRESENT:
        getCurrentPlotRef();

        logger.debug("checkReferences: userCollection = {}", userCollection);
    }

}
