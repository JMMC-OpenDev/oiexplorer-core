/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.oitools.image;

import fr.nom.tam.fits.BasicHDU;
import fr.nom.tam.fits.Fits;
import fr.nom.tam.fits.FitsException;
import fr.nom.tam.fits.FitsFactory;
import fr.nom.tam.fits.Header;
import fr.nom.tam.fits.HeaderCard;
import fr.nom.tam.fits.ImageData;
import fr.nom.tam.fits.ImageHDU;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

/**
 * This stateless class loads is an Fits image and cube into the FitsImageFile structure
 * 
 * From Aspro1:
 *  -------About the FITS file Format ------------------------------
 * First, the FITS file should describe a flux distribution on
 * sky, so the 2 first axes are in offset in RADIANS on the sky. The
 * following is an example of a typical header:
 * 
 * NAXIS   =                    2 /2 minimum!
 * NAXIS1  =                  512 /size 1st axis (for example)
 * NAXIS2  =                  512 /size 2nd axis
 * CRVAL1  =  0.0000000000000E+00 / center is at 0
 * CRPIX1  =  0.2560000000000E+03 / reference pixel is 256 in Alpha
 * CDELT1  = -0.4848136811095E-10 / increment is 0.1 millisec (radians), 
 * / and 'astronomy oriented' (i.e. 
 * / RA decreases with pixel number)
 * CRVAL2  =  0.0000000000000E+00 / center is at 0
 * CRPIX2  =  0.2560000000000E+03 / reference pixel is 256 in Delta
 * CDELT2  =  0.4848136811095E-10 / increment is 0.1 millisec.
 * 
 * The position of the "keyword = value / comment" fields is FIXED by
 * the fits norm. In doubt, see http://www.cv.nrao.edu/fits/aah2901.pdf
 * 
 * Axes increments map pixel images to RA and DEC sky coordinates, which
 * are positive to the West and North (and position angles are counted
 * West of North).
 * 
 * For single (monochromatic) images (NAXIS = 2), ASPRO assume that this
 * image is observed at the current observing wavelength, usually the
 * mean wavelength of the current instrument setup. Aspro has a support
 * for polychromatic images, as FITS cubes, with a few more keywords
 * (see below), in which case the used wavelengths will be those defined
 * in the FITS file, not those of the current instrument/interferometer.
 * 
 * The file may be a datacube (N images at different wavelengths) in
 * which case the 3rd axis must be sampled evenly. In the absence of
 * further keywords (see below), it will be assumed that th 3rd axis
 * is in MICRONS as in:
 * 
 * NAXIS   =                    3 /datacube
 * NAXIS1  =                  512 /size 1st axis (for example)
 * NAXIS2  =                  512 /size 2nd axis
 * NAXIS3  =                   32 /size 2nd axis
 * CRVAL1  =  0                   / center is at 0 RA offset on sky
 * CRPIX1  =  256                 / reference pixel is 256 in Alpha
 * CDELT1  = -0.4848136811095E-10 / increment is -0.1 millisec (radians), 
 * CRVAL2  =  0                   / center is at 0 DEC offset on sky
 * CRPIX2  =  256                 / reference pixel is 256 in Delta
 * CDELT2  =  0.4848136811095E-10 / increment is 0.1 millisec.
 * CRPIX3  =  1                   / reference pixel(channel) is 1 on 3rd axis
 * CRVAL3  =  2.2                 / 2.2 microns for this pix/channel
 * CDELT3  =  0.01                / microns channel width
 * 
 * However, the additional presence of CTYPE3 can affect
 * the 3rd axis definition:
 * CTYPE3  = 'FREQUENCY'          / means that Cxxxx3 are in Hz
 * or
 * CTYPE3  = 'WAVELENGTH'         / means that Cxxxx3 are in Microns
 * 
 * TODO: add support for FITS cube (NAXIS3) and then adjust FitsImage structure
 * 
 * @author bourgesl
 */
public final class FitsImageLoader {
    /* constants */

    /** Logger associated to meta model classes */
    private final static java.util.logging.Logger logger = java.util.logging.Logger.getLogger(FitsImageLoader.class.getName());

    static {
        FitsFactory.setUseHierarch(true);
    }

    /**
     * Private constructor
     */
    private FitsImageLoader() {
        super();
    }

    /**
     * Load the given file and return a FitsImageFile structure
     *
     * @param absFilePath absolute File path on file system (not URL)
     * @throws FitsException if any FITS error occured
     * @throws IOException IO failure
     * @return FitsImageFile structure on success
     */
    public static FitsImageFile load(final String absFilePath) throws FitsException, IOException {
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("loading " + absFilePath);
        }

        // Check if the given file exists :
        if (!new File(absFilePath).exists()) {
            throw new IOException("File not found : " + absFilePath);
        }

        try {
            // create new Fits image structure:
            final FitsImageFile imgFitsFile = new FitsImageFile(absFilePath);

            final long start = System.nanoTime();

            // open the fits file :
            final Fits fitsFile = new Fits(absFilePath);

            // read the complete file structure :
            final BasicHDU[] hdus = fitsFile.read();

            // process all HD units :
            if (hdus != null) {
                processHDUnits(imgFitsFile, hdus);
            }

            if (logger.isLoggable(Level.INFO)) {
                logger.info("load : duration = " + 1e-6d * (System.nanoTime() - start) + " ms.");
            }

            return imgFitsFile;

        } catch (FitsException fe) {
            logger.log(Level.SEVERE, "Unable to load the file : " + absFilePath, fe);
            throw fe;
        }
    }

    /**
     * Process all Fits HD units to load Fits images (skip other HDU) into the given FitsImageFile structure
     * @param imgFitsFile FitsImageFile structure to use
     * @param hdus array of hd unit
     * @throws FitsException if any FITS error occured
     */
    private static void processHDUnits(final FitsImageFile imgFitsFile, final BasicHDU[] hdus) throws FitsException {

        final int nbHDU = hdus.length;
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("processHDUnits : number of HDU = " + nbHDU);
        }

        final List<FitsImage> fitsImages = imgFitsFile.getFitsImages();

        FitsImage image;
        BasicHDU hdu;
        ImageHDU ih;

        // start from Primary HDU
        for (int i = 0; i < nbHDU; i++) {
            hdu = hdus[i];

            if (hdu instanceof ImageHDU) {
                ih = (ImageHDU) hdu;

                image = new FitsImage();

                // define the fits image identifier:
                image.setFitsImageIdentifier(imgFitsFile.getFileName() + "#" + i);

                // load table :
                processImage(ih, image);

                // register the image :
                fitsImages.add(image);
            }
        }
    }

    /**
     * Process a given Fits image to fill the given FitsImage object with header and image data
     * @param hdu image HDU
     * @param image Fits image
     * @throws FitsException if any FITS error occured
     */
    private static void processImage(final ImageHDU hdu, final FitsImage image) throws FitsException {
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("processImage : " + image);
        }

        // get Fits header :
        processKeywords(hdu.getHeader(), image);

        processData(hdu, image);
    }

    /**
     * Process the image header to get import keywords
     * @param header image header
     * @param image Fits image
     * @throws FitsException if any FITS error occured
     */
    private static void processKeywords(final Header header, final FitsImage image) throws FitsException {
        // Note : a fits keyword has a KEY, VALUE AND COMMENT

        // Handle x-y axes dimensions:
        /*
        KEYWORD NAXIS = '2'	// Number of axes
        KEYWORD NAXIS1 = '512'	// Axis length
        KEYWORD NAXIS2 = '512'	// Axis length
         */
        final int nAxis = header.getIntValue(FitsImageConstants.KEYWORD_NAXIS, 0);
        if (nAxis < 0) {
            throw new FitsException("Negative NAXIS value " + nAxis);
        }
        if (nAxis > 999) {
            throw new FitsException("NAXIS value " + nAxis + " too large");
        }

        // note: x axis has keyword index 1:
        image.setNbCols(header.getIntValue(FitsImageConstants.KEYWORD_NAXIS1, 0));
        // note: y axis has keyword index 2:
        image.setNbRows(header.getIntValue(FitsImageConstants.KEYWORD_NAXIS2, 0));

        // Process reference pixel:
        /*
        KEYWORD CRPIX1 = '256.'	// Reference pixel
        KEYWORD CRPIX2 = '256.'	// Reference pixel
         */
        image.setPixRefCol(header.getDoubleValue(FitsImageConstants.KEYWORD_CRPIX1, 1d));
        image.setPixRefRow(header.getDoubleValue(FitsImageConstants.KEYWORD_CRPIX2, 1d));

        // Process coordinates at the reference pixel:
        // note: units are ignored
        /*
        KEYWORD CRVAL1 = '0.'	// Coordinate at reference pixel
        KEYWORD CRVAL2 = '0.'	// Coordinate at reference pixel
        
        KEYWORD CTYPE1 = ''	//  Units of coordinate
        KEYWORD CTYPE2 = ''	//  Units of coordinate
         */
        image.setValRefCol(header.getDoubleValue(FitsImageConstants.KEYWORD_CRVAL1, 0d));
        image.setValRefRow(header.getDoubleValue(FitsImageConstants.KEYWORD_CRVAL2, 0d));

        // Process increments along axes:
        /*
        KEYWORD CDELT1 = '-1.2E-10'	// Coord. incr. per pixel (original value)
        KEYWORD CDELT2 = '1.2E-10'	// Coord. incr. per pixel (original value)
         */
        image.setSignedIncCol(header.getDoubleValue(FitsImageConstants.KEYWORD_CDELT1, 1d));
        image.setSignedIncRow(header.getDoubleValue(FitsImageConstants.KEYWORD_CDELT2, 1d));

        // Process data min/max:
        /*
        KEYWORD DATAMAX = '5120.758'	// Maximum data value
        KEYWORD DATAMIN = '0.0'	// Minimum data value
         */
        // note: data min/max are later recomputed (missing / invalid values or bad precision)
        image.setDataMin(header.getDoubleValue(FitsImageConstants.KEYWORD_DATAMIN, Double.NaN));
        image.setDataMax(header.getDoubleValue(FitsImageConstants.KEYWORD_DATAMAX, Double.NaN));

        // Copy all header cards:
        final List<FitsHeaderCard> imgHeaderCards = image.getHeaderCards(header.getNumberOfCards());

        HeaderCard card;
        String key;
        for (Iterator<?> it = header.iterator(); it.hasNext();) {
            card = (HeaderCard) it.next();

            key = card.getKey();

            if ("END".equals(key)) {
                break;
            }

            imgHeaderCards.add(new FitsHeaderCard(key, card.getValue(), card.getComment()));
        }
    }

    /**
     * Process the image data and store them in the given FitsImage
     * @param hdu image HDU
     * @param image Fits image
     * @throws FitsException if any FITS error occured
     */
    private static void processData(final ImageHDU hdu, final FitsImage image) throws FitsException {

        // load the complete image:
        final ImageData fitsData = (ImageData) hdu.getData();

        // interpret also BSCALE / BZERO (BUNIT) if present
        final int bitPix = hdu.getBitPix();
        final double bZero = hdu.getBZero();
        final double bScale = hdu.getBScale();

        final int nbCols = image.getNbCols();
        final int nbRows = image.getNbRows();

        // use raw array[1D] ??
        // convert automatically:
        // look at     Object o = ArrayFuncs.newInstance(base, dims);

        final float[][] imgData = getImageData(nbRows, nbCols, bitPix, fitsData.getData(), bZero, bScale);

        image.setData(imgData);
    }

    /**
     * Convert and optionaly scale the given array2D to float[][]
     * @param rows number of rows
     * @param cols number of columns
     * @param bitpix bit per pixels
     * @param array2D input array2D to convert
     * @param bZero zero point in scaling equation
     * @param bScale linear factor in scaling equation
     * @return float[][]
     */
    private static float[][] getImageData(final int rows, final int cols, final int bitpix, final Object array2D,
                                          final double bZero, final double bScale) {

        if (array2D == null) {
            return null;
        }

        logger.info("bitPix    = " + bitpix);

        final boolean doZero = (bZero != 0d);
        final boolean doScaling = (bScale != 1d);

        logger.info("doZero    = " + doZero);
        logger.info("doScaling = " + doScaling);

        if (bitpix == BasicHDU.BITPIX_FLOAT && !(doZero || doScaling)) {
            return (float[][]) array2D;
        }

        final float[][] output = new float[rows][cols];

        // 1 - convert data to float[][]
        float[] oRow;
        switch (bitpix) {
            case BasicHDU.BITPIX_BYTE:
                final byte[][] bArray = (byte[][]) array2D;
                byte[] bRow;
                for (int i, j = 0; j < rows; j++) {
                    oRow = output[j];
                    bRow = bArray[j];
                    for (i = 0; i < cols; i++) {
                        oRow[i] = (float) (bRow[i] & 0xFF);
                    }
                }
                break;
            case BasicHDU.BITPIX_SHORT:
                final short[][] sArray = (short[][]) array2D;
                short[] sRow;
                for (int i, j = 0; j < rows; j++) {
                    oRow = output[j];
                    sRow = sArray[j];
                    for (i = 0; i < cols; i++) {
                        oRow[i] = (float) sRow[i];
                    }
                }
                break;
            case BasicHDU.BITPIX_INT:
                final int[][] iArray = (int[][]) array2D;
                int[] iRow;
                for (int i, j = 0; j < rows; j++) {
                    oRow = output[j];
                    iRow = iArray[j];
                    for (i = 0; i < cols; i++) {
                        oRow[i] = (float) iRow[i];
                    }
                }
                break;
            case BasicHDU.BITPIX_LONG:
                final long[][] lArray = (long[][]) array2D;
                long[] lRow;
                for (int i, j = 0; j < rows; j++) {
                    oRow = output[j];
                    lRow = lArray[j];
                    for (i = 0; i < cols; i++) {
                        oRow[i] = (float) lRow[i];
                    }
                }
                break;
            case BasicHDU.BITPIX_FLOAT:
                // nothing to do
                break;
            case BasicHDU.BITPIX_DOUBLE:
                final double[][] dArray = (double[][]) array2D;
                double[] dRow;
                for (int i, j = 0; j < rows; j++) {
                    oRow = output[j];
                    dRow = dArray[j];
                    for (i = 0; i < cols; i++) {
                        oRow[i] = (float) dRow[i];
                    }
                }
                break;

            default:
        }

        // 2 - scale data:
        if (doZero || doScaling) {
            for (int i, j = 0; j < rows; j++) {
                oRow = output[j];
                for (i = 0; i < cols; i++) {
                    if (doZero) {
                        oRow[i] += bZero;
                    }
                    if (doScaling) {
                        oRow[i] *= bScale;
                    }
                }
            }
        }
        return output;
    }
}