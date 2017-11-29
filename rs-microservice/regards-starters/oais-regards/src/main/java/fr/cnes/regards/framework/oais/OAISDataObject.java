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

import java.net.URL;

import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotBlank;

import fr.cnes.regards.framework.oais.urn.DataType;
import fr.cnes.regards.framework.utils.file.validation.HandledMessageDigestAlgorithm;

/**
 *
 * OAIS data object
 *
 * @author Sylvain Vissiere-Guerinet
 * @author Marc Sordi
 *
 */
public class OAISDataObject {

    /**
     * The regards data type
     */
    @NotNull(message = "REGARDS data type is required to qualify the related data file")
    private DataType regardsDataType;

    /**
     * The url
     */
    @NotNull(message = "Data file URL is required")
    private URL url;

    /**
     * The file name
     */
    private String filename;

    /**
     * The checksum algorithm
     */
    @NotBlank(message = "Data file checksum algorithm is required")
    @HandledMessageDigestAlgorithm
    private String algorithm;

    /**
     * The checksum
     */
    @NotBlank(message = "Data file checksum is required")
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
    public URL getUrl() {
        return url;
    }

    /**
     * Set the url
     * @param url
     */
    public void setUrl(URL url) {
        this.url = url;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if ((o == null) || (getClass() != o.getClass())) {
            return false;
        }

        OAISDataObject that = (OAISDataObject) o;

        if (regardsDataType != that.regardsDataType) {
            return false;
        }
        if (url != null ? !url.equals(that.url) : that.url != null) {
            return false;
        }
        if (algorithm != null ? !algorithm.equals(that.algorithm) : that.algorithm != null) {
            return false;
        }
        return checksum != null ? checksum.equals(that.checksum) : that.checksum == null;
    }

    @Override
    public int hashCode() {
        int result = regardsDataType != null ? regardsDataType.hashCode() : 0;
        result = (31 * result) + (url != null ? url.hashCode() : 0);
        result = (31 * result) + (algorithm != null ? algorithm.hashCode() : 0);
        result = (31 * result) + (checksum != null ? checksum.hashCode() : 0);
        return result;
    }
}
