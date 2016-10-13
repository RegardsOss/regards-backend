/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.test.report.xml;

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
    private List<XmlRequirement> requirements;

    public List<XmlRequirement> getRequirements() {
        return requirements;
    }

    @XmlElement(name = "requirement")
    public void setRequirements(List<XmlRequirement> pRequirements) {
        requirements = pRequirements;
    }

    public void addRequirement(XmlRequirement pRequirement) {
        if (requirements == null) {
            requirements = new ArrayList<>();
        }
        requirements.add(pRequirement);
    }

}
