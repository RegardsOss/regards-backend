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

    @NotNull
    private DataType regardsDataType;

    @NotNull
    private URL url;

    private String filename;

    @NotNull
    @HandledMessageDigestAlgorithm
    private String algorithm;

    @NotBlank
    private String checksum;

    private Long fileSize;

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public DataType getRegardsDataType() {
        return regardsDataType;
    }

    public void setRegardsDataType(DataType regardsDataType) {
        this.regardsDataType = regardsDataType;
    }

    public URL getUrl() {
        return url;
    }

    public void setUrl(URL url) {
        this.url = url;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    public Long getFileSize() {
        return fileSize;
    }

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
