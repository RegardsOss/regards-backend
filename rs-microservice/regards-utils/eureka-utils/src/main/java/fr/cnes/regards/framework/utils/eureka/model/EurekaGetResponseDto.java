/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.utils.eureka.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Thibaud Michaudel
 **/
@XmlRootElement(name = "applications")
@XmlAccessorType(XmlAccessType.FIELD)
public class EurekaGetResponseDto {

    @XmlElement(name = "application")
    private List<EurekaApplication> applications;

    public List<EurekaApplication> getApplications() {
        if (applications == null) {
            applications = new ArrayList<>();
        }
        return applications;
    }

    public void setApplications(List<EurekaApplication> applications) {
        this.applications = applications;
    }
}
