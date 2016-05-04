
package org.endeavourhealth.messaging.configuration.schema.engineConfiguration;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for engineConfiguration complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="engineConfiguration">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="cassandra" type="{}cassandra"/>
 *         &lt;element name="logging" type="{}logging"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "engineConfiguration", propOrder = {
    "cassandra",
    "logging"
})
public class EngineConfiguration {

    @XmlElement(required = true)
    protected Cassandra cassandra;
    @XmlElement(required = true)
    protected Logging logging;

    /**
     * Gets the value of the cassandra property.
     * 
     * @return
     *     possible object is
     *     {@link Cassandra }
     *     
     */
    public Cassandra getCassandra() {
        return cassandra;
    }

    /**
     * Sets the value of the cassandra property.
     * 
     * @param value
     *     allowed object is
     *     {@link Cassandra }
     *     
     */
    public void setCassandra(Cassandra value) {
        this.cassandra = value;
    }

    /**
     * Gets the value of the logging property.
     * 
     * @return
     *     possible object is
     *     {@link Logging }
     *     
     */
    public Logging getLogging() {
        return logging;
    }

    /**
     * Sets the value of the logging property.
     * 
     * @param value
     *     allowed object is
     *     {@link Logging }
     *     
     */
    public void setLogging(Logging value) {
        this.logging = value;
    }

}
