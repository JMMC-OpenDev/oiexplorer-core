
package fr.jmmc.oiexplorer.core.model.oi;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import fr.jmmc.oiexplorer.core.model.OIBase;
import fr.jmmc.oiexplorer.core.model.plot.PlotDefinition;


/**
 * 
 *                 This type describes a common identifiable object (id, name)
 *             
 * 
 * <p>Java class for Identifiable complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="Identifiable"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="id" type="{http://www.w3.org/2001/XMLSchema}ID"/&gt;
 *         &lt;element name="name" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *         &lt;element name="description" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Identifiable", namespace = "http://www.jmmc.fr/oiexplorer-base/0.1", propOrder = {
    "id",
    "name",
    "description"
})
@XmlSeeAlso({
    OIDataFile.class,
    SubsetDefinition.class,
    PlotDefinition.class,
    GenericFilter.class,
    View.class
})
public class Identifiable
    extends OIBase
{

    @XmlElement(required = true)
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlID
    @XmlSchemaType(name = "ID")
    protected String id;
    @XmlElement(required = true)
    protected String name;
    protected String description;

    /**
     * Gets the value of the id property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the value of the id property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setId(String value) {
        this.id = value;
    }

    /**
     * Gets the value of the name property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the value of the name property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setName(String value) {
        this.name = value;
    }

    /**
     * Gets the value of the description property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the value of the description property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDescription(String value) {
        this.description = value;
    }
    
//--simple--preserve
    /** instance version to track effective changes (read only) */
    @javax.xml.bind.annotation.XmlTransient
    private int version = 0;

    /**
     * Return the instance version
     * @return instance version
     */
    public final int getVersion() {
        return version;
    }

    /**
     * PUBLIC: Increment the version
     */
    public final void incVersion() {
        this.version++;
    }

    /**
     * Return an new identifiable version (id,version,hash) comparable later
     * @return new identifiable version
     */
    public final fr.jmmc.oiexplorer.core.model.IdentifiableVersion getIdentifiableVersion() {
        return new fr.jmmc.oiexplorer.core.model.IdentifiableVersion(this.id, this.version, getHash());
    }
    
    /**
    * @return this object's hash code
    */
    public int getHash() {
        return System.identityHashCode(this);
    }

    /**
     * Perform a deep-copy of the given other instance into this instance
     * 
     * @see OIBase#clone() 
     * 
     * @param other other instance
     */
    @Override
    public final void copy(final OIBase other) {
        final Identifiable identifiable = (Identifiable) other;

        // skip id to avoid overriding identifier !
        /* this.id = identifiable.getId(); */
        this.version = identifiable.getVersion();

        // copy name, description, version:
        this.name = identifiable.getName();
        this.description = identifiable.getDescription();

        // Copy values (overriden by child classes):
        copyValues(other); // values
    }

    /**
     * Perform a deep-copy EXCEPT Identifiable attributes of the given other instance into this instance
     * 
     * Note: to be overriden in child class to perform deep-copy of class fields
     * 
     * @param other other instance
     */
    public void copyValues(final OIBase other) {
        // nothing to copy as wanted
    }

    @Override
    public final boolean equals(final Object obj) {
        return equals(obj, true);
    }

    public boolean equals(final Object obj, final boolean useVersion) {
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
        final Identifiable other = (Identifiable) obj;
        if (!fr.jmmc.jmcs.util.ObjectUtils.areEquals(this.id, other.getId())) {
            return false;
        }
        if (!fr.jmmc.jmcs.util.ObjectUtils.areEquals(this.name, other.getName())) {
            return false;
        }
        if (!fr.jmmc.jmcs.util.ObjectUtils.areEquals(this.description, other.getDescription())) {
            return false;
        }
        return !(useVersion && (this.version != other.getVersion()));
    }

    /**
     * toString() implementation using string builder
     * @param sb string builder to append to
     * @param full true to get complete information; false to get main information (shorter)
     */
    @Override
    public void toString(final StringBuilder sb, final boolean full) {
        super.toString(sb, full); // OIBase
        sb.append("{id=").append(this.id);
        sb.append(",name=").append(this.name);
        if (this.version > 1) {
            sb.append(", version=").append(this.version);
        }
        if (full) {
            if (this.description != null) {
                sb.append(", description='").append(this.description).append('\'');
            }
        }
        // put '}' in child classes
    }

    /* Generic Identifiable helper methods */
    /**
     * Clone the given identifiable instance
     * @param source identifiable instance to clone
     * @param <K> identifiable class type
     * @return clone instance or null if the given instance is null
     */
    @SuppressWarnings("unchecked")
    public static <K extends Identifiable> K clone(final K source) {
        if (source == null) {
            return null;
        }
        return (K) source.clone();
    }

    /**
     * Copy the given identifiable instance values (source) into the given identifiable instance (dest)
     * @param source identifiable instance to get its values
     * @param dest identifiable instance to copy into
     * @param <K> identifiable class type
     */
    public static <K extends Identifiable> void copy(final K source, final K dest) {
        if (source != null) {
            throw new IllegalStateException("undefined source object");
        }
        dest.copyValues(source);
    }

    /**
     * Return the identifiers present in the given list of identifiable instances 
     * @param list list of identifiable instances
     * @param <K> identifiable class type
     * @return list of identifiers
     */
    public static <K extends Identifiable> java.util.List<String> getIds(final java.util.List<K> list) {
        final java.util.ArrayList<String> ids = new java.util.ArrayList<String>(list.size());
        for (K identifiable : list) {
            ids.add(identifiable.getId());
        }
        return ids;
    }

    /**
     * Return the identifiable instance corresponding to the given identifier in the given list of identifiable instances 
     * @param id identifiable identifier
     * @param list list of identifiable instances
     * @param <K> identifiable class type
     * @return identifiable instance or null if the identifier was not found
     */
    public static <K extends Identifiable> K getIdentifiable(final String id, final java.util.List<K> list) {
        if (id != null) {
            for (K identifiable : list) {
                if (id.equals(identifiable.getId())) {
                    return identifiable;
                }
            }
        }
        return null;
    }

    /**
     * Return true if this identifiable identifier is present in the given list of identifiable instances 
     * @param id identifiable identifier
     * @param list list of identifiable instances
     * @param <K> identifiable class type
     * @return true if this identifiable identifier is present; false otherwise
     */
    public static <K extends Identifiable> boolean hasIdentifiable(final String id, final java.util.List<K> list) {
        return getIdentifiable(id, list) != null;
    }

    /**
     * Add the given identifiable instance into the given list of identifiable instances if its identifier is not already present
     * @param identifiable identifiable instance to add
     * @param list list of identifiable instances
     * @param <K> identifiable class type
     * @return true if the given identifiable instance was added; false otherwise
     */
    public static <K extends Identifiable> boolean addIdentifiable(final K identifiable, final java.util.List<K> list) {
        if ((identifiable != null) && (identifiable.getId() != null) && getIdentifiable(identifiable.getId(), list) == null) {
            // replace previous ??
            list.add(identifiable);
            return true;
        }
        return false;
    }

    /**
     * Remove the identifiable instance from the given list of identifiable instances corresponding to its identifier
     * @param identifiable identifiable instance to remove by its identifier
     * @param list list of identifiable instances
     * @param <K> identifiable class type
     * @return removed identifiable instance or null if the identifier was not found
     */
    public static <K extends Identifiable> K removeIdentifiable(final K identifiable, final java.util.List<K> list) {
        if (identifiable != null) {
            return removeIdentifiable(identifiable.getId(), list);
        }
        return null;
    }

    /**
     * Remove the identifiable instance from the given list of identifiable instances given its identifier
     * @param id identifiable identifier
     * @param list list of identifiable instances
     * @param <K> identifiable class type
     * @return removed identifiable instance or null if the identifier was not found
     */
    public static <K extends Identifiable> K removeIdentifiable(final String id, final java.util.List<K> list) {
        if (id != null) {
            final K previous = getIdentifiable(id, list);

            if (previous != null) {
                list.remove(previous);
            }

            return previous;
        }
        return null;
    }

    /**
     * Utility method for <code>equals()</code> methods.
     *
     * @param o1 one object
     * @param o2 another object
     * @param useVersion true to compare version; false otherwise
     * @param <K> identifiable class type
     *
     * @return <code>true</code> if they're both <code>null</code> or both equal
     */
    public static <K extends Identifiable> boolean areEquals(final K o1, final K o2, final boolean useVersion) {
        return (o1 == o2) || ((o1 != null) && o1.equals(o2, useVersion));
    }

    public static <K extends Identifiable> boolean areEquals(final java.util.List<K> list1, final java.util.List<K> list2,
                                                             final boolean useVersion) {
        if (list1 != null) {
            if (list2 == null) {
                return false;
            }
            final int s = list1.size();
            boolean equal;
            if (equal = (s == list2.size())) {
                for (int i = 0; i < s; i++) {
                    if (!areEquals(list1.get(i), list2.get(i), useVersion)) {
                        equal = false;
                        break;
                    }
                }
            }
            return equal;
        }
        return (list2 == null);
    }

    public static <K extends Identifiable> boolean areIdEquals(final K o1, final K o2) {
        return (o1 == o2) || ((o1 != null) && o1.getId().equals((o2 != null) ? o2.getId() : null));
    }
    
//--simple--preserve

}
