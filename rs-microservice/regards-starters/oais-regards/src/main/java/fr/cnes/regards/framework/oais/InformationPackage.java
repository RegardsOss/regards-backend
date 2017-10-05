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

import javax.validation.constraints.NotNull;

import java.util.Map;
import java.util.Set;

import org.hibernate.validator.constraints.NotEmpty;

import com.google.common.collect.Sets;

/**
 *
 * OAIS Information object
 *
 * @author Sylvain Vissiere-Guerinet
 * @author Marc Sordi
 *
 */
public class InformationPackage {

    @NotEmpty
    private Set<ContentInformation> contentInformations;

    @NotNull
    private PreservationDescriptionInformation pdi;

    private Map<String, Object> descriptiveInformation;

    public Set<ContentInformation> getContentInformations() {
        if(contentInformations == null) {
            contentInformations = Sets.newHashSet();
        }
        return contentInformations;
    }

    public PreservationDescriptionInformation getPdi() {
        return pdi;
    }

    public void setPdi(PreservationDescriptionInformation pPdi) {
        pdi = pPdi;
    }

    public Map<String, Object> getDescriptiveInformation() {
        return descriptiveInformation;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + ((contentInformations == null) ? 0 : contentInformations.hashCode());
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
        InformationPackage other = (InformationPackage) obj;
        if (contentInformations == null) {
            if (other.contentInformations != null) {
                return false;
            }
        } else if (!contentInformations.equals(other.contentInformations)) {
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
