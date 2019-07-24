/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.oais;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * OAIS content information
 * @author Sylvain Vissiere-Guerinet
 * @author Marc Sordi
 */
public class ContentInformation {

    @Valid
    @NotNull(message = "A representation information is required in content information")
    private RepresentationInformation representationInformation;

    @NotNull(message = "A data object is required in content information")
    @Valid
    private OAISDataObject dataObject;

    public OAISDataObject getDataObject() {
        return dataObject;
    }

    public void setDataObject(OAISDataObject pDataObject) {
        dataObject = pDataObject;
    }

    public RepresentationInformation getRepresentationInformation() {
        return representationInformation;
    }

    public void setRepresentationInformation(RepresentationInformation pRepresentationInformation) {
        representationInformation = pRepresentationInformation;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + ((dataObject == null) ? 0 : dataObject.hashCode());
        result = (prime * result) + ((representationInformation == null) ? 0 : representationInformation.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ContentInformation other = (ContentInformation) obj;
        if (dataObject == null) {
            if (other.dataObject != null) {
                return false;
            }
        } else if (!dataObject.equals(other.dataObject)) {
            return false;
        }
        if (representationInformation == null) {
            return other.representationInformation == null;
        } else
            return representationInformation.equals(other.representationInformation);
    }
}
