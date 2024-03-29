
package fr.jmmc.oiexplorer.core.model.oi;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * 
 *                 This type describes an oidata file
 *             
 * 
 * <p>Java class for OIDataFile complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="OIDataFile"&gt;
 *   &lt;complexContent&gt;
 *     &lt;extension base="{http://www.jmmc.fr/oiexplorer-base/0.1}Identifiable"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="file" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *         &lt;element name="checksum" type="{http://www.w3.org/2001/XMLSchema}long"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/extension&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "OIDataFile", propOrder = {
    "file",
    "checksum"
})
public class OIDataFile
    extends Identifiable
{

    @XmlElement(required = true)
    protected String file;
    protected long checksum;

    /**
     * Gets the value of the file property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getFile() {
        return file;
    }

    /**
     * Sets the value of the file property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setFile(String value) {
        this.file = value;
    }

    /**
     * Gets the value of the checksum property.
     * 
     */
    public long getChecksum() {
        return checksum;
    }

    /**
     * Sets the value of the checksum property.
     * 
     */
    public void setChecksum(long value) {
        this.checksum = value;
    }
    
//--simple--preserve
    /**
     * Perform a deep-copy EXCEPT Identifiable attributes of the given other instance into this instance
     * 
     * Note: to be overriden in child class to perform deep-copy of class fields
     * 
     * @param other other instance
     */
    @Override
    public void copyValues(final fr.jmmc.oiexplorer.core.model.OIBase other) {
        final OIDataFile dataFile = (OIDataFile) other;

        // copy file & chesksum:
        this.file = dataFile.file;
        this.checksum = dataFile.checksum;
        
        // ensure temporary data is null:
        this.oiFitsFile = null;
    }

    @Override
    public boolean equals(final Object obj, final boolean useVersion) {
        if (!super.equals(obj, useVersion)) { // Identifiable
            return false;
        }
        final OIDataFile other = (OIDataFile) obj;
        if (!fr.jmmc.jmcs.util.ObjectUtils.areEquals(this.file, other.file)) {
            return false;
        }
        return (this.checksum == other.checksum);
    }

    /** loaded oiFitsFile structure (read only) */
    @javax.xml.bind.annotation.XmlTransient
    private fr.jmmc.oitools.model.OIFitsFile oiFitsFile = null;

    /**
     * Return the loaded oiFitsFile structure
     * @return loaded oiFitsFile structure
     */
    public final fr.jmmc.oitools.model.OIFitsFile getOIFitsFile() {
        return this.oiFitsFile;
    }

    /**
     * Define the loaded oiFitsFile structure
     * @param oiFitsFile loaded oiFitsFile structure
     */
    public final void setOIFitsFile(final fr.jmmc.oitools.model.OIFitsFile oiFitsFile) {
        this.oiFitsFile = oiFitsFile;
    }

    /**
     * toString() implementation using string builder
     * @param sb string builder to append to
     * @param full true to get complete information; false to get main information (shorter)
     */
    @Override
    public void toString(final StringBuilder sb, final boolean full) {
        super.toString(sb, full); // Identifiable
        sb.append(", file='").append(this.file).append('\'');
        if (full) {
            sb.append(", checksum=").append(this.checksum);
        }
        sb.append('}');
    }
//--simple--preserve

}
