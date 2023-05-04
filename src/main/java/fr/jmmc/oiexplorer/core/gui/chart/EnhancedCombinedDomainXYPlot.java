/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.oiexplorer.core.gui.chart;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Iterator;
import org.jfree.chart.axis.AxisSpace;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.PlotState;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.ui.RectangleInsets;

/**
 * Enhanced CombinedDomainXYPlot to fix zoom range axes to use correct sub-Plot data area
 * @author bourgesl
 */
public final class EnhancedCombinedDomainXYPlot extends CombinedDomainXYPlot {

    /** For serialization. */
    private static final long serialVersionUID = -7765545541261907383L;

    /** flag to debug paint operations */
    public static final boolean DEBUG_PAINT = false;
    /** flag to display as squared plot */
    private boolean squareMode = false;

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

        final int subplotIndex = info.getSubplotIndex(source);
        XYPlot subplot = (subplotIndex >= 0) ? getSubplots().get(subplotIndex) : null;

        if (!isSquareMode() && (subplot != null)) {
            // LBO: use the correct subplot info:
            final PlotRenderingInfo subplotInfo = info.getSubplotInfo(subplotIndex);
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

        final int subplotIndex = info.getSubplotIndex(source);
        XYPlot subplot = (subplotIndex >= 0) ? getSubplots().get(subplotIndex) : null;

        if (!isSquareMode() && (subplot != null)) {
            // LBO: use the correct subplot info:
            final PlotRenderingInfo subplotInfo = info.getSubplotInfo(subplotIndex);
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
     * @param g2d  the graphics device.
     * @param area  the plot area (in Java2D space).
     * @param anchor  an anchor point in Java2D space ({@code null}
     *                permitted).
     * @param parentState  the state from the parent plot, if there is one
     *                     ({@code null} permitted).
     * @param info  collects chart drawing information ({@code null}
     *              permitted).
     */
    @Override
    public void draw(Graphics2D g2d, Rectangle2D area, Point2D anchor,
                     PlotState parentState, PlotRenderingInfo info) {

        // avoid drawing shared (domain) axis if no sub plot
        if (getSubplots().isEmpty()) {
            // draw the plot background only
            drawBackground(g2d, area);
        } else {
            Rectangle2D plotArea = area;

            if (isSquareMode()) {
                // See SquareXYPlot class:
                double hSpace = 0d;
                double vSpace = 0d;

                // get plot insets :
                final RectangleInsets insets = getInsets();

                hSpace += insets.getLeft() + insets.getRight();
                vSpace += insets.getTop() + insets.getBottom();

                // compute Axis Space :
                final AxisSpace space = calculateAxisSpace(g2d, area);

                hSpace += space.getLeft() + space.getRight();
                vSpace += space.getTop() + space.getBottom();

                // compute the square data area size :
                final double maxSize = Math.max(0.0, Math.min(area.getWidth() - hSpace, area.getHeight() - vSpace));

                // adjusted dimensions to get a square data area :
                final double adjustedWidth = maxSize + hSpace;
                final double adjustedHeight = maxSize + vSpace;

                // margins to center the plot into the rectangle area :
                final double marginWidth = (area.getWidth() - adjustedWidth) / 2d;
                final double marginHeight = (area.getHeight() - adjustedHeight) / 2d;

                plotArea = new Rectangle2D.Double();

                // note :
                // - rounding is required to have the background image fitted (int coordinates) in the plot area (double rectangle) :
                // - there can be some rounding issue that adjust lightly the square shape :
                plotArea.setRect(Math.round(area.getX() + marginWidth), Math.round(area.getY() + marginHeight),
                        Math.round(adjustedWidth), Math.round(adjustedHeight));

                if (DEBUG_PAINT) {
                    g2d.setStroke(new BasicStroke(4));
                    g2d.setPaint(Color.RED);
                    g2d.draw(area);

                    g2d.setStroke(new BasicStroke(4));
                    g2d.setPaint(Color.GREEN);
                    g2d.draw(plotArea);
                }
            }
            // draw the plot:
            super.draw(g2d, plotArea, anchor, parentState, info);
        }
    }

    public boolean isSquareMode() {
        return squareMode;
    }

    public void setSquareMode(boolean squareMode) {
        this.squareMode = squareMode;
    }

}
