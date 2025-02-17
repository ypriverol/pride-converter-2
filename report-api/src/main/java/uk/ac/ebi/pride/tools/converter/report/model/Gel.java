package uk.ac.ebi.pride.tools.converter.report.model;

import javax.xml.bind.annotation.*;
import java.io.Serializable;


/**
 * The GelType (abstract) element provides a basis for describing the kind of gel used for
 * this identication.
 * <p/>
 * <p/>
 * <p>Java class for GelType complex type.
 * <p/>
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p/>
 * <pre>
 * &lt;complexType name="GelType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="GelLink" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="additional" type="{}paramType" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "GelType", propOrder = {
        "gelLink",
        "additional"
})
@XmlSeeAlso({
        SimpleGel.class
})
public abstract class Gel
        implements Serializable, ReportObject {

    private final static long serialVersionUID = 100L;
    @XmlElement(name = "GelLink", required = true)
    protected String gelLink = "http://";
    protected Param additional = new Param();

    /**
     * Gets the value of the gelLink property.
     *
     * @return possible object is
     *         {@link String }
     */
    public String getGelLink() {
        return gelLink;
    }

    /**
     * Sets the value of the gelLink property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setGelLink(String value) {
        this.gelLink = value;
    }

    /**
     * Gets the value of the additional property.
     *
     * @return possible object is
     *         {@link uk.ac.ebi.pride.tools.converter.report.model.Param }
     */
    public Param getAdditional() {
        return additional;
    }

    /**
     * Sets the value of the additional property.
     *
     * @param value allowed object is
     *              {@link uk.ac.ebi.pride.tools.converter.report.model.Param }
     */
    public void setAdditional(Param value) {
        this.additional = value;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Gel");
        sb.append("{gelLink='").append(gelLink).append('\'');
        sb.append(", additional=").append(additional);
        sb.append('}');
        return sb.toString();
    }
}
