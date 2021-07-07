/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.oiexplorer.core.gui.chart;

import fr.jmmc.jmcs.gui.component.Disposable;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
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

/**
 * This class handles the overlay of the ruler
 *
 * @author martin
 */
public class RulerOverlay extends AbstractOverlay implements Overlay, EnhancedChartMouseListener, Disposable {

    private Point2D.Double origin, end;
    private final EnhancedChartPanel chartPanel;
    private final MotionListener motionListener = new MotionListener();

    final static int RECT_SIZE = 5;

    enum Translation {
        COORDS_TO_VALUES,
        VALUES_TO_COORDS
    }

    enum RulerState {
        DISABLED,
        EDITING,
        DONE
    }
    private RulerState rulerState = RulerState.DISABLED;

    /**
     *
     * @param chartPanel
     */
    public RulerOverlay(final ChartPanel chartPanel) {
        this.chartPanel = (EnhancedChartPanel) chartPanel;
        this.chartPanel.addChartMouseListener(this);
        this.chartPanel.addMouseMotionListener(motionListener);
    }

    /**
     * Free memory by removing listeners
     */
    public void dispose() {
        this.chartPanel.removeChartMouseListener(this);
        this.chartPanel.removeMouseMotionListener(motionListener);
    }

    /**
     * Convert plot coordinates (mas) to screen coordinates (pixels) if
     * translation == VALUES_TO_COORDS Convert screen coordinates (pixels) to
     * plot coordinates (mas) if translation == COORDS_TO_VALUES
     *
     * @param x
     * @param y
     * @return
     */
    private Point2D.Double translate(Point2D.Double p, Translation translation) {
        final Point2D pointOrigin = this.chartPanel.translateScreenToJava2D(new Point((int) p.getX(), (int) p.getY()));
        XYPlot plot = this.chartPanel.getChart().getXYPlot();

        Rectangle2D dataArea = null;

        final PlotRenderingInfo plotInfo = chartPanel.getChartRenderingInfo().getPlotInfo();

        final Rectangle2D plotDataArea = plotInfo.getDataArea();

        if (plotDataArea.contains(pointOrigin)) {
            dataArea = plotDataArea;
        } else {
            dataArea = plotInfo.getDataArea();
        }

        if (dataArea != null) {
            final ValueAxis xAxis = plot.getDomainAxis();
            final ValueAxis yAxis = plot.getRangeAxis();

            final RectangleEdge xAxisEdge = plot.getDomainAxisEdge();
            final RectangleEdge yAxisEdge = plot.getRangeAxisEdge();

            switch (translation) {
                case VALUES_TO_COORDS:
                    p = new Point2D.Double(xAxis.valueToJava2D(p.getX(), dataArea, xAxisEdge),
                            yAxis.valueToJava2D(p.getY(), dataArea, yAxisEdge));
                    break;
                case COORDS_TO_VALUES:
                    p = new Point2D.Double(xAxis.java2DToValue(p.getX(), dataArea, xAxisEdge),
                            yAxis.java2DToValue(p.getY(), dataArea, yAxisEdge));
                    break;
            }
        }
        return p;
    }

    /**
     * return the distance between the two points of the measure
     *
     * @return
     */
    private double calculateMeasure() {
        return Math.sqrt(Math.pow((end.getX() - origin.getX()), 2) + Math.pow((end.getY() - origin.getY()), 2));
    }

    private double calculateAngle() {
        return Math.toDegrees(Math.atan2(end.getY() - origin.getY(), end.getX() - origin.getX()));
    }

    @Override
    public void paintOverlay(Graphics2D g2, ChartPanel chartPanel) {
        if (this.rulerState != RulerState.DISABLED) {
            
            int height = chartPanel.getSize().height;
            g2.clip(chartPanel.getChartRenderingInfo().getPlotInfo().getDataArea());
            g2.setColor(Color.green);
            g2.setStroke(new BasicStroke(2));

            Point2D originCoords = translate(origin, Translation.VALUES_TO_COORDS);
            Point2D endCoords = translate(end, Translation.VALUES_TO_COORDS);
            g2.drawRect((int) originCoords.getX() - RECT_SIZE, (int) originCoords.getY() - RECT_SIZE, RECT_SIZE * 2, RECT_SIZE * 2);
            g2.drawRect((int) endCoords.getX() - RECT_SIZE, (int) endCoords.getY() - RECT_SIZE, RECT_SIZE * 2, RECT_SIZE * 2);
            g2.drawLine((int) originCoords.getX(), (int) originCoords.getY(), (int) endCoords.getX(), (int) endCoords.getY());

            g2.setClip(chartPanel.getChartRenderingInfo().getChartArea());
            g2.setColor(Color.black);
            g2.drawString(String.format("Point 1: x=%.5f y=%.5f", origin.getX(), origin.getY()), 100, height - 120);
            g2.drawString(String.format("Point 2: x=%.5f y=%.5f", end.getX(), end.getY()), 100, height - 105);
            g2.drawString(String.format("Measure: %.5f mas", calculateMeasure()), 100, height - 60);
            g2.drawString(String.format("Angle: %.5f °", calculateAngle()), 100, height - 45);
        }
    }

    @Override
    public boolean support(final int eventType) {
        return true;
    }

    @Override
    public void chartMouseMoved(final ChartMouseEvent chartMouseEvent) {
        if (rulerState == RulerState.EDITING) {
            end = translate(new Point2D.Double(
                    chartMouseEvent.getTrigger().getX(),
                    chartMouseEvent.getTrigger().getY()),
                    Translation.COORDS_TO_VALUES
            );
            chartPanel.repaint();
        }
    }

    @Override
    public void chartMouseClicked(final ChartMouseEvent chartMouseEvent) {

        switch (rulerState) {
            case DISABLED:
                rulerState = RulerState.DONE;
            case DONE:
                origin = translate(new Point2D.Double(
                        chartMouseEvent.getTrigger().getX(),
                        chartMouseEvent.getTrigger().getY()),
                        Translation.COORDS_TO_VALUES
                );
                end = origin;
                rulerState = RulerState.EDITING;
                chartPanel.repaint();
                break;

            case EDITING:
                end = translate(new Point2D.Double(
                        chartMouseEvent.getTrigger().getX(),
                        chartMouseEvent.getTrigger().getY()),
                        Translation.COORDS_TO_VALUES
                );
                rulerState = RulerState.DONE;
                chartPanel.repaint();
                break;
        }
    }

    /**
     * MouseMotionListener for dragging
     */
    private class MotionListener implements MouseMotionListener {

        private boolean doPointsIntersect(Point2D.Double p1, Point2D.Double p2, int spacing) {
            return (p1.getX() < p2.getX() + spacing
                    && p1.getX() > p2.getX() - spacing
                    && p1.getY() < p2.getX() + spacing
                    && p1.getY() > p2.getX() - spacing);
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            Point2D.Double p = translate(new Point2D.Double(
                    e.getPoint().getX(),
                    e.getPoint().getY()),
                    Translation.COORDS_TO_VALUES
            );

            if (doPointsIntersect(p, origin, RECT_SIZE)) {
                origin = p;
                chartPanel.repaint();
            } else if (doPointsIntersect(p, end, RECT_SIZE)) {
                end = p;
                chartPanel.repaint();
            }
        }

        @Override
        public void mouseMoved(MouseEvent e) {
        }
    }
}
