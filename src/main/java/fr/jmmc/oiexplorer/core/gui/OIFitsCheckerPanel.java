/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.oiexplorer.core.gui;

import fr.jmmc.jmcs.gui.component.ResizableTextViewFactory;
import fr.jmmc.oitools.model.OIFitsChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author bourgesl
 */
public final class OIFitsCheckerPanel {

    /** Class logger */
    private static final Logger logger = LoggerFactory.getLogger(OIFitsChecker.class.getName());

    public static void displayReport(final OIFitsChecker checker) {
        if (checker != null) {
            // log validation messages anyway:
            logger.info("validation results:\n{}", checker.getCheckReport()); // full mode

            // TODO: use a preference to show or hide the validation report:
            if (true && !checker.isEmpty()) {
                // display validation messages in compact mode:
                final String compactReport = checker.getCheckReport(true);

                // Create model dialog:
                ResizableTextViewFactory.createTextWindow(compactReport, "OIFits validator", true);
            }
        }
    }

    private OIFitsCheckerPanel() {
        super();
    }
}
