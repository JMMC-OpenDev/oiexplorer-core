
package fr.jmmc.oiexplorer.core.model.oi;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import fr.jmmc.oiexplorer.core.model.OIBase;


/**
 * 
 *                 This type describes an OIData table unique identifier among the OIDataCollection
 *             
 * 
 * <p>Java class for TableUID complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="TableUID"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="file" type="{http://www.w3.org/2001/XMLSchema}IDREF"/&gt;
 *         &lt;element name="extName" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="extNb" type="{http://www.w3.org/2001/XMLSchema}int" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "TableUID", propOrder = {
    "file",
    "extName",
    "extNb"
})
public class TableUID
    extends OIBase
{

    @XmlElement(required = true, type = Object.class)
    @XmlIDREF
    @XmlSchemaType(name = "IDREF")
    protected OIDataFile file;
    protected String extName;
    protected Integer extNb;

    /**
     * Gets the value of the file property.
     * 
     * @return
     *     possible object is
     *     {@link Object }
     *     
     */
    public OIDataFile getFile() {
        return file;
    }

    /**
     * Sets the value of the file property.
     * 
     * @param value
     *     allowed object is
     *     {@link Object }
     *     
     */
    public void setFile(OIDataFile value) {
        this.file = value;
    }

    /**
     * Gets the value of the extName property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getExtName() {
        return extName;
    }

    /**
     * Sets the value of the extName property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setExtName(String value) {
        this.extName = value;
    }

    /**
     * Gets the value of the extNb property.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public Integer getExtNb() {
        return extNb;
    }

    /**
     * Sets the value of the extNb property.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setExtNb(Integer value) {
        this.extNb = value;
    }
    
//--simple--preserve
    /**
     * Constructor for JAXB
     */
    public TableUID() {
    }

    /**
     * Constructor
     * @param file oidata file Identifier
     */
    public TableUID(final OIDataFile file) {
        this(file, null, null);
    }

    /**
     * Constructor
     * @param file oidata file Identifier
     * @param extName oidata table name
     * @param extNb oidata table number
     */
    public TableUID(final OIDataFile file, final String extName, final Integer extNb) {
        this.file = file;
        this.extName = extName;
        this.extNb = extNb;
    }

    /**
     * Perform a deep-copy of the given other instance into this instance
     * 
     * Note: to be overriden in child class to perform deep-copy of class fields
     * @see OIBase#clone() 
     * 
     * @param other other instance
     */
    @Override
    public void copy(final fr.jmmc.oiexplorer.core.model.OIBase other) {
        final TableUID tableUID = (TableUID) other;

        // copy file, extName, extNb:
        this.file = tableUID.getFile(); // reference only
        this.extName = tableUID.getExtName();
        this.extNb = tableUID.getExtNb();
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        // identity check:
        if (this == obj) {
            return true;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final TableUID other = (TableUID) obj;
        if (!Identifiable.areIdEquals(this.file, other.file)) {
            return false;
        }
        if (!fr.jmmc.jmcs.util.ObjectUtils.areEquals(this.extName, other.extName)) {
            return false;
        }
        return fr.jmmc.jmcs.util.ObjectUtils.areEquals(this.extNb, other.extNb);
    }

    /**
     * toString() implementation using string builder
     * @param sb string builder to append to
     * @param full true to get complete information; false to get main information (shorter)
     */
    @Override
    public void toString(final StringBuilder sb, final boolean full) {
        super.toString(sb, full); // OIBase
        sb.append("{file=").append(this.file);
        if (extName != null) {
            sb.append(", extName=").append(this.extName);
            sb.append(", extNb=").append(this.extNb);
        }
        sb.append('}');
    }

    /**
     * appends a short String representation. used for logging for example
     *
     * @param sb StringBuilder to which to appends the string representation
     */
    public void appendShortString(final StringBuilder sb) {
        sb.append("(").append(extName == null ? "null-extname" : extName);
        sb.append(",").append(extNb == null ? "null-extnb" : extNb);
        sb.append(",");
        if (file == null) {
            sb.append("null-file");
        } else {
            // we want a string not longer than 40 characters, to keep it short
            final String filename = new java.io.File(file.getFile()).getName();
            final int filenameLength = filename.length();
            if (filenameLength > 40) {
                sb.append(filename.substring(0, 17)).append("...");
                sb.append(filename.substring(filenameLength - 20));
            } else {
                sb.append(filename);
            }
        }
        sb.append(")");
    }
//--simple--preserve

}
