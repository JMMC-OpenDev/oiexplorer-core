/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.oiexplorer.core.gui.chart;

import fr.jmmc.jmcs.gui.component.Disposable;
import fr.jmmc.jmcs.gui.util.SwingUtils;
import fr.jmmc.oiexplorer.core.gui.FitsImagePanel;
import fr.jmmc.oitools.image.FitsUnit;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.panel.AbstractOverlay;
import org.jfree.chart.panel.Overlay;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.ui.RectangleEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class handles the overlay of the ruler
 *
 * @author martin
 */
public final class RulerOverlay extends AbstractOverlay implements Overlay, EnhancedChartMouseListener, Disposable {

    /** Class logger */
    private static final Logger logger = LoggerFactory.getLogger(RulerOverlay.class.getName());

    private final static int RECT_SIZE = SwingUtils.adjustUISize(5);
    private final static int LINE_HEIGHT = SwingUtils.adjustUISize(15);

    enum Translation {
        COORDS_TO_VALUES,
        VALUES_TO_COORDS
    }

    enum RulerState {
        DISABLED,
        EDIT_ORIG,
        EDIT_END,
        DONE
    }

    /* members */
    private final FitsImagePanel fitsImagePanel;
    private final EnhancedChartPanel chartPanel;
    private RulerState rulerState = RulerState.DISABLED;
    /* points in data space (milli-arcsecs) */
    private Point2D origin = null;
    private Point2D end = null;

    /**
     *
     * @param fitsImagePanel
     * @param chartPanel
     */
    public RulerOverlay(final FitsImagePanel fitsImagePanel, final ChartPanel chartPanel) {
        this.fitsImagePanel = fitsImagePanel;
        this.chartPanel = (EnhancedChartPanel) chartPanel;
        this.chartPanel.addChartMouseListener(this);
    }

    /**
     * Free memory by removing listeners
     */
    @Override
    public void dispose() {
        this.chartPanel.removeChartMouseListener(this);
    }

    /**
     * Convert plot coordinates (mas) to screen coordinates (pixels) if
     * translation == VALUES_TO_COORDS Convert screen coordinates (pixels) to
     * plot coordinates (mas) if translation == COORDS_TO_VALUES
     *
     * @param p input point
     * @param translation direction of conversion
     * @return output point or null
     */
    private Point2D translate(final Point2D p, final Translation translation) {
        final PlotRenderingInfo plotInfo = chartPanel.getChartRenderingInfo().getPlotInfo();

        final Rectangle2D dataArea = plotInfo.getDataArea();

        if (dataArea != null) {
            final XYPlot plot = this.chartPanel.getChart().getXYPlot();

            final ValueAxis xAxis = plot.getDomainAxis();
            final ValueAxis yAxis = plot.getRangeAxis();

            final RectangleEdge xAxisEdge = plot.getDomainAxisEdge();
            final RectangleEdge yAxisEdge = plot.getRangeAxisEdge();

            final FitsUnit axisUnit = fitsImagePanel.getCurrentAxisUnit();

            double x = p.getX();
            double y = p.getY();

            switch (translation) {
                case VALUES_TO_COORDS:
                    if ((axisUnit != null) && (axisUnit != FitsUnit.ANGLE_MILLI_ARCSEC)) {
                        // convert values (mas) to axisUnit:
                        x = FitsUnit.ANGLE_MILLI_ARCSEC.convert(x, axisUnit);
                        y = FitsUnit.ANGLE_MILLI_ARCSEC.convert(y, axisUnit);
                    }
                    // convert axis coordinates:
                    x = xAxis.valueToJava2D(x, dataArea, xAxisEdge);
                    y = yAxis.valueToJava2D(y, dataArea, yAxisEdge);
                    break;
                case COORDS_TO_VALUES:
                    // convert axis coordinates:
                    x = xAxis.java2DToValue(x, dataArea, xAxisEdge);
                    y = yAxis.java2DToValue(y, dataArea, yAxisEdge);

                    if ((axisUnit != null) && (axisUnit != FitsUnit.ANGLE_MILLI_ARCSEC)) {
                        // convert axisUnit to values (mas):
                        x = axisUnit.convert(x, FitsUnit.ANGLE_MILLI_ARCSEC);
                        y = axisUnit.convert(y, FitsUnit.ANGLE_MILLI_ARCSEC);
                    }
                    break;
            }
            return new Point2D.Double(x, y);
        }
        return null;
    }

    /**
     * return the distance between the two points of the measure
     *
     * @return distance in milli-arcsecs
     */
    private double calculateMeasure() {
        return Math.sqrt(Math.pow((end.getX() - origin.getX()), 2) + Math.pow((end.getY() - origin.getY()), 2));
    }

    private double calculateAngle() {
        // TODO: correct orientation (north)
        return Math.toDegrees(Math.atan2(end.getX() - origin.getX(), end.getY() - origin.getY()));
    }

    @Override
    public void paintOverlay(final Graphics2D g2, final ChartPanel chartPanel) {
        if (rulerState != RulerState.DISABLED) {
            logger.debug("paintOverlay: rulerState: {}", rulerState);

            g2.clip(chartPanel.getChartRenderingInfo().getPlotInfo().getDataArea());
            g2.setStroke(ChartUtils.LARGE_STROKE);

            final Point2D originCoords = translate(origin, Translation.VALUES_TO_COORDS);
            final Point2D endCoords = translate(end, Translation.VALUES_TO_COORDS);

            g2.setColor(((origin == end) || (rulerState == RulerState.EDIT_ORIG)) ? Color.MAGENTA : Color.GREEN);
            g2.drawRect((int) originCoords.getX() - RECT_SIZE, (int) originCoords.getY() - RECT_SIZE, RECT_SIZE * 2, RECT_SIZE * 2);

            if (origin != end) {
                g2.setColor((rulerState == RulerState.EDIT_END) ? Color.MAGENTA : Color.GREEN);
                g2.drawRect((int) endCoords.getX() - RECT_SIZE, (int) endCoords.getY() - RECT_SIZE, RECT_SIZE * 2, RECT_SIZE * 2);

                g2.setColor(Color.GREEN);
                g2.drawLine((int) originCoords.getX(), (int) originCoords.getY(), (int) endCoords.getX(), (int) endCoords.getY());

                // TODO: show angle
                // draw vertical at origin (north)
                // draw arc between (north axis and vector)
            }

            // move info into another panel (label)
            final int height = chartPanel.getSize().height;

            g2.setClip(chartPanel.getChartRenderingInfo().getChartArea());
            g2.setColor(Color.BLACK);
            g2.drawString(String.format("Point 1: x=%.5f y=%.5f", origin.getX(), origin.getY()), 100, height - 6 * LINE_HEIGHT);
            g2.drawString(String.format("Point 2: x=%.5f y=%.5f", end.getX(), end.getY()), 100, height - 5 * LINE_HEIGHT);
            g2.drawString(String.format("Measure: %.5f mas", calculateMeasure()), 100, height - 4 * LINE_HEIGHT);
            g2.drawString(String.format("Angle: %.5f °", calculateAngle()), 100, height - 3 * LINE_HEIGHT);
        }
    }

    @Override
    public boolean support(final int eventType) {
        return true;
    }

    @Override
    public void chartMouseClicked(final ChartMouseEvent cme) {
        logger.debug("chartMouseClicked: {}", rulerState);

        final Point2D pCoords = new Point2D.Double(cme.getTrigger().getX(), cme.getTrigger().getY());
        final Point2D p = translate(pCoords, Translation.COORDS_TO_VALUES);

        switch (rulerState) {
            case DISABLED:
            // set origin = end, like DONE
            case DONE:
                if (rulerState == RulerState.DONE) {
                    final Point2D originCoords = translate(origin, Translation.VALUES_TO_COORDS);
                    final Point2D endCoords = translate(end, Translation.VALUES_TO_COORDS);

                    // test if click on orig or end points in pixel space ?
                    if (doPointsIntersect(pCoords, originCoords, RECT_SIZE)) {
                        rulerState = RulerState.EDIT_ORIG;
                        chartPanel.repaint();
                        return;
                    } else if (doPointsIntersect(pCoords, endCoords, RECT_SIZE)) {
                        rulerState = RulerState.EDIT_END;
                        chartPanel.repaint();
                        return;
                    }
                }

                // Set origin = end:
                origin = p;
                end = origin;
                rulerState = RulerState.EDIT_END;
                chartPanel.repaint();
                break;

            case EDIT_ORIG:
                // Set origin:
                origin = p;
                rulerState = RulerState.DONE;
                chartPanel.repaint();
                break;

            case EDIT_END:
                // Set end:
                end = p;
                rulerState = RulerState.DONE;
                chartPanel.repaint();
                break;
        }
    }

    @Override
    public void chartMouseMoved(final ChartMouseEvent cme) {
        if ((rulerState == RulerState.EDIT_ORIG) || (rulerState == RulerState.EDIT_END)) {
            logger.debug("chartMouseMoved: {}", rulerState);

            final Point2D pCoords = new Point2D.Double(cme.getTrigger().getX(), cme.getTrigger().getY());
            final Point2D p = translate(pCoords, Translation.COORDS_TO_VALUES);

            if (rulerState == RulerState.EDIT_ORIG) {
                origin = p;
                chartPanel.repaint();
            } else if (rulerState == RulerState.EDIT_END) {
                end = p;
                chartPanel.repaint();
            }
        }
    }

    private static boolean doPointsIntersect(final Point2D p1, final Point2D p2, final double spacing) {
        final double dx = p1.getX() - p2.getX();
        final double dy = p1.getY() - p2.getY();
        return (Math.abs(dx) <= spacing && Math.abs(dy) <= spacing);
    }
}
