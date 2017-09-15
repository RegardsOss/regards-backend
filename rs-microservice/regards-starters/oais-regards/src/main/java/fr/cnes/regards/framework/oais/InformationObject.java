/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.net.MalformedURLException;
import java.security.NoSuchAlgorithmException;

import javax.validation.constraints.NotNull;

/**
 *
 * OAIS Information object
 *
 * @author Sylvain Vissiere-Guerinet
 * @author Marc Sordi
 *
 */
public class InformationObject {

    @NotNull
    private ContentInformation contentInformation;

    @NotNull
    private PreservationDescriptionInformation pdi;

    public ContentInformation getContentInformation() {
        return contentInformation;
    }

    public void setContentInformation(ContentInformation pContentInformation) {
        contentInformation = pContentInformation;
    }

    public PreservationDescriptionInformation getPdi() {
        return pdi;
    }

    public void setPdi(PreservationDescriptionInformation pPdi) {
        pdi = pPdi;
    }

    // FIXME to remove
    @Deprecated
    public InformationObject generateRandomInformationObject() throws NoSuchAlgorithmException, MalformedURLException {
        contentInformation = new ContentInformation().generate();
        pdi = new PreservationDescriptionInformation().generate();
        return this;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + ((contentInformation == null) ? 0 : contentInformation.hashCode());
        result = (prime * result) + ((pdi == null) ? 0 : pdi.hashCode());
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
        InformationObject other = (InformationObject) obj;
        if (contentInformation == null) {
            if (other.contentInformation != null) {
                return false;
            }
        } else if (!contentInformation.equals(other.contentInformation)) {
            return false;
        }
        if (pdi == null) {
            if (other.pdi != null) {
                return false;
            }
        } else if (!pdi.equals(other.pdi)) {
            return false;
        }
        return true;
    }

}
