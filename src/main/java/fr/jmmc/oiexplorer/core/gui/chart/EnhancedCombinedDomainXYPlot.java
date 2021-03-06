/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.oiexplorer.core.gui.chart;

import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Iterator;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.PlotState;
import org.jfree.chart.plot.XYPlot;

/**
 * Enhanced CombinedDomainXYPlot to fix zoom range axes to use correct sub-Plot data area
 * @author bourgesl
 */
public final class EnhancedCombinedDomainXYPlot extends CombinedDomainXYPlot {

    /** For serialization. */
    private static final long serialVersionUID = -7765545541261907383L;

    /**
     * Creates a new plot.
     *
     * @param rangeAxis  the shared axis.
     */
    public EnhancedCombinedDomainXYPlot(final ValueAxis rangeAxis) {
        super(rangeAxis);
    }

    /**
     * Multiplies the range on the range axis/axes by the specified factor.
     *
     * @param factor  the zoom factor.
     * @param info  the plot rendering info (<code>null</code> not permitted).
     * @param source  the source point (in Java2D coordinates).
     * @param useAnchor  use source point as zoom anchor?
     */
    @Override
    public void zoomRangeAxes(double factor, PlotRenderingInfo info,
                              Point2D source, boolean useAnchor) {
        // delegate 'state' and 'source' argument checks...
//        XYPlot subplot = findSubplot(state, source);
        final int subplotIndex = info.getSubplotIndex(source);
        XYPlot subplot = (subplotIndex >= 0) ? (XYPlot) getSubplots().get(subplotIndex) : null;

        if (subplot != null) {
            // LBO: use the correct subplot info:
            PlotRenderingInfo subplotInfo = info.getSubplotInfo(subplotIndex);
            subplot.zoomRangeAxes(factor, subplotInfo, source, useAnchor);
        } else {
            // if the source point doesn't fall within a subplot, we do the
            // zoom on all subplots...
            Iterator<?> iterator = getSubplots().iterator();
            while (iterator.hasNext()) {
                subplot = (XYPlot) iterator.next();
                subplot.zoomRangeAxes(factor, null, null, false); // LBO: ignore anchor
            }
        }
    }

    /**
     * Zooms in on the range axes.
     *
     * @param lowerPercent  the lower bound.
     * @param upperPercent  the upper bound.
     * @param info  the plot rendering info (<code>null</code> not permitted).
     * @param source  the source point (<code>null</code> not permitted).
     */
    @Override
    public void zoomRangeAxes(double lowerPercent, double upperPercent,
                              PlotRenderingInfo info, Point2D source) {
        // delegate 'info' and 'source' argument checks...
//        XYPlot subplot = findSubplot(info, source);
        final int subplotIndex = info.getSubplotIndex(source);
        XYPlot subplot = (subplotIndex >= 0) ? (XYPlot) getSubplots().get(subplotIndex) : null;

        if (subplot != null) {
            // LBO: use the correct subplot info:
            PlotRenderingInfo subplotInfo = info.getSubplotInfo(subplotIndex);
            subplot.zoomRangeAxes(lowerPercent, upperPercent, subplotInfo, source);
        } else {
            // if the source point doesn't fall within a subplot, we do the
            // zoom on all subplots...
            Iterator<?> iterator = getSubplots().iterator();
            while (iterator.hasNext()) {
                subplot = (XYPlot) iterator.next();
                subplot.zoomRangeAxes(lowerPercent, upperPercent, null, null); // LBO: ignore anchor
            }
        }
    }

    /**
     * Draws the plot within the specified area on a graphics device.
     *
     * @param g2  the graphics device.
     * @param area  the plot area (in Java2D space).
     * @param anchor  an anchor point in Java2D space ({@code null}
     *                permitted).
     * @param parentState  the state from the parent plot, if there is one
     *                     ({@code null} permitted).
     * @param info  collects chart drawing information ({@code null}
     *              permitted).
     */
    @Override
    public void draw(Graphics2D g2, Rectangle2D area, Point2D anchor,
                     PlotState parentState, PlotRenderingInfo info) {

        // avoid drawing shared (domain) axis if no sub plot
        if (getSubplots().isEmpty()) {
            // draw the plot background only
            drawBackground(g2, area);
        } else {
            super.draw(g2, area, anchor, parentState, info);
        }
    }
}
