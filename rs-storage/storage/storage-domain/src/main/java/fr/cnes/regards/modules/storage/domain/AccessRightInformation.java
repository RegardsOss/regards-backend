/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.domain;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Random;

import javax.validation.constraints.NotNull;

public class AccessRightInformation implements Serializable {

    @NotNull
    private String publisherDID;

    @NotNull
    private String publisherID;

    @NotNull
    private String dataRights;

    private OffsetDateTime publicReleaseDate;

    public AccessRightInformation(String pPublisherDID, String pPublisherID, String pDataRights) {
        super();
        publisherDID = pPublisherDID;
        publisherID = pPublisherID;
        dataRights = pDataRights;
    }

    /**
     *
     */
    public AccessRightInformation() {
    }

    public String getPublisherDID() {
        return publisherDID;
    }

    public void setPublisherDID(String pPublisherDID) {
        publisherDID = pPublisherDID;
    }

    public String getPublisherID() {
        return publisherID;
    }

    public void setPublisherID(String pPublisherID) {
        publisherID = pPublisherID;
    }

    public String getDataRights() {
        return dataRights;
    }

    public void setDataRights(String pDataRights) {
        dataRights = pDataRights;
    }

    public OffsetDateTime getPublicReleaseDate() {
        return publicReleaseDate;
    }

    public void setPublicReleaseDate(OffsetDateTime pPublicReleaseDate) {
        publicReleaseDate = pPublicReleaseDate;
    }

    public AccessRightInformation generate() {
        Random random = new Random();
        int maxStringLength = 20;
        dataRights = String.valueOf(generateRandomString(random, maxStringLength));
        publisherDID = String.valueOf(generateRandomString(random, maxStringLength));
        publisherID = String.valueOf(generateRandomString(random, maxStringLength));
        return this;
    }

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
        } else
            if (!dataRights.equals(other.dataRights)) {
                return false;
            }
        if (publicReleaseDate == null) {
            if (other.publicReleaseDate != null) {
                return false;
            }
        } else
            if (!publicReleaseDate.equals(other.publicReleaseDate)) {
                return false;
            }
        if (publisherDID == null) {
            if (other.publisherDID != null) {
                return false;
            }
        } else
            if (!publisherDID.equals(other.publisherDID)) {
                return false;
            }
        if (publisherID == null) {
            if (other.publisherID != null) {
                return false;
            }
        } else
            if (!publisherID.equals(other.publisherID)) {
                return false;
            }
        return true;
    }

}
