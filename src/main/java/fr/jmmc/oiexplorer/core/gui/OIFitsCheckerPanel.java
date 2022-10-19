/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.oiexplorer.core.gui;

import fr.jmmc.jmcs.gui.component.ResizableTextViewFactory;
import fr.jmmc.oiexplorer.core.Preferences;
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

    public static void displayReport(final OIFitsChecker checker, final Preferences preferences) {
        if (checker != null) {
            // get validation messages in compact mode:
            final String compactReport = checker.getCheckReport(true);
            // log validation messages anyway:
            logger.info("validation results:\n{}", compactReport);

            // TODO: use a preference to show or hide the validation report:
            if (!checker.isEmpty()) {
                // return false if the preference value is missing:
                final boolean dontShow = (preferences != null) ? preferences.getPreferenceAsBoolean(Preferences.VALIDATOR_DONT_SHOW_REPORT, true) : false;

                if (!dontShow) {
                    // display validation messages:
                    final String text = "JMMC OIFITS Validator rules defined at:\n"
                            + "https://jmmc-opendev.github.io/oitools/rules/DataModelV2_output.html"
                            + "\n\n" + compactReport;

                    // TODO: use HTML for color codes ?
                    // Create model dialog:
                    ResizableTextViewFactory.createTextWindow(text, "OIFits validator", true);
                }
            }
        }
    }

    private OIFitsCheckerPanel() {
        super();
    }
}
