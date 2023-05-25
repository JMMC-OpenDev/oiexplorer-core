/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.oiexplorer.core.gui;

import fr.jmmc.jmcs.util.ObjectUtils;
import fr.jmmc.oitools.meta.ColumnMeta;
import org.jfree.data.Range;

/**
 *
 * @author bourgesl
 */
public final class AxisInfo {

    /** colum meta data */
    ColumnMeta columnMeta = null;
    /** converter unit */
    String unit = null;
    /** is symetric axis */
    boolean useSymmetry = false;
    /** inverted */
    boolean inverted = false;
    /** is log axis */
    boolean useLog = false;
    /** data range */
    Range dataRange = null;
    /** data + error range  */
    Range dataErrRange = null;
    /** flag indicating that the dataset has data with error on this axis */
    boolean hasDataError = false;
    /** view bounds (with margin) */
    Range viewBounds = null;
    /** view range */
    Range viewRange = null;
    /** view range */
    Range plotRange = null;

    AxisInfo() {
    }

    AxisInfo(final AxisInfo src) {
        this.columnMeta = src.columnMeta;
        this.unit = src.unit;
        this.useSymmetry = src.useSymmetry;
        this.inverted = src.inverted;
        this.useLog = src.useLog;
        this.dataRange = src.dataRange;
        this.dataErrRange = src.dataErrRange;
        this.hasDataError = src.hasDataError;
        this.viewBounds = null;
        this.viewRange = null;
        this.plotRange = null;
    }

    void combineRanges(final AxisInfo src) {
        this.dataRange = Range.combine(dataRange, src.dataRange);
        this.dataErrRange = Range.combine(dataErrRange, src.dataErrRange);
    }

    public boolean isCompatible(final AxisInfo other) {
        return columnMeta.getName().equals(other.columnMeta.getName())
                && inverted == other.inverted
                && useLog == other.useLog
                && ObjectUtils.areEquals(unit, other.unit);
    }

    @Override
    public String toString() {
        return "AxisInfo{" + "columnMeta=" + columnMeta
                + ", unit=" + unit
                + ", useSymmetry=" + useSymmetry
                + ", inverted=" + inverted
                + ", useLog=" + useLog
                + ", dataRange=" + dataRange
                + ", dataErrRange=" + dataErrRange
                + ", hasDataError=" + hasDataError
                + ", viewBounds=" + viewBounds
                + ", viewRange=" + viewRange
                + ", plotRange=" + plotRange + '}';
    }

}
