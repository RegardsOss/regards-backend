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

import org.hibernate.validator.constraints.NotBlank;

import fr.cnes.regards.framework.file.utils.validation.HandledMessageDigestAlgorithm;

/**
 *
 * OAIS Fixity information object
 *
 * @author Sylvain Vissiere-Guerinet
 * @author Marc Sordi
 *
 */
public class FixityInformation {

    @NotNull
    @HandledMessageDigestAlgorithm
    private String algorithm;

    @NotBlank
    private String checksum;

    private Long fileSize;

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
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + ((algorithm == null) ? 0 : algorithm.hashCode());
        result = (prime * result) + ((checksum == null) ? 0 : checksum.hashCode());
        result = (prime * result) + ((fileSize == null) ? 0 : fileSize.hashCode());
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
        FixityInformation other = (FixityInformation) obj;
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
        return true;
    }

}
