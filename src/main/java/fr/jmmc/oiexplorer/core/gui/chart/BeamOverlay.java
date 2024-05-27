/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.oiexplorer.core.gui.chart;

import fr.jmmc.jmcs.gui.util.SwingUtils;
import fr.jmmc.jmcs.util.ObjectUtils;
import fr.jmmc.oiexplorer.core.gui.FitsImagePanel;
import fr.jmmc.oitools.image.FitsUnit;
import fr.jmmc.oitools.processing.BeamInfo;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
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
 * This class handles the overlay of the gaussian beam (rx, ry, PA)
 *
 * @author laurent
 */
public final class BeamOverlay extends AbstractOverlay implements Overlay {

    /** Class logger */
    private static final Logger logger = LoggerFactory.getLogger(BeamOverlay.class.getName());

    private final static int MARKER_SIZE = SwingUtils.adjustUISize(3);

    /* members */
    /** parent FitsImagePanel */
    private final FitsImagePanel fitsImagePanel;
    /** enabled flag to show beam overlay */
    private boolean enabled = true;
    /** Beam information */
    private BeamInfo beamInfo = null;

    private final Ellipse2D ellipse = new Ellipse2D.Double();
    private final Line2D line = new Line2D.Double();

    public BeamOverlay(final FitsImagePanel fitsImagePanel) {
        this.fitsImagePanel = fitsImagePanel;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        if (this.enabled != enabled) {
            this.enabled = enabled;
            fireOverlayChanged();
        }
    }

    public BeamInfo getBeamInfo() {
        return beamInfo;
    }

    public void setBeamInfo(final BeamInfo beamInfo) {
        if (!ObjectUtils.areEquals(this.beamInfo, beamInfo)) {
            this.beamInfo = beamInfo;
            fireOverlayChanged();
        }
    }

    @Override
    public void paintOverlay(final Graphics2D g2, final ChartPanel chartPanel) {
        if (enabled && (beamInfo != null)) {
            logger.debug("paintOverlay: beamInfo: {}", beamInfo);

            final PlotRenderingInfo plotInfo = chartPanel.getChartRenderingInfo().getPlotInfo();
            final Rectangle2D dataArea = plotInfo.getDataArea();

            if (dataArea != null) {
                final Shape savedClip = g2.getClip();
                final AffineTransform savedTx = g2.getTransform();

                g2.clip(dataArea);

                final XYPlot plot = chartPanel.getChart().getXYPlot();

                final ValueAxis xAxis = plot.getDomainAxis();
                final ValueAxis yAxis = plot.getRangeAxis();

                final RectangleEdge xAxisEdge = plot.getDomainAxisEdge();
                final RectangleEdge yAxisEdge = plot.getRangeAxisEdge();

                final FitsUnit axisUnit = fitsImagePanel.getCurrentAxisUnit();

                // get the anchor point to draw the beam ellipse:
                double x = 0.0;
                double y = 0.0;

                // get origin for length conversions:
                final double zerox = xAxis.valueToJava2D(x, dataArea, xAxisEdge);
                final double zeroy = yAxis.valueToJava2D(y, dataArea, yAxisEdge);

                // use image center as (0,0) may be anywhere:
                if (xAxis instanceof BoundedNumberAxis) {
                    x = ((BoundedNumberAxis) xAxis).getBounds().getCentralValue();
                }

                if (yAxis instanceof BoundedNumberAxis) {
                    y = ((BoundedNumberAxis) yAxis).getBounds().getCentralValue();
                }

                if ((axisUnit != null) && (axisUnit != FitsUnit.ANGLE_MILLI_ARCSEC)) {
                    // convert values (mas) to axisUnit:
                    x = FitsUnit.ANGLE_MILLI_ARCSEC.convert(x, axisUnit);
                    y = FitsUnit.ANGLE_MILLI_ARCSEC.convert(y, axisUnit);
                }

                // convert axis coordinates:
                final double cx = xAxis.valueToJava2D(x, dataArea, xAxisEdge);
                final double cy = yAxis.valueToJava2D(y, dataArea, yAxisEdge);

                // major-axis is aligned on north axis (y):
                x = beamInfo.ry; // minor axis (hw-hm)
                y = beamInfo.rx; // minor axis (hw-hm)

                if ((axisUnit != null) && (axisUnit != FitsUnit.ANGLE_MILLI_ARCSEC)) {
                    // convert values (mas) to axisUnit:
                    x = FitsUnit.ANGLE_MILLI_ARCSEC.convert(x, axisUnit);
                    y = FitsUnit.ANGLE_MILLI_ARCSEC.convert(y, axisUnit);
                }

                // convert lengths (hw-hm):
                final double wx = Math.abs(xAxis.valueToJava2D(x, dataArea, xAxisEdge) - zerox);
                final double wy = Math.abs(yAxis.valueToJava2D(y, dataArea, yAxisEdge) - zeroy);

                g2.translate(cx, cy);
                g2.rotate((xAxis.isInverted() ? -1.0 : 1.0) * Math.toRadians(beamInfo.angle));

                g2.setColor(Color.GREEN);

                g2.setStroke(ChartUtils.DEFAULT_STROKE);
                // horizontal markers:
                line.setLine(-wx - MARKER_SIZE, 0.0, -wx + MARKER_SIZE, 0);
                g2.draw(line);
                line.setLine(+wx - MARKER_SIZE, 0.0, +wx + MARKER_SIZE, 0);
                g2.draw(line);
                // vertical markers:
                line.setLine(0, -wy - MARKER_SIZE, 0, -wy + MARKER_SIZE);
                g2.draw(line);
                line.setLine(0, +wy - MARKER_SIZE, 0, +wy + MARKER_SIZE);
                g2.draw(line);

                g2.setStroke(ChartUtils.LARGE_STROKE);
                // ellipse:
                ellipse.setFrame(-wx, -wy, 2.0 * wx, 2.0 * wy);
                g2.draw(ellipse);

                g2.setTransform(savedTx);
                g2.setClip(savedClip);
            }
        }
    }

}
