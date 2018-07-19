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
package fr.cnes.regards.framework.oais;

import java.net.URL;
import java.util.Set;

import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotBlank;
import org.hibernate.validator.constraints.NotEmpty;

import fr.cnes.regards.framework.oais.urn.DataType;
import fr.cnes.regards.framework.oais.validator.ValidOAISDataObject;
import fr.cnes.regards.framework.utils.file.validation.HandledMessageDigestAlgorithm;

/**
 *
 * OAIS data object
 *
 * @author Sylvain Vissiere-Guerinet
 * @author Marc Sordi
 *
 */
@ValidOAISDataObject
public class OAISDataObject {

    /**
     * The regards data type
     */
    @NotNull(message = "REGARDS data type is required to qualify the related data file")
    private DataType regardsDataType;

    /**
     * Required flag to indicate a data file is just a reference (whether physical file is managed internally or not)
     */
    @NotNull(message = "Reference flag is required")
    private Boolean reference;

    /**
     * The url
     */
    @NotEmpty(message = "Data file URL is required")
    private Set<URL> urls;

    /**
     * The file name
     */
    @NotBlank(message = "Filename is required")
    private String filename;

    /**
     * The checksum algorithm (<b>required</b> if data object is not a reference)
     */
    @HandledMessageDigestAlgorithm
    private String algorithm;

    /**
     * The checksum (<b>required</b> if data object is not a reference)
     */
    private String checksum;

    /**
     * The file size
     */
    private Long fileSize;

    /**
     * @return the file name
     */
    public String getFilename() {
        return filename;
    }

    /**
     * Set the file name
     * @param filename
     */
    public void setFilename(String filename) {
        this.filename = filename;
    }

    /**
     * @return the regards data type
     */
    public DataType getRegardsDataType() {
        return regardsDataType;
    }

    /**
     * Set the regards data type
     * @param regardsDataType
     */
    public void setRegardsDataType(DataType regardsDataType) {
        this.regardsDataType = regardsDataType;
    }

    /**
     * @return the url
     */
    public Set<URL> getUrls() {
        return urls;
    }

    /**
     * Set the url
     * @param urls
     */
    public void setUrls(Set<URL> urls) {
        this.urls = urls;
    }

    /**
     * @return the checksum algorithm
     */
    public String getAlgorithm() {
        return algorithm;
    }

    /**
     * Set the checksum algorithm
     * @param algorithm
     */
    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    /**
     * @return the checksum
     */
    public String getChecksum() {
        return checksum;
    }

    /**
     * Set the checksum
     * @param checksum
     */
    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    /**
     * @return the file size
     */
    public Long getFileSize() {
        return fileSize;
    }

    /**
     * Set the file size
     * @param fileSize
     */
    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public Boolean getReference() {
        return reference;
    }

    public Boolean isReference() {
        return reference;
    }

    public void setReference(Boolean reference) {
        this.reference = reference;
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
        OAISDataObject other = (OAISDataObject) obj;
        if (algorithm == null) {
            if (other.algorithm != null) {
                return false;
            }
        } else if (!algorithm.equals(other.algorithm)) {
            return false;
        }
        if (checksum == null) {
            if (other.checksum != null) {
                return false;
            }
        } else if (!checksum.equals(other.checksum)) {
            return false;
        }
        if (fileSize == null) {
            if (other.fileSize != null) {
                return false;
            }
        } else if (!fileSize.equals(other.fileSize)) {
            return false;
        }
        if (filename == null) {
            if (other.filename != null) {
                return false;
            }
        } else if (!filename.equals(other.filename)) {
            return false;
        }
        if (reference == null) {
            if (other.reference != null) {
                return false;
            }
        } else if (!reference.equals(other.reference)) {
            return false;
        }
        if (regardsDataType != other.regardsDataType) {
            return false;
        }
        if (urls == null) {
            if (other.urls != null) {
                return false;
            }
        } else if (!urls.equals(other.urls)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + ((algorithm == null) ? 0 : algorithm.hashCode());
        result = (prime * result) + ((checksum == null) ? 0 : checksum.hashCode());
        result = (prime * result) + ((fileSize == null) ? 0 : fileSize.hashCode());
        result = (prime * result) + ((filename == null) ? 0 : filename.hashCode());
        result = (prime * result) + ((reference == null) ? 0 : reference.hashCode());
        result = (prime * result) + ((regardsDataType == null) ? 0 : regardsDataType.hashCode());
        result = (prime * result) + ((urls == null) ? 0 : urls.hashCode());
        return result;
    }
}
