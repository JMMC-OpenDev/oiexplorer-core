/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.jmmc.oiexplorer.core.gui.chart;

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
public class RulerOverlay extends AbstractOverlay implements Overlay, EnhancedChartMouseListener {

    private PointDouble origin, end;

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

    private final EnhancedChartPanel chartPanel;
    private final MotionListener motionListener = new MotionListener();

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
    public void destroy() {
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
    private PointDouble translate(PointDouble p, Translation translation) {
        final Point2D pointOrigin = this.chartPanel.translateScreenToJava2D(p);
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
                    p = new PointDouble(xAxis.valueToJava2D(p.getX(), dataArea, xAxisEdge),
                            yAxis.valueToJava2D(p.getY(), dataArea, yAxisEdge));
                    break;
                case COORDS_TO_VALUES:
                    p = new PointDouble(xAxis.java2DToValue(p.getX(), dataArea, xAxisEdge),
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
        return Math.toDegrees(Math.atan2((end.getX() - origin.getX()), (end.getY() - origin.getY())));
    }

    @Override
    public void paintOverlay(Graphics2D g2, ChartPanel chartPanel) {
        if (this.rulerState != RulerState.DISABLED) {
            g2.clip(chartPanel.getChartRenderingInfo().getPlotInfo().getDataArea());
            g2.setColor(Color.green);
            g2.setStroke(new BasicStroke(2));

            PointDouble originCoords = translate(origin, Translation.VALUES_TO_COORDS);
            PointDouble endCoords = translate(end, Translation.VALUES_TO_COORDS);
            g2.drawRect((int) originCoords.getX() - RECT_SIZE, (int) originCoords.getY() - RECT_SIZE, RECT_SIZE * 2, RECT_SIZE * 2);
            g2.drawRect((int) endCoords.getX() - RECT_SIZE, (int) endCoords.getY() - RECT_SIZE, RECT_SIZE * 2, RECT_SIZE * 2);
            g2.drawLine((int) originCoords.getX(), (int) originCoords.getY(), (int) endCoords.getX(), (int) endCoords.getY());

            g2.setClip(chartPanel.getChartRenderingInfo().getChartArea());
            g2.setColor(Color.black);
            g2.drawString("Point 1: x=" + String.format("%.5f", origin.getX()) + " y=" + String.format("%.5f", origin.getY()), 100, chartPanel.getSize().height - 120);
            g2.drawString("Point 2: x=" + String.format("%.5f", end.getX()) + " y=" + String.format("%.5f", end.getY()), 100, chartPanel.getSize().height - 105);
            g2.drawString("Measure: " + String.format("%.5f", calculateMeasure()) + " mas", 100, chartPanel.getSize().height - 60);
            g2.drawString("Angle: " + String.format("%.5f", calculateAngle()) + " °", 100, chartPanel.getSize().height - 45);
        }
    }

    @Override
    public boolean support(final int eventType) {
        return true;
    }

    @Override
    public void chartMouseMoved(final ChartMouseEvent chartMouseEvent) {
        if (rulerState == RulerState.EDITING) {
            end = translate(new PointDouble(
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
                origin = translate(new PointDouble(
                        chartMouseEvent.getTrigger().getX(),
                        chartMouseEvent.getTrigger().getY()),
                        Translation.COORDS_TO_VALUES
                );
                end = origin;
                rulerState = RulerState.EDITING;
                chartPanel.repaint();
                break;

            case EDITING:
                end = translate(new PointDouble(
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

        private boolean doPointsIntersect(PointDouble p1, PointDouble p2, int spacing) {
            return (p1.getX() < p2.getX() + spacing
                    && p1.getX() > p2.getX() - spacing
                    && p1.getY() < p2.getX() + spacing
                    && p1.getY() > p2.getX() - spacing);
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            PointDouble p = translate(new PointDouble(
                    e.getPoint().getX(),
                    e.getPoint().getY()),
                    Translation.COORDS_TO_VALUES
            );

            if (doPointsIntersect(p, origin, RECT_SIZE)) {
                origin = translate(new PointDouble(
                        e.getX(),
                        e.getY()),
                        Translation.COORDS_TO_VALUES
                );
                chartPanel.repaint();
            } else if (doPointsIntersect(p, end, RECT_SIZE)) {
                end = translate(new PointDouble(
                        e.getX(),
                        e.getY()),
                        Translation.COORDS_TO_VALUES
                );
                chartPanel.repaint();
            }
        }

        @Override
        public void mouseMoved(MouseEvent e) {
        }

    }

    /**
     * Extension of the Point class to allow coordinates of type Double
     */
    private class PointDouble extends Point {

        private double x;
        private double y;

        PointDouble(double x, double y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public double getX() {
            return this.x;
        }

        @Override
        public double getY() {
            return this.y;
        }
    }
}
