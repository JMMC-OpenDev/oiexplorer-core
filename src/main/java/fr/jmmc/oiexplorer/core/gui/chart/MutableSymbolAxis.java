/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.oiexplorer.core.gui.chart;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import org.jfree.chart.axis.AxisState;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTick;
import org.jfree.chart.axis.Tick;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.axis.ValueTick;

import org.jfree.chart.event.AxisChangeEvent;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.ValueAxisPlot;
import org.jfree.chart.text.TextUtils;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.ui.TextAnchor;
import org.jfree.chart.util.Args;
import org.jfree.chart.util.PaintUtils;
import org.jfree.chart.util.SerialUtils;
import org.jfree.data.Range;

/* ===========================================================
 * JFreeChart : a free chart library for the Java(tm) platform
 * ===========================================================
 *
 * (C) Copyright 2000-2008, by Object Refinery Limited and Contributors.
 *
 * Project Info:  http://www.jfree.org/jfreechart/index.html
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301,
 * USA.
 *
 * [Java is a trademark or registered trademark of Sun Microsystems, Inc.
 * in the United States and other countries.]
 *
 * ---------------
 * SymbolAxis.java
 * ---------------
 * (C) Copyright 2002-2008, by Anthony Boulestreau and Contributors.
 *
 * Original Author:  Anthony Boulestreau;
 * Contributor(s):   David Gilbert (for Object Refinery Limited);
 *
 *
 * Changes
 * -------
 * 29-Mar-2002 : First version (AB);
 * 19-Apr-2002 : Updated formatting and import statements (DG);
 * 21-Jun-2002 : Make change to use the class TickUnit - remove valueToString()
 *               method and add SymbolicTickUnit (AB);
 * 25-Jun-2002 : Removed redundant code (DG);
 * 25-Jul-2002 : Changed order of parameters in ValueAxis constructor (DG);
 * 05-Sep-2002 : Updated constructor to reflect changes in the Axis class (DG);
 * 08-Nov-2002 : Moved to new package com.jrefinery.chart.axis (DG);
 * 14-Feb-2003 : Added back missing constructor code (DG);
 * 26-Mar-2003 : Implemented Serializable (DG);
 * 14-May-2003 : Renamed HorizontalSymbolicAxis --> SymbolicAxis and merged in
 *               VerticalSymbolicAxis (DG);
 * 12-Aug-2003 : Fixed bug where refreshTicks() method has different signature
 *               to super class (DG);
 * 29-Oct-2003 : Added workaround for font alignment in PDF output (DG);
 * 02-Nov-2003 : Added code to avoid overlapping labels (MR);
 * 07-Nov-2003 : Modified to use new tick classes (DG);
 * 18-Nov-2003 : Fixed bug where symbols are not being displayed on the
 *               axis (DG);
 * 24-Nov-2003 : Added fix for gridlines on zooming (bug id 834643) (DG);
 * 21-Jan-2004 : Update for renamed method in ValueAxis (DG);
 * 11-Mar-2004 : Modified the way the background grid color is being drawn, see
 *               this thread:
 *               http://www.jfree.org/phpBB2/viewtopic.php?p=22973 (DG);
 * 16-Mar-2004 : Added plotState to draw() method (DG);
 * 07-Apr-2004 : Modified string bounds calculation (DG);
 * 28-Mar-2005 : Renamed autoRangeIncludesZero() --> getAutoRangeIncludesZero()
 *               and autoRangeStickyZero() --> getAutoRangeStickyZero() (DG);
 * 05-Jul-2005 : Fixed signature on refreshTicks() method - see bug report
 *               1232264 (DG);
 * 06-Jul-2005 : Renamed SymbolicAxis --> SymbolAxis, added equals() method,
 *               renamed getSymbolicValue() --> getSymbols(), renamed
 *               symbolicGridPaint --> gridBandPaint, fixed serialization of
 *               gridBandPaint, renamed symbolicGridLinesVisible -->
 *               gridBandsVisible, eliminated symbolicGridLineList (DG);
 * ------------- JFREECHART 1.0.x ---------------------------------------------
 * 02-Feb-2007 : Removed author tags all over JFreeChart sources (DG);
 * 28-Feb-2007 : Fixed bug 1669302 (tick label overlap) (DG);
 * 25-Jul-2007 : Added new field for alternate grid band paint (DG);
 * 15-Aug-2008 : Use alternate grid band paint when drawing (DG);
 * 02-Jul-2013 : Use ParamChecks (DG);
 * 19-Mar-2014 : Fix gridbands (bug #1056) (DG);
 * 08-Sep-2014 : Update Javadocs for grid bands (DG):
 *
 */
/**
 * A standard linear value axis that replaces integer values with symbols.
 */
public class MutableSymbolAxis extends NumberAxis implements Serializable {

    /** For serialization. */
    private static final long serialVersionUID = 7216330468770619716L;
    /** The default grid band paint. */
    public static final Paint DEFAULT_GRID_BAND_PAINT = new Color(232, 234, 232, 128);
    /**
     * The default paint for alternate grid bands.
     *
     * @since 1.0.7
     */
    public static final Paint DEFAULT_GRID_BAND_ALTERNATE_PAINT = new Color(0, 0, 0, 0);  // transparent
    /** The list of symbols to display instead of the numeric values. */
    private List<String> symbols;
    /** Flag that indicates whether or not grid bands are visible. */
    private boolean gridBandsVisible;
    /** The paint used to color the grid bands (if the bands are visible). */
    private transient Paint gridBandPaint;
    /**
     * The paint used to fill the alternate grid bands.
     *
     * @since 1.0.7
     */
    private transient Paint gridBandAlternatePaint;

    /**
     * Constructs a symbol axis, using default attribute values where
     * necessary.
     *
     * @param label  the axis label ({@code null} permitted).
     * @param sv  the list of symbols to display instead of the numeric
     *            values.
     */
    public MutableSymbolAxis(String label, String[] sv) {
        super(label);
        this.symbols = Arrays.asList(sv);
        this.gridBandsVisible = true;
        this.gridBandPaint = DEFAULT_GRID_BAND_PAINT;
        this.gridBandAlternatePaint = DEFAULT_GRID_BAND_ALTERNATE_PAINT;
        setAutoTickUnitSelection(false, false);
        setAutoRangeStickyZero(false);
    }

    /**
     * Update the symbols (mutable)
     * @param sv  the list of symbols to display instead of the numeric
     *            values.
     */
    public void setSymbols(String[] sv) {
        this.symbols = Arrays.asList(sv);
        notifyListeners(new AxisChangeEvent(this));
    }

    /**
     * Returns an array of the symbols for the axis.
     *
     * @return The symbols.
     */
    public String[] getSymbols() {
        return this.symbols.toArray(new String[this.symbols.size()]);
    }

    /**
     * Returns the flag that controls whether or not grid bands are drawn for 
     * the axis.  The default value is {@code true}. 
     *
     * @return A boolean.
     *
     * @see #setGridBandsVisible(boolean)
     */
    public boolean isGridBandsVisible() {
        return this.gridBandsVisible;
    }

    /**
     * Sets the flag that controls whether or not grid bands are drawn for this
     * axis and notifies registered listeners that the axis has been modified.
     * Each band is the area between two adjacent gridlines 
     * running perpendicular to the axis.  When the bands are drawn they are 
     * filled with the colors {@link #getGridBandPaint()} and 
     * {@link #getGridBandAlternatePaint()} in an alternating sequence.
     *
     * @param flag  the new setting.
     *
     * @see #isGridBandsVisible()
     */
    public void setGridBandsVisible(boolean flag) {
        this.gridBandsVisible = flag;
        fireChangeEvent();
    }

    /**
     * Returns the paint used to color grid bands (two colors are used
     * alternately, the other is returned by 
     * {@link #getGridBandAlternatePaint()}).  The default value is
     * {@link #DEFAULT_GRID_BAND_PAINT}.
     *
     * @return The paint (never {@code null}).
     *
     * @see #setGridBandPaint(Paint)
     * @see #isGridBandsVisible()
     */
    public Paint getGridBandPaint() {
        return this.gridBandPaint;
    }

    /**
     * Sets the grid band paint and notifies registered listeners that the
     * axis has been changed.  See the {@link #setGridBandsVisible(boolean)}
     * method for more information about grid bands.
     *
     * @param paint  the paint ({@code null} not permitted).
     *
     * @see #getGridBandPaint()
     */
    public void setGridBandPaint(Paint paint) {
        Args.nullNotPermitted(paint, "paint");
        this.gridBandPaint = paint;
        fireChangeEvent();
    }

    /**
     * Returns the second paint used to color grid bands (two colors are used
     * alternately, the other is returned by {@link #getGridBandPaint()}).  
     * The default value is {@link #DEFAULT_GRID_BAND_ALTERNATE_PAINT} 
     * (transparent).
     *
     * @return The paint (never {@code null}).
     *
     * @see #setGridBandAlternatePaint(Paint)
     *
     * @since 1.0.7
     */
    public Paint getGridBandAlternatePaint() {
        return this.gridBandAlternatePaint;
    }

    /**
     * Sets the grid band paint and notifies registered listeners that the
     * axis has been changed.  See the {@link #setGridBandsVisible(boolean)}
     * method for more information about grid bands.
     *
     * @param paint  the paint ({@code null} not permitted).
     *
     * @see #getGridBandAlternatePaint()
     * @see #setGridBandPaint(Paint)
     *
     * @since 1.0.7
     */
    public void setGridBandAlternatePaint(Paint paint) {
        Args.nullNotPermitted(paint, "paint");
        this.gridBandAlternatePaint = paint;
        fireChangeEvent();
    }

    /**
     * This operation is not supported by this axis.
     *
     * @param g2  the graphics device.
     * @param dataArea  the area in which the plot and axes should be drawn.
     * @param edge  the edge along which the axis is drawn.
     */
    @Override
    protected void selectAutoTickUnit(Graphics2D g2, Rectangle2D dataArea,
                                      RectangleEdge edge) {
        throw new UnsupportedOperationException();
    }

    /**
     * Draws the axis on a Java 2D graphics device (such as the screen or a
     * printer).
     *
     * @param g2  the graphics device ({@code null} not permitted).
     * @param cursor  the cursor location.
     * @param plotArea  the area within which the plot and axes should be drawn
     *                  ({@code null} not permitted).
     * @param dataArea  the area within which the data should be drawn
     *                  ({@code null} not permitted).
     * @param edge  the axis location ({@code null} not permitted).
     * @param plotState  collects information about the plot
     *                   ({@code null} permitted).
     *
     * @return The axis state (never {@code null}).
     */
    @Override
    public AxisState draw(Graphics2D g2, double cursor, Rectangle2D plotArea,
                          Rectangle2D dataArea, RectangleEdge edge,
                          PlotRenderingInfo plotState) {

        AxisState info = new AxisState(cursor);
        if (isVisible()) {
            info = super.draw(g2, cursor, plotArea, dataArea, edge, plotState);
        }
        if (this.gridBandsVisible) {
            drawGridBands(g2, plotArea, dataArea, edge, info.getTicks());
        }
        return info;

    }

    /**
     * Draws the grid bands (alternate bands are colored using
     * {@link #getGridBandPaint()} and {@link #getGridBandAlternatePaint()}.
     *
     * @param g2  the graphics target ({@code null} not permitted).
     * @param plotArea  the area within which the plot is drawn 
     *     ({@code null} not permitted).
     * @param dataArea  the data area to which the axes are aligned 
     *     ({@code null} not permitted).
     * @param edge  the edge to which the axis is aligned ({@code null} not
     *     permitted).
     * @param ticks  the ticks ({@code null} not permitted).
     */
    protected void drawGridBands(Graphics2D g2, Rectangle2D plotArea,
                                 Rectangle2D dataArea, RectangleEdge edge, List ticks) {
        Shape savedClip = g2.getClip();
        g2.clip(dataArea);
        if (RectangleEdge.isTopOrBottom(edge)) {
            drawGridBandsHorizontal(g2, plotArea, dataArea, true, ticks);
        } else if (RectangleEdge.isLeftOrRight(edge)) {
            drawGridBandsVertical(g2, plotArea, dataArea, true, ticks);
        }
        g2.setClip(savedClip);
    }

    /**
     * Draws the grid bands for the axis when it is at the top or bottom of
     * the plot.
     *
     * @param g2  the graphics target ({@code null} not permitted).
     * @param plotArea  the area within which the plot is drawn (not used here).
     * @param dataArea  the area for the data (to which the axes are aligned,
     *         {@code null} not permitted).
     * @param firstGridBandIsDark  True: the first grid band takes the
     *                             color of {@code gridBandPaint}.
     *                             False: the second grid band takes the
     *                             color of {@code gridBandPaint}.
     * @param ticks  a list of ticks ({@code null} not permitted).
     */
    protected void drawGridBandsHorizontal(Graphics2D g2,
                                           Rectangle2D plotArea, Rectangle2D dataArea,
                                           boolean firstGridBandIsDark, List ticks) {

        boolean currentGridBandIsDark = firstGridBandIsDark;
        double yy = dataArea.getY();
        double xx1, xx2;

        //gets the outline stroke width of the plot
        double outlineStrokeWidth = 1.0;
        Stroke outlineStroke = getPlot().getOutlineStroke();
        if (outlineStroke != null && outlineStroke instanceof BasicStroke) {
            outlineStrokeWidth = ((BasicStroke) outlineStroke).getLineWidth();
        }

        Iterator iterator = ticks.iterator();
        ValueTick tick;
        Rectangle2D band;
        while (iterator.hasNext()) {
            tick = (ValueTick) iterator.next();
            xx1 = valueToJava2D(tick.getValue() - 0.5d, dataArea,
                    RectangleEdge.BOTTOM);
            xx2 = valueToJava2D(tick.getValue() + 0.5d, dataArea,
                    RectangleEdge.BOTTOM);
            if (currentGridBandIsDark) {
                g2.setPaint(this.gridBandPaint);
            } else {
                g2.setPaint(this.gridBandAlternatePaint);
            }
            band = new Rectangle2D.Double(Math.min(xx1, xx2),
                    yy + outlineStrokeWidth, Math.abs(xx2 - xx1),
                    dataArea.getMaxY() - yy - outlineStrokeWidth);
            g2.fill(band);
            currentGridBandIsDark = !currentGridBandIsDark;
        }
    }

    /**
     * Draws the grid bands for an axis that is aligned to the left or
     * right of the data area (that is, a vertical axis).
     *
     * @param g2  the graphics target ({@code null} not permitted).
     * @param plotArea  the area within which the plot is drawn (not used here).
     * @param dataArea  the area for the data (to which the axes are aligned,
     *         {@code null} not permitted).
     * @param firstGridBandIsDark  True: the first grid band takes the
     *                             color of {@code gridBandPaint}.
     *                             False: the second grid band takes the
     *                             color of {@code gridBandPaint}.
     * @param ticks  a list of ticks ({@code null} not permitted).
     */
    protected void drawGridBandsVertical(Graphics2D g2, Rectangle2D plotArea,
                                         Rectangle2D dataArea, boolean firstGridBandIsDark,
                                         List ticks) {

        boolean currentGridBandIsDark = firstGridBandIsDark;
        double xx = dataArea.getX();
        double yy1, yy2;

        //gets the outline stroke width of the plot
        double outlineStrokeWidth = 1.0;
        Stroke outlineStroke = getPlot().getOutlineStroke();
        if (outlineStroke != null && outlineStroke instanceof BasicStroke) {
            outlineStrokeWidth = ((BasicStroke) outlineStroke).getLineWidth();
        }

        Iterator iterator = ticks.iterator();
        ValueTick tick;
        Rectangle2D band;
        while (iterator.hasNext()) {
            tick = (ValueTick) iterator.next();
            yy1 = valueToJava2D(tick.getValue() + 0.5d, dataArea,
                    RectangleEdge.LEFT);
            yy2 = valueToJava2D(tick.getValue() - 0.5d, dataArea,
                    RectangleEdge.LEFT);
            if (currentGridBandIsDark) {
                g2.setPaint(this.gridBandPaint);
            } else {
                g2.setPaint(this.gridBandAlternatePaint);
            }
            band = new Rectangle2D.Double(xx + outlineStrokeWidth,
                    Math.min(yy1, yy2), dataArea.getMaxX() - xx
                    - outlineStrokeWidth, Math.abs(yy2 - yy1));
            g2.fill(band);
            currentGridBandIsDark = !currentGridBandIsDark;
        }
    }

    /**
     * Rescales the axis to ensure that all data is visible.
     */
    @Override
    protected void autoAdjustRange() {
        Plot plot = getPlot();
        if (plot == null) {
            return;  // no plot, no data
        }

        if (plot instanceof ValueAxisPlot) {

            // ensure that all the symbols are displayed
            double upper = this.symbols.size() - 1;
            double lower = 0;
            double range = upper - lower;

            // ensure the autorange is at least <minRange> in size...
            double minRange = getAutoRangeMinimumSize();
            if (range < minRange) {
                upper = (upper + lower + minRange) / 2;
                lower = (upper + lower - minRange) / 2;
            }

            // this ensure that the grid bands will be displayed correctly.
            double upperMargin = 0.5;
            double lowerMargin = 0.5;

            if (getAutoRangeIncludesZero()) {
                if (getAutoRangeStickyZero()) {
                    if (upper <= 0.0) {
                        upper = 0.0;
                    } else {
                        upper = upper + upperMargin;
                    }
                    if (lower >= 0.0) {
                        lower = 0.0;
                    } else {
                        lower = lower - lowerMargin;
                    }
                } else {
                    upper = Math.max(0.0, upper + upperMargin);
                    lower = Math.min(0.0, lower - lowerMargin);
                }
            } else {
                if (getAutoRangeStickyZero()) {
                    if (upper <= 0.0) {
                        upper = Math.min(0.0, upper + upperMargin);
                    } else {
                        upper = upper + upperMargin * range;
                    }
                    if (lower >= 0.0) {
                        lower = Math.max(0.0, lower - lowerMargin);
                    } else {
                        lower = lower - lowerMargin;
                    }
                } else {
                    upper = upper + upperMargin;
                    lower = lower - lowerMargin;
                }
            }
            setRange(new Range(lower, upper), false, false);
        }
    }

    /**
     * Calculates the positions of the tick labels for the axis, storing the
     * results in the tick label list (ready for drawing).
     *
     * @param g2  the graphics device.
     * @param state  the axis state.
     * @param dataArea  the area in which the data should be drawn.
     * @param edge  the location of the axis.
     *
     * @return A list of ticks.
     */
    @Override
    public List refreshTicks(Graphics2D g2,
                             AxisState state,
                             Rectangle2D dataArea,
                             RectangleEdge edge) {
        List ticks = null;
        if (RectangleEdge.isTopOrBottom(edge)) {
            ticks = refreshTicksHorizontal(g2, dataArea, edge);
        } else if (RectangleEdge.isLeftOrRight(edge)) {
            ticks = refreshTicksVertical(g2, dataArea, edge);
        }
        return ticks;
    }

    /**
     * Calculates the positions of the tick labels for the axis, storing the
     * results in the tick label list (ready for drawing).
     *
     * @param g2  the graphics device.
     * @param dataArea  the area in which the data should be drawn.
     * @param edge  the location of the axis.
     *
     * @return The ticks.
     */
    @Override
    protected List refreshTicksHorizontal(Graphics2D g2,
                                          Rectangle2D dataArea,
                                          RectangleEdge edge) {

        List ticks = new java.util.ArrayList();

        Font tickLabelFont = getTickLabelFont();
        g2.setFont(tickLabelFont);

        double size = getTickUnit().getSize();
        int count = calculateVisibleTickCount();
        double lowestTickValue = calculateLowestVisibleTickValue();

        double previousDrawnTickLabelPos = 0.0;
        double previousDrawnTickLabelLength = 0.0;

        if (count <= ValueAxis.MAXIMUM_TICK_COUNT) {
            for (int i = 0; i < count; i++) {
                double currentTickValue = lowestTickValue + (i * size);
                double xx = valueToJava2D(currentTickValue, dataArea, edge);
                String tickLabel;
                NumberFormat formatter = getNumberFormatOverride();
                if (formatter != null) {
                    tickLabel = formatter.format(currentTickValue);
                } else {
                    tickLabel = valueToString(currentTickValue);
                }

                // avoid to draw overlapping tick labels
                Rectangle2D bounds = TextUtils.getTextBounds(tickLabel, g2,
                        g2.getFontMetrics());
                double tickLabelLength = isVerticalTickLabels()
                        ? bounds.getHeight() : bounds.getWidth();
                boolean tickLabelsOverlapping = false;
                if (i > 0) {
                    double avgTickLabelLength = (previousDrawnTickLabelLength
                            + tickLabelLength) / 2.0;
                    if (Math.abs(xx - previousDrawnTickLabelPos)
                            < avgTickLabelLength) {
                        tickLabelsOverlapping = true;
                    }
                }
                if (tickLabelsOverlapping) {
                    tickLabel = ""; // don't draw this tick label
                } else {
                    // remember these values for next comparison
                    previousDrawnTickLabelPos = xx;
                    previousDrawnTickLabelLength = tickLabelLength;
                }

                TextAnchor anchor = null;
                TextAnchor rotationAnchor = null;
                double angle = 0.0;
                if (isVerticalTickLabels()) {
                    anchor = TextAnchor.CENTER_RIGHT;
                    rotationAnchor = TextAnchor.CENTER_RIGHT;
                    if (edge == RectangleEdge.TOP) {
                        angle = Math.PI / 2.0;
                    } else {
                        angle = -Math.PI / 2.0;
                    }
                } else {
                    if (edge == RectangleEdge.TOP) {
                        anchor = TextAnchor.BOTTOM_CENTER;
                        rotationAnchor = TextAnchor.BOTTOM_CENTER;
                    } else {
                        anchor = TextAnchor.TOP_CENTER;
                        rotationAnchor = TextAnchor.TOP_CENTER;
                    }
                }
                Tick tick = new NumberTick(Double.valueOf(currentTickValue),
                        tickLabel, anchor, rotationAnchor, angle);
                ticks.add(tick);
            }
        }
        return ticks;

    }

    /**
     * Calculates the positions of the tick labels for the axis, storing the
     * results in the tick label list (ready for drawing).
     *
     * @param g2  the graphics device.
     * @param dataArea  the area in which the plot should be drawn.
     * @param edge  the location of the axis.
     *
     * @return The ticks.
     */
    @Override
    protected List refreshTicksVertical(Graphics2D g2,
                                        Rectangle2D dataArea,
                                        RectangleEdge edge) {

        List ticks = new java.util.ArrayList();

        Font tickLabelFont = getTickLabelFont();
        g2.setFont(tickLabelFont);

        double size = getTickUnit().getSize();
        int count = calculateVisibleTickCount();
        double lowestTickValue = calculateLowestVisibleTickValue();

        double previousDrawnTickLabelPos = 0.0;
        double previousDrawnTickLabelLength = 0.0;

        if (count <= ValueAxis.MAXIMUM_TICK_COUNT) {
            for (int i = 0; i < count; i++) {
                double currentTickValue = lowestTickValue + (i * size);
                double yy = valueToJava2D(currentTickValue, dataArea, edge);
                String tickLabel;
                NumberFormat formatter = getNumberFormatOverride();
                if (formatter != null) {
                    tickLabel = formatter.format(currentTickValue);
                } else {
                    tickLabel = valueToString(currentTickValue);
                }

                // avoid to draw overlapping tick labels
                Rectangle2D bounds = TextUtils.getTextBounds(tickLabel, g2,
                        g2.getFontMetrics());
                double tickLabelLength = isVerticalTickLabels()
                        ? bounds.getWidth() : bounds.getHeight();
                boolean tickLabelsOverlapping = false;
                if (i > 0) {
                    double avgTickLabelLength = (previousDrawnTickLabelLength
                            + tickLabelLength) / 2.0;
                    if (Math.abs(yy - previousDrawnTickLabelPos)
                            < avgTickLabelLength) {
                        tickLabelsOverlapping = true;
                    }
                }
                if (tickLabelsOverlapping) {
                    tickLabel = ""; // don't draw this tick label
                } else {
                    // remember these values for next comparison
                    previousDrawnTickLabelPos = yy;
                    previousDrawnTickLabelLength = tickLabelLength;
                }

                TextAnchor anchor = null;
                TextAnchor rotationAnchor = null;
                double angle = 0.0;
                if (isVerticalTickLabels()) {
                    anchor = TextAnchor.BOTTOM_CENTER;
                    rotationAnchor = TextAnchor.BOTTOM_CENTER;
                    if (edge == RectangleEdge.LEFT) {
                        angle = -Math.PI / 2.0;
                    } else {
                        angle = Math.PI / 2.0;
                    }
                } else {
                    if (edge == RectangleEdge.LEFT) {
                        anchor = TextAnchor.CENTER_RIGHT;
                        rotationAnchor = TextAnchor.CENTER_RIGHT;
                    } else {
                        anchor = TextAnchor.CENTER_LEFT;
                        rotationAnchor = TextAnchor.CENTER_LEFT;
                    }
                }
                Tick tick = new NumberTick(Double.valueOf(currentTickValue),
                        tickLabel, anchor, rotationAnchor, angle);
                ticks.add(tick);
            }
        }
        return ticks;

    }

    /**
     * Converts a value to a string, using the list of symbols.
     *
     * @param value  value to convert.
     *
     * @return The symbol.
     */
    public String valueToString(double value) {
        String strToReturn;
        try {
            strToReturn = this.symbols.get((int) value);
        } catch (IndexOutOfBoundsException ex) {
            strToReturn = "";
        }
        return strToReturn;
    }

    /**
     * Tests this axis for equality with an arbitrary object.
     *
     * @param obj  the object (<code>null</code> permitted).
     *
     * @return A boolean.
     */
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof MutableSymbolAxis)) {
            return false;
        }
        MutableSymbolAxis that = (MutableSymbolAxis) obj;
        if (!this.symbols.equals(that.symbols)) {
            return false;
        }
        if (this.gridBandsVisible != that.gridBandsVisible) {
            return false;
        }
        if (!PaintUtils.equal(this.gridBandPaint, that.gridBandPaint)) {
            return false;
        }
        if (!PaintUtils.equal(this.gridBandAlternatePaint,
                that.gridBandAlternatePaint)) {
            return false;
        }
        return super.equals(obj);
    }

    /**
     * Provides serialization support.
     *
     * @param stream  the output stream.
     *
     * @throws IOException  if there is an I/O error.
     */
    private void writeObject(ObjectOutputStream stream) throws IOException {
        stream.defaultWriteObject();
        SerialUtils.writePaint(this.gridBandPaint, stream);
        SerialUtils.writePaint(this.gridBandAlternatePaint, stream);
    }

    /**
     * Provides serialization support.
     *
     * @param stream  the input stream.
     *
     * @throws IOException  if there is an I/O error.
     * @throws ClassNotFoundException  if there is a classpath problem.
     */
    private void readObject(ObjectInputStream stream)
            throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        this.gridBandPaint = SerialUtils.readPaint(stream);
        this.gridBandAlternatePaint = SerialUtils.readPaint(stream);
    }
}
