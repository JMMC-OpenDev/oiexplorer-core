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

    /** default resampling filter */
    public final static Filter DEFAULT_FILTER = Filter.FILTER_LANCZOS2;

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

    /** 
     * Call prepareImage for each FitsImage of the given FitsImageHDU. 
     * @param fitsImageHDU (can be null but then the function do nothing)
     * @throws IllegalArgumentException 
     */
    public static void prepareImages(final FitsImageHDU fitsImageHDU) throws IllegalArgumentException {
        if (fitsImageHDU != null) {
            for (FitsImage fitsImage : fitsImageHDU.getFitsImages()) {
                // note: fits image instance can be modified by image preparation:
                // can throw IllegalArgumentException if image has invalid keyword(s) / data:
                FitsImageUtils.prepareImage(fitsImage);
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

    public static boolean changeViewportImages(final FitsImageHDU hdu, final Rectangle2D.Double newArea) throws IllegalArgumentException {
        if (newArea == null || newArea.isEmpty()) {
            throw new IllegalStateException("Invalid area: " + newArea);
        }
        boolean changed = false;

        if (hdu != null && hdu.hasImages()) {
            for (FitsImage fitsImage : hdu.getFitsImages()) {
                // First modify image:
                changed |= changeViewportImage(fitsImage, newArea);
            }
        }
        return changed;
    }

    public static final class ImageSize {

        public double fov; // unit MAS.
        public int nbPixels;
        public double inc; // unit MAS.
    }

    /** 
     * Foresee the ImageSize resulting from modifyImage().
     * @param fitsImage required.
     * @param newFov required. unit MAS.
     * @param newInc required. unit MAS.
     * @return the fov, the nb of pixels, and the inc that would result from applying modifyImage(). null if cannot foresee for some reason.
     */
    public static ImageSize foreseeModifyImage(final FitsImage fitsImage, double newFov, double newInc) {

        if ((newFov <= 0.0) || (newInc <= 0.0) || (newFov <= newInc)) {
            logger.debug("Could not modify image because wrong values for newFov ({}) or newInc ({})",
                    newFov, newInc);
            return null;
        }
        // VIEWPORT
        // computing the new area that we get with new fov
        Rectangle2D.Double newArea = computeNewArea(fitsImage.getArea(), newFov);
        if (newArea == null) {
            return null;
        }

        final double inc = FitsUnit.ANGLE_RAD.convert(fitsImage.getIncCol(), FitsUnit.ANGLE_MILLI_ARCSEC);
        final ImageSize newImageSize = new ImageSize();

        // get the width in number of pixels that we get with the new area
        newImageSize.nbPixels = computeAreaRectangle(fitsImage, newArea).width;
        // adjusting fov
        newImageSize.fov = inc * newImageSize.nbPixels;

        // RESAMPLE
        newImageSize.nbPixels = computeNewNbPixels(newImageSize.nbPixels, inc, newInc);

        // adjusting inc
        newImageSize.inc = newImageSize.fov / newImageSize.nbPixels;

        return newImageSize;
    }

    /** 
     * Change viewPort with newFov, then resample with newInc (with a computation of nb of pixel).
     * @param fitsImage required. will be modified. data field will be copied before modified.
     * @param newFov required. unit MAS.
     * @param newInc required. unit MAS.
     * @return true of successfully modified FitsImage. false if something wrong happened.
     */
    public static boolean modifyImage(final FitsImage fitsImage, double newFov, double newInc) {
        // change viewport
        final Rectangle2D.Double newArea = computeNewArea(fitsImage.getArea(), newFov);
        if (newArea == null) {
            logger.info("Could not modifyImage because could not compute new area.");
            return false;
        }
        boolean changed = changeViewportImages(fitsImage.getFitsImageHDU(), newArea);

        // resample
        final int nbPixels = fitsImage.getNbCols();
        final double inc = FitsUnit.ANGLE_RAD.convert(fitsImage.getIncCol(), FitsUnit.ANGLE_MILLI_ARCSEC);

        final int newNbPixels = computeNewNbPixels(nbPixels, inc, newInc);

        if (newNbPixels != nbPixels) {
            resampleImages(fitsImage.getFitsImageHDU(), newNbPixels, DEFAULT_FILTER);
            changed = true;
        }
        return changed;
    }

    /** 
     * Compute the Area resulting from a fov change.
     * it starts from center of the current fov, and extend to the new fov.
     * @param area required.
     * @param newFov unit MAS. should be positive.
     * @return new area in RAD. null if params were wrong.
     */
    public static Rectangle2D.Double computeNewArea(final Rectangle2D.Double area, final double newFov) {
        if ((area == null) || (newFov <= 0.0d)) {
            return null;
        }

        // convert to radians because it is the unit used in areas
        final double newFovRad = FitsUnit.ANGLE_MILLI_ARCSEC.convert(newFov, FitsUnit.ANGLE_RAD);

        final double centerX = area.getCenterX();
        final double centerY = area.getCenterY();
        final double halfNewFovRad = newFovRad / 2.0d;

        // Starting from center, define top-left corner and bottom-right corner
        final Rectangle2D.Double newArea = new Rectangle2D.Double();
        newArea.setFrameFromDiagonal(centerX - halfNewFovRad, centerY - halfNewFovRad,
                centerX + halfNewFovRad, centerY + halfNewFovRad);

        return newArea;
    }

    /** 
     * Compute rectangle of the image [x,y,w,h] in number of pixels, resulting from a change of area.
     * @param fitsImage the original image.
     * @param newArea the new area for the image.
     * @return Rectangle containing sizes in number of pixels.
     */
    private static Rectangle computeAreaRectangle(final FitsImage fitsImage, final Rectangle2D.Double newArea) {
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

    private static boolean changeViewportImage(final FitsImage fitsImage, final Rectangle2D.Double newArea) {
        if (fitsImage != null) {
            final int nbRows = fitsImage.getNbRows();
            final int nbCols = fitsImage.getNbCols();

            final Rectangle sizes = computeAreaRectangle(fitsImage, newArea);
            final int x = sizes.x, y = sizes.y, w = sizes.width, h = sizes.height;

            if ((x != 0) || (y != 0) || (w != nbCols) || (h != nbRows)) {
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
                return true;
            }
        }
        return false;
    }

    /** 
     * Compute number of pixels, resulting from a change of inc.
     * field of view is kept constant and number of pixels is modified.
     * FOV = nbPixels * inc
     * image must be a square.
     * @param nbPixels number of pixels.
     * @param inc size of a pixel in mas.
     * @param newInc new size of a pixel in mas.
     * @return new nb of pixel. always even, never zero.
     */
    public static int computeNewNbPixels(final int nbPixels, final double inc, final double newInc) {
        // minimal difference to actually modify the number of pixels.
        // this permits the GUI to use only 3 digits precision numbers
        if (Math.abs(newInc - inc) < 1e-3) {
            return nbPixels;
        }

        int newSize = (int) Math.ceil(nbPixels * (inc / newInc));

        // check bounds:
        newSize = checkBounds(newSize, 1, MAX_IMAGE_SIZE);

        // Keep it square and even to avoid any black border (not present originally):
        return (newSize % 2 != 0) ? newSize + 1 : newSize;
    }

    public static void resampleImages(final FitsImageHDU hdu, final int newSize, final Filter filter) throws IllegalArgumentException {
        if (newSize < 1) {
            throw new IllegalStateException("Invalid size: " + newSize);
        }
        if (hdu != null && hdu.hasImages()) {
            for (FitsImage fitsImage : hdu.getFitsImages()) {
                // First modify image:
                resampleImage(fitsImage, newSize, filter);
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
