/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.oiexplorer.core.gui.chart.dataset;

import fr.jmmc.jmcs.util.NumberUtils;
import fr.jmmc.oiexplorer.core.gui.chart.ColorPalette;
import fr.jmmc.oiexplorer.core.model.OIFitsCollectionManager;
import fr.jmmc.oiexplorer.core.model.OIFitsCollectionManagerEvent;
import fr.jmmc.oiexplorer.core.model.OIFitsCollectionManagerEventListener;
import fr.jmmc.oiexplorer.core.model.OIFitsCollectionManagerEventType;
import static fr.jmmc.oiexplorer.core.model.OIFitsCollectionManagerEventType.SUBSET_CHANGED;
import static fr.jmmc.oiexplorer.core.model.OIFitsCollectionManagerEventType.SUBSET_LIST_CHANGED;
import fr.jmmc.oiexplorer.core.model.oi.SubsetDefinition;
import static fr.jmmc.oitools.OIFitsConstants.STA_NAME_SEPARATOR;
import fr.jmmc.oitools.processing.SelectorResult;
import fr.jmmc.oitools.util.StationNamesComparator;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Global series attributes (names <-> Color, Symbol ...) to have consistent representation across plots based on ordered names
 * @author bourgesl
 */
public final class SharedSeriesAttributes implements OIFitsCollectionManagerEventListener {

    /** Class logger */
    private static final Logger logger = LoggerFactory.getLogger(SharedSeriesAttributes.class.getName());

    /** singleton instance */
    public final static SharedSeriesAttributes INSTANCE = new SharedSeriesAttributes("global", true);
    /** singleton instance */
    public final static SharedSeriesAttributes INSTANCE_OIXP = new SharedSeriesAttributes("oiexplorer", INSTANCE, false);

    /* members */
    private final String name;
    private final boolean doSortLabels;
    private final SharedSeriesAttributes parent;
    private final Set<String> labelNames = new HashSet<String>(64);
    private final Set<SeriesLabel> labels = new LinkedHashSet<SeriesLabel>(64); // preserve insertion order
    private final Map<String, Integer> colorMap = new HashMap<String, Integer>(64);
    private int colIdx = -1;

    private SharedSeriesAttributes(final String name, final boolean doSortLabels) {
        this(name, null, doSortLabels);
    }

    private SharedSeriesAttributes(final String name, final SharedSeriesAttributes parent, final boolean doSortLabels) {
        this.name = name;
        this.parent = parent;
        this.doSortLabels = doSortLabels;
        reset();
    }

    public void reset() {
        labelNames.clear();
        labels.clear();
        colorMap.clear();
        colIdx = (parent != null) ? parent.getColIdx() : 0;
    }

    public boolean hasLabel(final String label) {
        if (parent != null && parent.hasLabel(label)) {
            return true;
        }
        return this.labelNames.contains(label);
    }

    public void addLabel(final String label) {
        addLabel(label, null);
    }

    public void addLabel(final String label, final String alias) {
        if (!hasLabel(label)) {
            labelNames.add(label);
            if (alias != null) {
                labelNames.add(alias);
            }
            labels.add(new SeriesLabel(label, alias));
        }
    }

    public void define() {
        logger.debug("define[{}]: labels (raw):  {}", name, labels);

        final Collection<SeriesLabel> sortedLabels;

        // Sort labels by names:
        if (doSortLabels) {
            final ArrayList<SeriesLabel> labelsList = new ArrayList<SeriesLabel>(labels);
            Collections.sort(labelsList);
            sortedLabels = labelsList;
        } else {
            sortedLabels = labels;
        }

        logger.debug("define[{}]: labels (sort): {}", name, sortedLabels);

        // start at current index:
        int c = colIdx;

        for (SeriesLabel l : sortedLabels) {
            final Integer idx = NumberUtils.valueOf(c++);

            colorMap.put(l.label, idx);
            if (l.alias != null) {
                colorMap.put(l.alias, idx);
            }
        }
        logger.debug("define[{}]: colorMap:      {}", name, colorMap);
        // update current index:
        colIdx = c;
    }

    public void addColorAlias(final String label, final String alias) {
        if (!label.equals(alias)) {
            final Integer idx = colorMap.get(label);
            if (idx == null) {
                if (parent != null) {
                    parent.addColorAlias(label, alias);
                }
            } else {
                // define alias:
                colorMap.put(alias, idx);
            }
        }
    }

    public Integer getColorIndex(final String label) {
        final Integer idx = colorMap.get(label);
        if (idx == null) {
            if (parent != null) {
                return parent.getColorIndex(label);
            }
            logger.warn("Missing color for label: {}", label);
            // Use first color in the palette:
            return 0;
        }
        return idx;
    }

    public Color getColor(final String label) {
        return ColorPalette.getColorPalette().getColor(getColorIndex(label));
    }

    public Color getColorAlpha(final String label) {
        return ColorPalette.getColorPaletteAlpha().getColor(getColorIndex(label));
    }

    private int getColIdx() {
        return colIdx;
    }

    @Override
    public String toString() {
        return "SharedSeriesAttributes{" + "name=" + name + ", parent=" + parent
                + ", labels=" + labels + ", colorMap=" + colorMap + '}';
    }

    protected final static class SeriesLabel implements Comparable<SeriesLabel> {

        /* members */
        final String label;
        final String alias;

        protected SeriesLabel(final String label, final String alias) {
            this.label = label;
            this.alias = alias;
        }

        @Override
        public int compareTo(final SeriesLabel other) {
            return StationNamesComparator.INSTANCE.compare(label, other.label);
        }

        @Override
        public int hashCode() {
            return this.label.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final SeriesLabel other = (SeriesLabel) obj;
            return !((this.label == null) ? (other.label != null) : !this.label.equals(other.label));
        }

        @Override
        public String toString() {
            return "SeriesLabel{label=" + label + ", alias=" + alias + '}';
        }

    }

    /** Removes listeners references */
    @Override
    public void dispose() {
        OIFitsCollectionManager.getInstance().unbind(this);
    }

    public void register() {
        // listen for SUBSET_CHANGED events to collect used StaNames / StaConfs and define properly color mappings once:
        OIFitsCollectionManager.getInstance().bindSubsetDefinitionChanged(this);
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
            case SUBSET_CHANGED:
                defineColorMapping(OIFitsCollectionManager.getInstance().getSubsetDefinitionList());
                break;
            default:
        }
        logger.debug("onProcess {} - done", event);
    }

    private void defineColorMapping(final List<SubsetDefinition> subsetDefinitionList) {
        logger.debug("defineColorMapping: {}", subsetDefinitionList);

        // reset labels and colors:
        reset();

        final ArrayList<String> allSingles = new ArrayList<String>(8);
        final ArrayList<String> allOtherStaNames = new ArrayList<String>(32);
        final ArrayList<String> allStaConfs = new ArrayList<String>(8);

        // Collect all distinct StaNames / StaConfs and split 1T:
        for (SubsetDefinition subsetDefinition : subsetDefinitionList) {
            // reuse selector result from current subset definition:
            final SelectorResult result = subsetDefinition.getSelectorResult();
            if (result != null) {
                final ArrayList<String> staNames;

                if (result.hasTargetResult()) {
                    staNames = result.getTargetResult().getDistinctStaNames();
                    allStaConfs.addAll(result.getTargetResult().getDistinctStaConfs());
                } else {
                    staNames = result.getDistinctStaNames();
                    allStaConfs.addAll(result.getDistinctStaConfs());
                }
                classifyStaNames(staNames, allSingles, allOtherStaNames);
            }
        }

        if (!allStaConfs.isEmpty()) {
            if (allSingles.size() > 1) {
                Collections.sort(allSingles);
            }
            if (allOtherStaNames.size() > 1) {
                Collections.sort(allOtherStaNames, StationNamesComparator.INSTANCE);
            }
            if (allStaConfs.size() > 1) {
                Collections.sort(allStaConfs, StationNamesComparator.INSTANCE);
            }
            logger.debug("defineColorMapping: allSingles:       {}", allSingles);
            logger.debug("defineColorMapping: allOtherStaNames: {}", allOtherStaNames);
            logger.debug("defineColorMapping: allStaConfs:      {}", allStaConfs);

            // Add labels in specific order to set their colors:
            // add 2T or 3T staNames:
            for (String label : allOtherStaNames) {
                addLabel(label);
            }
            // add StaConfs:
            for (String label : allStaConfs) {
                addLabel(label);
            }
            // add 1T staNames:
            for (String label : allSingles) {
                addLabel(label);
            }

            // Assign ONCE colors to labels automatically:
            define();
        }
    }

    private static void classifyStaNames(final ArrayList<String> staNames,
                                         final ArrayList<String> singleLabel,
                                         final ArrayList<String> otherLabels) {
        for (String staName : staNames) {
            if (staName.indexOf(STA_NAME_SEPARATOR) == -1) {
                singleLabel.add(staName);
            } else {
                otherLabels.add(staName);
            }
        }
    }
}
