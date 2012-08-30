
package fr.jmmc.oiexplorer.core.model.plot;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import fr.jmmc.oiexplorer.core.model.OIBase;


/**
 * 
 *                 This type describes a plot definition.
 *             
 * 
 * <p>Java class for PlotDefinition complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="PlotDefinition">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="name" type="{http://www.w3.org/2001/XMLSchema}ID"/>
 *         &lt;element name="description" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="skipFlaggedData" type="{http://www.w3.org/2001/XMLSchema}boolean"/>
 *         &lt;element name="drawLine" type="{http://www.w3.org/2001/XMLSchema}boolean"/>
 *         &lt;element name="xAxis" type="{http://www.jmmc.fr/oiexplorer-core-plot-definition/0.1}Axis"/>
 *         &lt;element name="yAxes" type="{http://www.jmmc.fr/oiexplorer-core-plot-definition/0.1}Axis" maxOccurs="unbounded"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "PlotDefinition", propOrder = {
    "name",
    "description",
    "skipFlaggedData",
    "drawLine",
    "xAxis",
    "yAxes"
})
public class PlotDefinition
    extends OIBase
{

    @XmlElement(required = true)
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlID
    @XmlSchemaType(name = "ID")
    protected String name;
    protected String description;
    protected boolean skipFlaggedData;
    protected boolean drawLine;
    @XmlElement(required = true)
    protected Axis xAxis;
    @XmlElement(required = true)
    protected List<Axis> yAxes;

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

    /**
     * Gets the value of the skipFlaggedData property.
     * 
     */
    public boolean isSkipFlaggedData() {
        return skipFlaggedData;
    }

    /**
     * Sets the value of the skipFlaggedData property.
     * 
     */
    public void setSkipFlaggedData(boolean value) {
        this.skipFlaggedData = value;
    }

    /**
     * Gets the value of the drawLine property.
     * 
     */
    public boolean isDrawLine() {
        return drawLine;
    }

    /**
     * Sets the value of the drawLine property.
     * 
     */
    public void setDrawLine(boolean value) {
        this.drawLine = value;
    }

    /**
     * Gets the value of the xAxis property.
     * 
     * @return
     *     possible object is
     *     {@link Axis }
     *     
     */
    public Axis getXAxis() {
        return xAxis;
    }

    /**
     * Sets the value of the xAxis property.
     * 
     * @param value
     *     allowed object is
     *     {@link Axis }
     *     
     */
    public void setXAxis(Axis value) {
        this.xAxis = value;
    }

    /**
     * Gets the value of the yAxes property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the yAxes property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getYAxes().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Axis }
     * 
     * 
     */
    public List<Axis> getYAxes() {
        if (yAxes == null) {
            yAxes = new ArrayList<Axis>();
        }
        return this.yAxes;
    }

}
