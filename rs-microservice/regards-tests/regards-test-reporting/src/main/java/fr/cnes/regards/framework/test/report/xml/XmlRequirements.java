/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.framework.test.report.xml;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

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
