/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.core.test.report.xml;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * List of requirements
 *
 * @author msordi
 *
 */
@XmlRootElement(name = "requirements")
public class XmlRequirements {

    /**
     * List of requirements
     */
    private List<XmlRequirement> requirements_;

    /**
     * @return the requirements
     */
    public List<XmlRequirement> getRequirements() {
        return requirements_;
    }

    /**
     * @param pRequirements
     *            the requirements to set
     */
    @XmlElement(name = "requirement")
    public void setRequirements(List<XmlRequirement> pRequirements) {
        requirements_ = pRequirements;
    }

    /**
     * Add a requirement
     *
     * @param pRequirement
     *            requirement to add
     */
    public void addRequirement(XmlRequirement pRequirement) {
        if (requirements_ == null) {
            requirements_ = new ArrayList<>();
        }
        requirements_.add(pRequirement);
    }

}
