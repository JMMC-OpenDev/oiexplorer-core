
package fr.jmmc.oiexplorer.core.model.oi;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * 
 *                 This type describes a subset definition
 *             
 * 
 * <p>Java class for SubsetDefinition complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="SubsetDefinition"&gt;
 *   &lt;complexContent&gt;
 *     &lt;extension base="{http://www.jmmc.fr/oiexplorer-base/0.1}Identifiable"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="filter" type="{http://www.jmmc.fr/oiexplorer-data-collection/0.1}SubsetFilter" maxOccurs="unbounded"/&gt;
 *         &lt;element name="genericFilter" type="{http://www.jmmc.fr/oiexplorer-data-collection/0.1}GenericFilter" maxOccurs="unbounded" minOccurs="0"/&gt;
 *         &lt;element name="show" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/extension&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SubsetDefinition", propOrder = {
    "filters",
    "genericFilters",
    "show"
})
public class SubsetDefinition
    extends Identifiable
{

    @XmlElement(name = "filter", required = true)
    protected List<SubsetFilter> filters;
    @XmlElement(name = "genericFilter")
    protected List<GenericFilter> genericFilters;
    protected Boolean show;

    /**
     * Gets the value of the filters property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the filters property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getFilters().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link SubsetFilter }
     * 
     * 
     */
    public List<SubsetFilter> getFilters() {
        if (filters == null) {
            filters = new ArrayList<SubsetFilter>();
        }
        return this.filters;
    }

    /**
     * Gets the value of the genericFilters property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the genericFilters property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getGenericFilters().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link GenericFilter }
     * 
     * 
     */
    public List<GenericFilter> getGenericFilters() {
        if (genericFilters == null) {
            genericFilters = new ArrayList<GenericFilter>();
        }
        return this.genericFilters;
    }

    /**
     * Gets the value of the show property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isShow() {
        return show;
    }

    /**
     * Sets the value of the show property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setShow(Boolean value) {
        this.show = value;
    }
    
//--simple--preserve
    public boolean isHideFilteredData() {
        return ((isShow() != null) && !(isShow().booleanValue()));
    }

    public void setHideFilteredData(final boolean value) {
        setShow(value ? Boolean.FALSE : null);
    }

    /**
     * Return the first SubsetFilter (or create a new instance)
     * @return SubsetFilter instance
     */
    public SubsetFilter getFilter() {
        final SubsetFilter filter;
        if (filters == null || filters.isEmpty()) {
            filter = new SubsetFilter();
            getFilters().add(filter);
        } else {
            filter = getFilters().get(0);
        }
        return filter;
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
        final SubsetDefinition subset = (SubsetDefinition) other;

        // copy show:
        this.show = subset.show;

        // deep copy filters:
        this.filters = fr.jmmc.jmcs.util.ObjectUtils.deepCopyList(subset.filters);
        this.genericFilters = fr.jmmc.jmcs.util.ObjectUtils.deepCopyList(subset.genericFilters);
        
        // ensure temporary data is null:
        this.oiFitsSubset = null;
        this.selectorResult = null;
    }

    @Override
    public boolean equals(final Object obj, final boolean useVersion) {
        if (!super.equals(obj, useVersion)) { // Identifiable
            return false;
        }
        final SubsetDefinition other = (SubsetDefinition) obj;
        if (!fr.jmmc.jmcs.util.ObjectUtils.areEquals(this.filters, other.filters)) {
            return false;
        }
        if (!Identifiable.areEquals(this.genericFilters, other.genericFilters, useVersion)) {
            return false;
        }
        return this.isHideFilteredData() == other.isHideFilteredData();
    }

    /**
     * subset oiFitsFile structure (read only)
     */
    @javax.xml.bind.annotation.XmlTransient
    private fr.jmmc.oitools.model.OIFitsFile oiFitsSubset = null;

    /**
     * Return the subset oiFitsFile structure
     * @return subset oiFitsFile structure
     */
    public final fr.jmmc.oitools.model.OIFitsFile getOIFitsSubset() {
        if (this.oiFitsSubset == null && this.selectorResult != null) {
            // lazy generate fake OIFitsFile structure (for compatibility)
            final fr.jmmc.oitools.model.OIFitsFile oiFitsFile;

            // create a new fake OIFitsFile:
            oiFitsFile = new fr.jmmc.oitools.model.OIFitsFile(fr.jmmc.oitools.meta.OIFitsStandard.VERSION_1);

            // add all tables:
            for (fr.jmmc.oitools.model.OIData oiData : this.selectorResult.getSortedOIDatas()) {
                oiFitsFile.addOiTable(oiData);
            }
            this.oiFitsSubset = oiFitsFile;

            if (logger.isDebugEnabled()) {
                logger.debug("getOIFitsSubset(): {}", this.oiFitsSubset);
            }
        }
        return this.oiFitsSubset;
    }

    /** SelectorResult containing the result of filters */
    @javax.xml.bind.annotation.XmlTransient
    private fr.jmmc.oitools.processing.SelectorResult selectorResult = null;

    /**
     * @return the selectorResult
     */
    public fr.jmmc.oitools.processing.SelectorResult getSelectorResult() {
        return selectorResult;
    }

    /**
     * @param selectorResult the selectorResult to set
     */
    public void setSelectorResult(fr.jmmc.oitools.processing.SelectorResult selectorResult) {
        this.selectorResult = selectorResult;
        this.oiFitsSubset = null; // reset
    }

    /**
     * toString() implementation using string builder
     * @param sb string builder to append to
     * @param full true to get complete information; false to get main information (shorter)
     */
    @Override
    public void toString(final StringBuilder sb, final boolean full) {
        super.toString(sb, full); // Identifiable

        if (full) {
            sb.append(", show=").append(this.show);
            sb.append(", filters=");
            fr.jmmc.jmcs.util.ObjectUtils.toString(sb, full, this.filters);
            sb.append(", genericFilters=");
            fr.jmmc.jmcs.util.ObjectUtils.toString(sb, full, this.genericFilters);
        }
        sb.append('}');
    }

    /**
     * Check bad references and update OIDataFile references in subset filter's tables
     * @param mapIdOiDataFiles Map<ID, OIDataFile> index
     */
    protected void checkReferences(final java.util.Map<String, OIDataFile> mapIdOiDataFiles) {
        if (filters != null) {
            for (SubsetFilter filter : filters) {
                SubsetFilter.updateOIDataFileReferences(filter.tables, mapIdOiDataFiles);
            }
        }
        if ((this.selectorResult != null) && !this.selectorResult.isEmpty()) {
            this.selectorResult.getDataModel().refresh();
        }
    }

//--simple--preserve

}
