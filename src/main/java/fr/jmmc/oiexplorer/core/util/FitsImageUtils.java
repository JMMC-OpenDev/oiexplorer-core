/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.oiexplorer.core.util;

import fr.jmmc.jmal.ALX;
import fr.jmmc.jmal.image.ImageArrayUtils;
import fr.jmmc.jmal.image.job.ImageLowerThresholdJob;
import fr.jmmc.jmal.image.job.ImageMinMaxJob;
import fr.jmmc.jmal.image.job.ImageNormalizeJob;
import fr.jmmc.jmcs.util.NumberUtils;
import fr.jmmc.oitools.image.FitsImage;
import fr.jmmc.oitools.image.FitsImageFile;
import fr.jmmc.oitools.image.FitsImageHDU;
import fr.jmmc.oitools.image.FitsImageLoader;
import fr.jmmc.oitools.image.FitsUnit;
import fr.jmmc.oitools.processing.Resampler;
import fr.jmmc.oitools.processing.Resampler.Filter;
import fr.jmmc.oitools.util.ArrayConvert;
import fr.nom.tam.fits.FitsException;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This utility class provides several helper methods over FitsImage class
 * 
 * TODO: enhance profile usage and add new dynamic histogram (log(value))
 * 
 * @author bourgesl
 */
public final class FitsImageUtils {

    /* constants */
    /** Logger associated to image classes */
    private final static Logger logger = LoggerFactory.getLogger(FitsImageUtils.class.getName());

    public final static int MAX_IMAGE_SIZE = 4096;

    /** Smallest positive number used in double comparisons (rounding). */
    public final static double MAS_EPSILON = 1e-6d * ALX.MILLI_ARCSEC_IN_DEGREES;

    /**
     * Forbidden constructor
     */
    private FitsImageUtils() {
        super();
    }

    /**
     * Create a new FitsImage given its data and updates dataMin/Max
     * @param data image data as float[nbRows][nbCols] ie [Y][X]
     * @return new FitsImage
     */
    public static FitsImage createFitsImage(final float[][] data) {
        final FitsImage fitsImage = new FitsImage();

        updateFitsImage(fitsImage, data);

        return fitsImage;
    }

    /**
     * Update data of the given FitsImage given its data and updates dataMin/Max
     * @param image FitsImage to update
     * @param data image data as float[nbRows][nbCols] ie [Y][X]
     */
    public static void updateFitsImage(final FitsImage image, final float[][] data) {
        image.setData(data);

        // update dataMin/Max:
        updateDataRangeExcludingZero(image);
    }

    /**
     * Create a new FitsImage given its data and updates dataMin/Max
     * @param data image data as float[nbRows][nbCols] ie [Y][X]
     * @param dataMin minimum value in data
     * @param dataMax maximum value in data
     * @return new FitsImage
     */
    public static FitsImage createFitsImage(final float[][] data,
                                            final double dataMin, final double dataMax) {
        final FitsImage fitsImage = new FitsImage();

        updateFitsImage(fitsImage, data, dataMin, dataMax);

        return fitsImage;
    }

    /**
     * Update data of the given FitsImage given its data and updates dataMin/Max
     * @param fitsImage FitsImage to update
     * @param data image data as float[nbRows][nbCols] ie [Y][X]
     * @param dataMin minimum value in data
     * @param dataMax maximum value in data
     */
    public static void updateFitsImage(final FitsImage fitsImage, final float[][] data,
                                       final double dataMin, final double dataMax) {
        if (fitsImage != null) {
            fitsImage.setData(data);

            fitsImage.setDataMin(dataMin);
            fitsImage.setDataMax(dataMax);
        }
    }

    /**
     * Create a new FitsImage given its data and coordinate informations
     * and updates dataMin/Max
     * @param data image data as float[nbRows][nbCols] ie [Y][X]
     * @param pixRefRow row index of the reference pixel (real starting from 1.0)
     * @param pixRefCol column position of the reference pixel (real starting from 1.0)
     * @param incRow signed coordinate increment along the row axis in radians
     * @param incCol signed coordinate increment along the column axis in radians
     * @return new FitsImage
     */
    public static FitsImage createFitsImage(final float[][] data,
                                            final double pixRefRow, final double pixRefCol,
                                            final double incRow, final double incCol) {

        final FitsImage fitsImage = createFitsImage(data);

        fitsImage.setPixRefRow(pixRefRow);
        fitsImage.setPixRefCol(pixRefCol);

        fitsImage.setSignedIncRow(incRow);
        fitsImage.setSignedIncCol(incCol);

        return fitsImage;
    }

    /**
     * Create a new FitsImage given its data and coordinate informations
     * and updates dataMin/Max
     * @param data image data as float[nbRows][nbCols] ie [Y][X]
     * @param dataMin minimum value in data
     * @param dataMax maximum value in data
     * @param pixRefRow row index of the reference pixel (real starting from 1.0)
     * @param pixRefCol column position of the reference pixel (real starting from 1.0)
     * @param incRow signed coordinate increment along the row axis in radians
     * @param incCol signed coordinate increment along the column axis in radians
     * @return new FitsImage
     */
    public static FitsImage createFitsImage(final float[][] data,
                                            final double dataMin, final double dataMax,
                                            final double pixRefRow, final double pixRefCol,
                                            final double incRow, final double incCol) {

        final FitsImage fitsImage = new FitsImage();

        updateFitsImage(fitsImage, data, dataMin, dataMax);

        fitsImage.setPixRefRow(pixRefRow);
        fitsImage.setPixRefCol(pixRefCol);

        fitsImage.setSignedIncRow(incRow);
        fitsImage.setSignedIncCol(incCol);

        return fitsImage;
    }

    /**
     * Load the given file and return a FitsImageFile structure.
     * This methods updates dataMin/Max of each FitsImage
     *
     * @param absFilePath absolute File path on file system (not URL)
     * @param firstOnly load only the first valid Image HDU
     * @return FitsImageFile structure on success
     * 
     * @throws FitsException if any FITS error occured
     * @throws IOException IO failure
     * @throws IllegalArgumentException if unsupported unit or unit conversion is not allowed
     */
    public static FitsImageFile load(final String absFilePath, final boolean firstOnly) throws FitsException, IOException, IllegalArgumentException {
        final FitsImageFile imgFitsFile = FitsImageLoader.load(absFilePath, firstOnly);

        for (FitsImageHDU fitsImageHDU : imgFitsFile.getFitsImageHDUs()) {
            for (FitsImage fitsImage : fitsImageHDU.getFitsImages()) {
                // update boundaries excluding zero values:
                updateDataRangeExcludingZero(fitsImage);
            }
        }

        return imgFitsFile;
    }

    public static void prepareAllImages(final List<FitsImageHDU> hdus) throws IllegalArgumentException {
        if (hdus != null) {
            for (FitsImageHDU hdu : hdus) {
                for (FitsImage fitsImage : hdu.getFitsImages()) {
                    // note: fits image instance can be modified by image preparation:
                    // can throw IllegalArgumentException if image has invalid keyword(s) / data:
                    FitsImageUtils.prepareImage(fitsImage);
                }
            }
        }
    }

    /**
     * Prepare the given image and Update the given FitsImage by the prepared FitsImage ready for display
     * @param fitsImage FitsImage to process
     * @throws IllegalArgumentException if image has invalid keyword(s) / data
     */
    public static void prepareImage(final FitsImage fitsImage) throws IllegalArgumentException {
        if (fitsImage != null) {
            if (!fitsImage.isDataRangeDefined()) {
                // update boundaries excluding zero values:
                updateDataRangeExcludingZero(fitsImage);
            }

            // in place modifications:
            float[][] data = fitsImage.getData();
            int nbRows = fitsImage.getNbRows();
            int nbCols = fitsImage.getNbCols();

            logger.info("Image size: {} x {}", nbRows, nbCols);

            // 1 - Ignore negative values:
            
            // TODO: fix special case: image is [0] !
            if (fitsImage.getDataMax() <= 0d) {
                throw new IllegalArgumentException("Fits image [" + fitsImage.getFitsImageIdentifier() + "] has only negative data !");
            }
            if (fitsImage.getDataMin() < 0d) {
                final float threshold = 0f;

                final ImageLowerThresholdJob thresholdJob = new ImageLowerThresholdJob(data, nbCols, nbRows, threshold, 0f);
                logger.info("ImageLowerThresholdJob - threshold = {} (ignore negative values)", threshold);

                thresholdJob.forkAndJoin();

                logger.info("ImageLowerThresholdJob - updateCount: {}", thresholdJob.getUpdateCount());

                // update boundaries excluding zero values:
                FitsImageUtils.updateDataRangeExcludingZero(fitsImage);
            }

            // 2 - Normalize data (total flux):
            if (!NumberUtils.equals(fitsImage.getSum(), 1.0, 1e-3)) {
                final double normFactor = 1d / fitsImage.getSum();

                final ImageNormalizeJob normJob = new ImageNormalizeJob(data, nbCols, nbRows, normFactor);
                logger.info("ImageNormalizeJob - factor: {}", normFactor);

                normJob.forkAndJoin();

                // update boundaries excluding zero values:
                FitsImageUtils.updateDataRangeExcludingZero(fitsImage);
            }

            // 3 - Make sure the image is square i.e. padding (width = height = even number):
            final int size = Math.max(nbRows, nbCols);
            final int newSize = (size % 2 != 0) ? size + 1 : size;

            if (newSize != nbRows || newSize != nbCols) {
                data = ImageArrayUtils.enlarge(nbRows, nbCols, data, newSize, newSize);

                // update data/dataMin/dataMax:
                FitsImageUtils.updateFitsImage(fitsImage, data, fitsImage.getDataMin(), fitsImage.getDataMax());

                // update ref pixel:
                fitsImage.setPixRefRow(fitsImage.getPixRefRow() + ((newSize - nbRows) / 2));
                fitsImage.setPixRefCol(fitsImage.getPixRefCol() + ((newSize - nbCols) / 2));

                nbRows = fitsImage.getNbRows();
                nbCols = fitsImage.getNbCols();

                logger.info("Square size = {} x {}", nbRows, nbCols);
            }
        }
    }

    /** 
     * Update the data Min/Max of the given fitsImage
     * @param fitsImage fitsImage to process and update
     */
    public static void updateDataRange(final FitsImage fitsImage) {
        updateDataRange(fitsImage, false);
    }

    /** 
     * Update the data Min/Max of the given fitsImage excluding values equals to zero
     * @param fitsImage fitsImage to process and update
     */
    public static void updateDataRangeExcludingZero(final FitsImage fitsImage) {
        updateDataRange(fitsImage, true);
    }

    /** 
     * Update the data Min/Max of the given fitsImage
     * @param fitsImage fits image to process and update
     * @param excludeZero true to indicate to ignore zero values
     */
    private static void updateDataRange(final FitsImage fitsImage, final boolean excludeZero) {
        if (fitsImage != null) {
            // update min/max ignoring zero:
            final ImageMinMaxJob minMaxJob = new ImageMinMaxJob(fitsImage.getData(),
                    fitsImage.getNbCols(), fitsImage.getNbRows(), excludeZero);

            minMaxJob.forkAndJoin();

            if (logger.isInfoEnabled()) {
                logger.info("ImageMinMaxJob min: {} - max: {} - nData: {} - sum: {}",
                        minMaxJob.getMin(), minMaxJob.getMax(), minMaxJob.getNData(), minMaxJob.getSum());
            }

            // update nData:
            fitsImage.setNData(minMaxJob.getNData());
            // update sum:
            fitsImage.setSum(minMaxJob.getSum());

            // update dataMin/dataMax:
            fitsImage.setDataMin(minMaxJob.getMin());
            fitsImage.setDataMax(minMaxJob.getMax());
        }
    }

    public static void changeViewportImages(final FitsImageHDU hdu, final Rectangle2D.Double newArea) throws IllegalArgumentException {
        if (newArea == null || newArea.isEmpty()) {
            throw new IllegalStateException("Invalid area: " + newArea);
        }
        if (hdu != null && hdu.hasImages()) {
            for (FitsImage fitsImage : hdu.getFitsImages()) {
                // First modify image:
                changeViewportImage(fitsImage, newArea);

                // note: fits image instance can be modified by image preparation:
                // can throw IllegalArgumentException if image has invalid keyword(s) / data:
                FitsImageUtils.prepareImage(fitsImage);
            }
        }
    }

    public static class ImageSize {
        public double fovMas;
        public int nbPixels;
        public double pixelSizeMas;
    }

    public static ImageSize computeNewImageSizeIfResampleV2(final FitsImage fitsImage, double newFovMas, double newPixelSizeMas) {

        final ImageSize newImageSize = new ImageSize();

        // VIEWPORT
        // computing new Area from newFovMas
        Rectangle2D.Double newArea = computeNewAreaIfNewFov(fitsImage.getArea(), newFovMas);
        if (newArea == null) {
            return null;
        }

        // computing new nbPixels from, newArea
        Rectangle newSizes = computeNewSizesIfNewArea(fitsImage, newArea);

        // we keep the nbPixels from computation
        newImageSize.nbPixels = newSizes.width;

        // pixelSize never changes when we change viewport (it is our choice)
        newImageSize.pixelSizeMas = FitsUnit.ANGLE_RAD.convert(fitsImage.getIncRow(), FitsUnit.ANGLE_MILLI_ARCSEC);

        // we reajust fov: because nbPixels is an integer, it may not reflects exactly the change in the fov.
        // So we reajust fov to be as close as possible to the ideal value (which could not be available because
        // double is not a perfect precision format).
        // the ideal value is such that: fov == pixelSize * nbPixels.
        // in the function that actually modify the viewport, this reajustment is implicit
        // by the fact that the area will be set to null (and recomputed on demand)
        newImageSize.fovMas = newImageSize.pixelSizeMas * newImageSize.nbPixels;

        // RESAMPLE

        final double oldPixelSizeMas = FitsUnit.ANGLE_RAD.convert(fitsImage.getIncCol(), FitsUnit.ANGLE_MILLI_ARCSEC);

        newImageSize.fovMas = newSizes.width * oldPixelSizeMas;

        final int newNbPixels = computeNbPixelsIfChangePixelSize(newSizes.width, oldPixelSizeMas, newPixelSizeMas);

        // nbPixels changes when we change pixelSizeMas
        newImageSize.nbPixels = newNbPixels;

        // for same reason as viewport, we reajust pixelSize to be the more accurate possible.
        newImageSize.pixelSizeMas = (newNbPixels == 0) ? Double.POSITIVE_INFINITY : newImageSize.fovMas / newImageSize.nbPixels;

        // fov is kept constant when resample (by our choice)
        return newImageSize;
    }

    /** computes an Area resulting from a change in the fov.
     * it starts from center of the current fov, and extend to the new fov.
     * @param oldArea optional.
     * @param newFovMas  must be positive.
     * @return new area. null if params were wrong.
     */
    public static Rectangle2D.Double computeNewAreaIfNewFov(final Rectangle2D.Double oldArea, final double newFovMas) {
        // some checks
        if (oldArea == null || newFovMas <= 0) {
            return null;
        }

        // convert to radians because it is the unit used in oldArea
        final double newFovRad = FitsUnit.ANGLE_MILLI_ARCSEC.convert(newFovMas, FitsUnit.ANGLE_RAD);

        final double halfNewFovRad = newFovRad / 2.0D;

        // Starting from center, define top-left corner and bottom-right corner
        final Rectangle2D.Double newArea = new Rectangle2D.Double();
        newArea.setFrameFromDiagonal(
                oldArea.getCenterX() - halfNewFovRad, oldArea.getCenterY() - halfNewFovRad,
                oldArea.getCenterX() + halfNewFovRad, oldArea.getCenterY() + halfNewFovRad);

        return newArea;
    }

    /** computes sizes and coords for the new area.
    @param fitsImage required.
    @param newArea
    @return Box containing sizes and coords.
     */
    private static Rectangle computeNewSizesIfNewArea(final FitsImage fitsImage, final Rectangle2D.Double newArea) {
        final int nbRows = fitsImage.getNbRows();
        final int nbCols = fitsImage.getNbCols();

        // area reference :
        final Rectangle2D.Double areaRef = fitsImage.getArea();

        if (logger.isDebugEnabled()) {
            logger.debug("image area     = {}", newArea);
            logger.debug("image area REF = {}", areaRef);
            logger.debug("image REF      = [{} x {}]", nbCols, nbRows);
        }

        final double pixRatioX = ((double) nbCols) / areaRef.getWidth();
        final double pixRatioY = ((double) nbRows) / areaRef.getHeight();

        // note : floor/ceil to be sure to have at least 1x1 pixel image
        int x = (int) Math.floor(pixRatioX * (newArea.getX() - areaRef.getX()));
        int y = (int) Math.floor(pixRatioY * (newArea.getY() - areaRef.getY()));
        int w = (int) Math.ceil(pixRatioX * newArea.getWidth());
        int h = (int) Math.ceil(pixRatioY * newArea.getHeight());

        // check bounds:
        w = checkBounds(w, 1, MAX_IMAGE_SIZE);
        h = checkBounds(h, 1, MAX_IMAGE_SIZE);

        // Keep it square and even to avoid any black border (not present originally):
        final int newSize = Math.max(w, h);
        w = h = (newSize % 2 != 0) ? newSize + 1 : newSize;

        if (logger.isDebugEnabled()) {
            logger.debug("new image [{}, {} - {}, {}]", new Object[]{x, y, w, h});
        }

        return new Rectangle(x, y, w, h);
    }

    private static void changeViewportImage(final FitsImage fitsImage, final Rectangle2D.Double newArea) {
        if (fitsImage != null) {

            final int nbRows = fitsImage.getNbRows();
            final int nbCols = fitsImage.getNbCols();

            final Rectangle sizes = computeNewSizesIfNewArea(fitsImage, newArea);
            final int x = sizes.x, y = sizes.y, w = sizes.width, h = sizes.height;

            final float[][] data = fitsImage.getData();

            final float[][] newData = new float[w][h];

            final int sx0 = Math.max(0, x);
            final int swx = Math.min(x + w, nbCols) - sx0;
            if (logger.isDebugEnabled()) {
                logger.debug("sx [{} - {}]", sx0, swx);
            }

            final int sy0 = Math.max(0, y);
            final int sy1 = Math.min(y + h, nbRows);
            if (logger.isDebugEnabled()) {
                logger.debug("sy [{} - {}]", sy0, sy1);
            }

            final int offX = (x < 0) ? -x : 0;
            final int offY = (y < 0) ? -y : -sy0;
            if (logger.isDebugEnabled()) {
                logger.debug("off [{} - {}]", offX, offY);
            }

            for (int j = sy0; j < sy1; j++) {
                System.arraycopy(data[j], sx0, newData[j + offY], offX, swx);
            }

            updateFitsImage(fitsImage, newData);

            // update ref pixel:
            fitsImage.setPixRefCol(fitsImage.getPixRefCol() - x);
            fitsImage.setPixRefRow(fitsImage.getPixRefRow() - y);

            logger.debug("changeViewportImage: updated image: {}", fitsImage);
        }
    }

    /** compute number of pixels with a new pixel sizes in mas.
     * field of view is kept constant and number of pixels is modified:
     *      FOV    =    oldNbPixels * oldPixelSizeMas
     *      FOV * (newPixelSizeMas / oldPixelSizeMas)    =    oldNbPixels * oldPixelSizeMas * (newPixelSizeMas / oldPixelSizeMas)
     *      FOV    =    oldNbPixels * (newPixelSizeMas / oldPixelSizeMas)   *   oldPixelSizeMas * (newPixelSizeMas / oldPixelSizeMas)
     *      FOV    =    oldNbPixels * (newPixelSizeMas / oldPixelSizeMas)   *   newPixelSizeMas
     *      newNbPixels    =    oldNbPixels * (newPixelSizeMas / oldPixelSizeMas)
     * image must be a square.
    @param oldNbPixels number of pixels.
    @param oldPixelSizeMas size of a pixel in mas.
    @param newPixelSizeMas new size of a pixel in mas.
    @return new nb of pixel. always even, never zero.
     */
    public static int computeNbPixelsIfChangePixelSize(final int oldNbPixels, final double oldPixelSizeMas, final double newPixelSizeMas) {

        final int newNbPixels = (int) Math.floor(oldNbPixels * (oldPixelSizeMas / newPixelSizeMas));

        final int evenNewNbPixels = (newNbPixels % 2 == 0) ? newNbPixels : (newNbPixels + 1);

        return evenNewNbPixels;
    }

    public static void resampleImages(final FitsImageHDU hdu, final int newSize, final Filter filter) throws IllegalArgumentException {
        if (newSize < 1) {
            throw new IllegalStateException("Invalid size: " + newSize);
        }
        if (hdu != null && hdu.hasImages()) {
            for (FitsImage fitsImage : hdu.getFitsImages()) {
                // First modify image:
                resampleImage(fitsImage, newSize, filter);

                // note: fits image instance can be modified by image preparation:
                // can throw IllegalArgumentException if image has invalid keyword(s) / data:
                FitsImageUtils.prepareImage(fitsImage);
            }
        }
    }

    private static void resampleImage(final FitsImage fitsImage, final int newSize, final Filter filter) {
        if (fitsImage != null) {
            final float[][] data = fitsImage.getData();
            final int nbRows = fitsImage.getNbRows();
            final int nbCols = fitsImage.getNbCols();

            final double[][] imgDbl = ArrayConvert.toDoubles(nbRows, nbCols, data);
            if (logger.isDebugEnabled()) {
                logger.debug("resampleImage: input [{} x {}] dest [{} x {}]", nbCols, nbRows, newSize, newSize);
            }

            final long start = System.nanoTime();

            final double[][] imgResized = Resampler.filter(imgDbl, new double[newSize][newSize], filter, true); // only positive flux

            logger.info("resampleImage: duration = {} ms.", 1e-6d * (System.nanoTime() - start));

            updateFitsImage(fitsImage, ArrayConvert.toFloats(newSize, newSize, imgResized));

            // Preserve origin:
            // origin = - inc * ( ref - 1 )
            final double oriCol = -(fitsImage.getPixRefCol() - 1.0) * fitsImage.getIncCol();
            final double oriRow = -(fitsImage.getPixRefRow() - 1.0) * fitsImage.getIncRow();

            // update increments:
            fitsImage.setSignedIncCol((fitsImage.getSignedIncCol() * nbCols) / newSize);
            fitsImage.setSignedIncRow((fitsImage.getSignedIncRow() * nbRows) / newSize);

            // update ref pixel:
            // -orign = inc * ref - inc
            // ref = (-origin + inc) / inc = - origin / inc + 1
            fitsImage.setPixRefCol(-oriCol / fitsImage.getIncCol() + 1.0);
            fitsImage.setPixRefRow(-oriRow / fitsImage.getIncRow() + 1.0);

            logger.debug("resampleImage: updated image: {}", fitsImage);
        }
    }

    public static void rescaleImages(final FitsImageHDU hdu, final double incCol, final double incRow) throws IllegalArgumentException {
        if (Double.isNaN(incCol) || NumberUtils.equals(incCol, 0.0, MAS_EPSILON)) {
            throw new IllegalStateException("Invalid column increment: " + incCol);
        }
        if (Double.isNaN(incRow) || NumberUtils.equals(incRow, 0.0, MAS_EPSILON)) {
            throw new IllegalStateException("Invalid row increment: " + incRow);
        }
        if (hdu != null && hdu.hasImages()) {
            for (FitsImage fitsImage : hdu.getFitsImages()) {
                // First modify image:
                rescaleImage(fitsImage, incCol, incRow);

                // note: fits image instance can be modified by image preparation:
                // can throw IllegalArgumentException if image has invalid keyword(s) / data:
                FitsImageUtils.prepareImage(fitsImage);
            }
        }
    }

    public static void rescaleImage(final FitsImage fitsImage, final double incCol, final double incRow) {
        if (fitsImage != null) {
            // update increments:
            fitsImage.setSignedIncCol(fitsImage.isIncColPositive() ? incCol : -incCol);
            fitsImage.setSignedIncRow(fitsImage.isIncRowPositive() ? incRow : -incRow);

            // update initial image FOV:
            fitsImage.defineOrigMaxAngle();

            logger.debug("rescaleImage: updated image: {}", fitsImage);
        }
    }

    /**
     * Return the value or the closest bound
     * @param value value to check
     * @param min minimum value
     * @param max maximum value
     * @return value or the closest bound
     */
    public static int checkBounds(final int value, final int min, final int max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }
}
