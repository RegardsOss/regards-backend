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

import java.time.OffsetDateTime;
import java.util.Random;

import javax.validation.constraints.NotNull;

/**
 *
 * OAIS Access Right Information object
 *
 * @author Sylvain Vissiere-Guerinet
 * @author Marc Sordi
 *
 */
public class AccessRightInformation {

    @NotNull
    private String publisherDID;

    @NotNull
    private String publisherID;

    @NotNull
    private String dataRights;

    private OffsetDateTime publicReleaseDate;

    public String getPublisherDID() {
        return publisherDID;
    }

    public void setPublisherDID(String publisherDID) {
        this.publisherDID = publisherDID;
    }

    public String getPublisherID() {
        return publisherID;
    }

    public void setPublisherID(String publisherID) {
        this.publisherID = publisherID;
    }

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

    // TODO remove
    @Deprecated
    public AccessRightInformation generate() {
        Random random = new Random();
        int maxStringLength = 20;
        dataRights = String.valueOf(generateRandomString(random, maxStringLength));
        publisherDID = String.valueOf(generateRandomString(random, maxStringLength));
        publisherID = String.valueOf(generateRandomString(random, maxStringLength));
        return this;
    }

    // TODO remove
    @Deprecated
    private char[] generateRandomString(Random random, int maxStringLength) {
        String possibleLetters = "abcdefghijklmnopqrstuvwxyz0123456789ABCDEFGHIJKLMNOPQRSTUVWYXZ";
        int stringSize = random.nextInt(maxStringLength);
        char[] string = new char[stringSize];
        for (int j = 0; j < stringSize; j++) {
            string[j] = possibleLetters.charAt(random.nextInt(possibleLetters.length()));
        }
        return string;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + ((dataRights == null) ? 0 : dataRights.hashCode());
        result = (prime * result) + ((publicReleaseDate == null) ? 0 : publicReleaseDate.hashCode());
        result = (prime * result) + ((publisherDID == null) ? 0 : publisherDID.hashCode());
        result = (prime * result) + ((publisherID == null) ? 0 : publisherID.hashCode());
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
            if (other.publicReleaseDate != null) {
                return false;
            }
        } else if (!publicReleaseDate.equals(other.publicReleaseDate)) {
            return false;
        }
        if (publisherDID == null) {
            if (other.publisherDID != null) {
                return false;
            }
        } else if (!publisherDID.equals(other.publisherDID)) {
            return false;
        }
        if (publisherID == null) {
            if (other.publisherID != null) {
                return false;
            }
        } else if (!publisherID.equals(other.publisherID)) {
            return false;
        }
        return true;
    }

}
