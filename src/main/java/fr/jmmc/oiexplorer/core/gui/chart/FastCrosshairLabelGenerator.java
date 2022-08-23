/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.oiexplorer.core.gui.chart;

import fr.jmmc.jmcs.util.NumberUtils;
import java.io.Serializable;
import org.jfree.chart.labels.CrosshairLabelGenerator;
import org.jfree.chart.plot.Crosshair;

/**
 * Fast label generator using NumberUtils.format(value)
 * @author bourgesl
 */
public final class FastCrosshairLabelGenerator implements CrosshairLabelGenerator,
                                                          Serializable {

    private static final long serialVersionUID = 1L;

    public static final CrosshairLabelGenerator INSTANCE = new FastCrosshairLabelGenerator();

    /**
     * Creates a new instance
     */
    private FastCrosshairLabelGenerator() {
        super();
    }

    /**
     * Returns a string that can be used as the label for a crosshair.
     *
     * @param crosshair  the crosshair ({@code null} not permitted).
     *
     * @return The label (possibly {@code null}).
     */
    @Override
    public String generateLabel(final Crosshair crosshair) {
        return NumberUtils.format(crosshair.getValue());
    }

}
