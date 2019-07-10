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
package fr.cnes.regards.modules.storagelight.domain.database;

import java.util.List;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.JoinColumn;

import org.springframework.util.MimeType;

import fr.cnes.regards.framework.jpa.converter.MimeTypeConverter;

/**
 * @author sbinda
 *
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
     * Stored file reference path
     */
    @Column(length = 512)
    private String filePath;

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

    /**
     * File type
     */
    @Column(name = "type")
    @ElementCollection
    @CollectionTable(name = "file_ref_types", joinColumns = @JoinColumn(name = "file_ref_id"))
    private List<String> types;

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
     * @return the filePath
     */
    public String getFilePath() {
        return filePath;
    }

    /**
     * @param filePath the filePath to set
     */
    public void setFilePath(String filePath) {
        this.filePath = filePath;
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
     * @return the types
     */
    public List<String> getTypes() {
        return types;
    }

    /**
     * @param types the types to set
     */
    public void setTypes(List<String> types) {
        this.types = types;
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

}
