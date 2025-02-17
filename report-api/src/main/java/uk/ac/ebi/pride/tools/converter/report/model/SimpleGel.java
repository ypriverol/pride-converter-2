package uk.ac.ebi.pride.tools.converter.report.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;
import java.io.Serializable;


/**
 * The SimpleGel element (complex) extends the GelType element to provide a concrete
 * implementation of gel, holding a reference to a gel image (if available).
 * <p/>
 * <p/>
 * <p>Java class for SimpleGel complex type.
 * <p/>
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p/>
 * <pre>
 * &lt;complexType name="SimpleGel">
 *   &lt;complexContent>
 *     &lt;extension base="{}GelType">
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SimpleGel")
public class SimpleGel
        extends Gel
        implements Serializable {

    private final static long serialVersionUID = 100L;

}
