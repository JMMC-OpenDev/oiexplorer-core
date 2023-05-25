/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.oiexplorer.core.export;

import fr.jmmc.jmcs.data.MimeType;
import fr.jmmc.jmcs.gui.util.SwingUtils;
import java.awt.geom.Rectangle2D;

/**
 *
 * @author bourgesl
 */
public final class PDFOptions extends DocumentOptions {

    protected PDFOptions(final MimeType mimeType) {
        super(mimeType);
    }

    @Override
    public Rectangle2D.Float adjustDocumentSize() {
        // adjust document size (SMALL, A3, A2) and orientation according to the options :
        int scale;
        switch (getDocumentSize()) {
            default:
            case SMALL:
                scale = 1;
                break;
            case NORMAL:
                scale = 2;
                break;
            case LARGE:
                scale = 4;
                break;
        }

        // Adjust Hi-dpi screens => enlarge document size:
        final int scaleUI = (int) Math.ceil(SwingUtils.adjustUISize(1.0));

        if (scaleUI > 1) {
            scale *= scaleUI;
        }

        com.lowagie.text.Rectangle documentPage;

        if (scale <= 1) {
            documentPage = com.lowagie.text.PageSize.A4;
        } else if (scale <= 2) {
            documentPage = com.lowagie.text.PageSize.A3;
        } else if (scale <= 4) {
            documentPage = com.lowagie.text.PageSize.A2;
        } else {
            documentPage = com.lowagie.text.PageSize.A1;
        }

        if (Orientation.Landscape == getOrientation()) {
            documentPage = documentPage.rotate();
        }
        return new Rectangle2D.Float(documentPage.getLeft(), documentPage.getBottom(),
                documentPage.getWidth(), documentPage.getHeight());
    }

    /**
     * Overwrite options of the method parameter if the command options are not null or default
     * @param otherOptions
     */
    @Override
    public void merge(final DocumentOptions otherOptions) {
        super.merge(otherOptions);
        logger.debug("merge(PDFOptions): this: {}", this);
    }
}
