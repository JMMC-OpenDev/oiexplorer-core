
package fr.jmmc.oiexplorer.core.model.oi;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import fr.jmmc.oiexplorer.core.model.plot.Range;


/**
 * 
 *                 This type describes a subset filter
 *             
 * 
 * <p>Java class for GenericFilter complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="GenericFilter"&gt;
 *   &lt;complexContent&gt;
 *     &lt;extension base="{http://www.jmmc.fr/oiexplorer-base/0.1}Identifiable"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="columnName" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *         &lt;element name="dataType" type="{http://www.jmmc.fr/oiexplorer-data-collection/0.1}DataType"/&gt;
 *         &lt;element name="acceptedValues" type="{http://www.w3.org/2001/XMLSchema}string" maxOccurs="unbounded" minOccurs="0"/&gt;
 *         &lt;element name="acceptedRanges" type="{http://www.jmmc.fr/oiexplorer-core-plot-definition/0.1}Range" maxOccurs="unbounded" minOccurs="0"/&gt;
 *         &lt;element name="enabled" type="{http://www.w3.org/2001/XMLSchema}boolean"/&gt;
 *         &lt;element name="not" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/extension&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "GenericFilter", propOrder = {
    "columnName",
    "dataType",
    "acceptedValues",
    "acceptedRanges",
    "enabled",
    "not"
})
public class GenericFilter
    extends Identifiable
{

    @XmlElement(required = true)
    protected String columnName;
    @XmlElement(required = true)
    @XmlSchemaType(name = "string")
    protected DataType dataType;
    protected List<String> acceptedValues;
    protected List<Range> acceptedRanges;
    protected boolean enabled;
    protected Boolean not;

    /**
     * Gets the value of the columnName property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getColumnName() {
        return columnName;
    }

    /**
     * Sets the value of the columnName property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setColumnName(String value) {
        this.columnName = value;
    }

    /**
     * Gets the value of the dataType property.
     * 
     * @return
     *     possible object is
     *     {@link DataType }
     *     
     */
    public DataType getDataType() {
        return dataType;
    }

    /**
     * Sets the value of the dataType property.
     * 
     * @param value
     *     allowed object is
     *     {@link DataType }
     *     
     */
    public void setDataType(DataType value) {
        this.dataType = value;
    }

    /**
     * Gets the value of the acceptedValues property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the acceptedValues property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getAcceptedValues().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link String }
     * 
     * 
     */
    public List<String> getAcceptedValues() {
        if (acceptedValues == null) {
            acceptedValues = new ArrayList<String>();
        }
        return this.acceptedValues;
    }

    /**
     * Gets the value of the acceptedRanges property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the acceptedRanges property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getAcceptedRanges().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Range }
     * 
     * 
     */
    public List<Range> getAcceptedRanges() {
        if (acceptedRanges == null) {
            acceptedRanges = new ArrayList<Range>();
        }
        return this.acceptedRanges;
    }

    /**
     * Gets the value of the enabled property.
     * 
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Sets the value of the enabled property.
     * 
     */
    public void setEnabled(boolean value) {
        this.enabled = value;
    }

    /**
     * Gets the value of the not property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isNot() {
        return not;
    }

    /**
     * Sets the value of the not property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setNot(Boolean value) {
        this.not = value;
    }
    
//--simple--preserve
    public boolean isInclusive() {
        return isNot() == null || !(isNot().booleanValue());
    }

    /**
     * Perform a deep-copy EXCEPT Identifiable attributes of the given other instance into this instance
     * 
     * Note: to be overriden in child class to perform deep-copy of class fields
     * 
     * @param other other instance
     */
    @Override
    public void copyValues(final fr.jmmc.oiexplorer.core.model.OIBase other) {
        final GenericFilter filter = (GenericFilter) other;

        // copy columnName, dataType, enabled:
        this.columnName = filter.getColumnName();
        this.dataType = filter.getDataType();
        this.enabled = filter.isEnabled();
        this.not = filter.isNot();

        // copy acceptedValues, acceptedRanges:
        this.acceptedValues = fr.jmmc.jmcs.util.ObjectUtils.copyList(filter.acceptedValues);
        this.acceptedRanges = fr.jmmc.jmcs.util.ObjectUtils.deepCopyList(filter.acceptedRanges);
    }

    @Override
    public boolean equals(final Object obj) {
        if (!super.equals(obj)) { // Identifiable
            return false;
        }
        final GenericFilter other = (GenericFilter) obj;
        if (!fr.jmmc.jmcs.util.ObjectUtils.areEquals(this.columnName, other.getColumnName())) {
            return false;
        }
        if (this.dataType != other.dataType) {
            return false;
        }
        if (!fr.jmmc.jmcs.util.ObjectUtils.areEquals(this.acceptedValues, other.acceptedValues)) {
            return false;
        }
        if (!fr.jmmc.jmcs.util.ObjectUtils.areEquals(this.acceptedRanges, other.acceptedRanges)) {
            return false;
        }
        if (this.enabled != other.enabled) {
            return false;
        }
        return this.isInclusive() == other.isInclusive();
    }

    /**
     * toString() implementation using string builder
     *
     * @param sb string builder to append to
     * @param full true to get complete information; false to get main information (shorter)
     */
    @Override
    public void toString(final StringBuilder sb, final boolean full) {
        super.toString(sb, full);

        if (full) {
            sb.append(", columnName=").append(columnName);
            sb.append(", dataType=").append(dataType);
            sb.append(", acceptedValues=");
            fr.jmmc.jmcs.util.ObjectUtils.toString(sb, full, acceptedValues);
            sb.append(", acceptedRanges=");
            fr.jmmc.jmcs.util.ObjectUtils.toString(sb, full, acceptedRanges);
            sb.append(", enabled=").append(enabled);
            sb.append(", not=").append(not);
        }

        sb.append(" }");
    }
//--simple--preserve

}
