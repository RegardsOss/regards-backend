/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.storage.domain.database;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Embeddable;

import org.springframework.util.MimeType;

import fr.cnes.regards.framework.jpa.converter.MimeTypeConverter;

/**
 * Database definition of meta information on a file.
 *
 * @author Sébastien Binda
 */
@Embeddable
public class FileReferenceMetaInfo {

    /**
     * length used as the checksum column definition. Why 128? it allows to use sha-512. That should limit issues with
     * checksum length for a few years
     */
    public static final int CHECKSUM_MAX_LENGTH = 128;

    /**
     * File checksum
     */
    @Column(length = CHECKSUM_MAX_LENGTH, nullable = false)
    private String checksum;

    /**
     * Checksum algorithm
     */
    @Column(length = 16, nullable = false)
    private String algorithm;

    /**
     * Stored file reference name
     */
    @Column(length = 256, nullable = false)
    private String fileName;

    /**
     * Stored file reference size in bytes.
     */
    @Column
    private Long fileSize;

    /**
     * Height of image file references.
     */
    @Column
    private Integer height;

    /**
     * Width of image file references.
     */
    @Column
    private Integer width;

    @Column(nullable = false, name = "mime_type")
    @Convert(converter = MimeTypeConverter.class)
    private MimeType mimeType;

    @Column(length = 256)
    private String type;

    public FileReferenceMetaInfo() {
        super();
    }

    public FileReferenceMetaInfo(String checksum, String algorithm, String fileName, Long fileSize, MimeType mimeType) {
        super();
        this.checksum = checksum;
        this.algorithm = algorithm;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.mimeType = mimeType;
    }

    /**
     * @return the checksum
     */
    public String getChecksum() {
        return checksum;
    }

    /**
     * @param checksum the checksum to set
     */
    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    /**
     * @return the algorithm
     */
    public String getAlgorithm() {
        return algorithm;
    }

    /**
     * @param algorithm the algorithm to set
     */
    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    /**
     * @return the fileName
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * @param fileName the fileName to set
     */
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    /**
     * @return the fileSize
     */
    public Long getFileSize() {
        return fileSize;
    }

    /**
     * @param fileSize the fileSize to set
     */
    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    /**
     * @return the height
     */
    public Integer getHeight() {
        return height;
    }

    /**
     * @param height the height to set
     */
    public void setHeight(Integer height) {
        this.height = height;
    }

    /**
     * @return the width
     */
    public Integer getWidth() {
        return width;
    }

    /**
     * @param width the width to set
     */
    public void setWidth(Integer width) {
        this.width = width;
    }

    /**
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * @return the mimeType
     */
    public MimeType getMimeType() {
        return mimeType;
    }

    /**
     * @param mimeType the mimeType to set
     */
    public void setMimeType(MimeType mimeType) {
        this.mimeType = mimeType;
    }

    public FileReferenceMetaInfo withType(String type) {
        this.type = type;
        return this;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + ((algorithm == null) ? 0 : algorithm.hashCode());
        result = (prime * result) + ((checksum == null) ? 0 : checksum.hashCode());
        result = (prime * result) + ((fileName == null) ? 0 : fileName.hashCode());
        result = (prime * result) + ((fileSize == null) ? 0 : fileSize.hashCode());
        result = (prime * result) + ((height == null) ? 0 : height.hashCode());
        result = (prime * result) + ((mimeType == null) ? 0 : mimeType.hashCode());
        result = (prime * result) + ((type == null) ? 0 : type.hashCode());
        result = (prime * result) + ((width == null) ? 0 : width.hashCode());
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
        FileReferenceMetaInfo other = (FileReferenceMetaInfo) obj;
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
        if (fileName == null) {
            if (other.fileName != null) {
                return false;
            }
        } else if (!fileName.equals(other.fileName)) {
            return false;
        }
        if (fileSize == null) {
            if (other.fileSize != null) {
                return false;
            }
        } else if (!fileSize.equals(other.fileSize)) {
            return false;
        }
        if (height == null) {
            if (other.height != null) {
                return false;
            }
        } else if (!height.equals(other.height)) {
            return false;
        }
        if (mimeType == null) {
            if (other.mimeType != null) {
                return false;
            }
        } else if (!mimeType.equals(other.mimeType)) {
            return false;
        }
        if (type == null) {
            if (other.type != null) {
                return false;
            }
        } else if (!type.equals(other.type)) {
            return false;
        }
        if (width == null) {
            if (other.width != null) {
                return false;
            }
        } else if (!width.equals(other.width)) {
            return false;
        }
        return true;
    }

}
