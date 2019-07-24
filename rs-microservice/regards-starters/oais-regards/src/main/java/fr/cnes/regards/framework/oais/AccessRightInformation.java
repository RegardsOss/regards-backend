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

import javax.validation.constraints.NotNull;
import java.time.OffsetDateTime;

/**
 * OAIS Access Right Information object
 * @author Sylvain Vissiere-Guerinet
 * @author Marc Sordi
 */
public class AccessRightInformation {

    private String licence;

    @NotNull
    private String dataRights;

    private OffsetDateTime publicReleaseDate;

    public String getDataRights() {
        return dataRights;
    }

    public void setDataRights(String dataRights) {
        this.dataRights = dataRights;
    }

    public OffsetDateTime getPublicReleaseDate() {
        return publicReleaseDate;
    }

    public void setPublicReleaseDate(OffsetDateTime publicReleaseDate) {
        this.publicReleaseDate = publicReleaseDate;
    }

    public String getLicence() {
        return licence;
    }

    public void setLicence(String licence) {
        this.licence = licence;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + ((dataRights == null) ? 0 : dataRights.hashCode());
        result = (prime * result) + ((publicReleaseDate == null) ? 0 : publicReleaseDate.hashCode());
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
        AccessRightInformation other = (AccessRightInformation) obj;
        if (dataRights == null) {
            if (other.dataRights != null) {
                return false;
            }
        } else if (!dataRights.equals(other.dataRights)) {
            return false;
        }
        if (publicReleaseDate == null) {
            return other.publicReleaseDate == null;
        } else
            return publicReleaseDate.equals(other.publicReleaseDate);
    }

}
