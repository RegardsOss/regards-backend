/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.fileaccess.dto;

import org.springframework.util.MimeType;

/**
 * Dto represents meta information about a file referenced in storage catalog.
 *
 * @author Sébastien Binda
 */
public class FileReferenceMetaInfoDto {

    /**
     * File checksum
     */
    private String checksum;

    /*
     * Checksum algorithm
     */
    private String algorithm;

    /**
     * File name
     */
    private String fileName;

    /**
     * File size in bytes
     */
    private Long fileSize;

    /**
     * Height of the file (only for image files) in pixels
     */
    private Integer height;

    /**
     * Width of the file (only for image files) in pixels
     */
    private Integer width;

    /**
     * {@link MimeType} of the file
     */
    private String mimeType;

    /**
     * File type
     */
    private String type;

    public FileReferenceMetaInfoDto() {

    }

    public FileReferenceMetaInfoDto(String checksum,
                                    String algorithm,
                                    String fileName,
                                    Long fileSize,
                                    Integer height,
                                    Integer width,
                                    String mimeType,
                                    String type) {
        this.checksum = checksum;
        this.algorithm = algorithm;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.height = height;
        this.width = width;
        this.mimeType = mimeType;
        this.type = type;
    }

    public String getChecksum() {
        return checksum;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    //fileName is not final as the real fileName might be calculated later in the process
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }

    //fileSize is not final as the real fileSize might be calculated later in the process
    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public Long getFileSize() {
        return fileSize;
    }

    //height is not final as the real height might be calculated later in the process
    public void setHeight(Integer height) {
        this.height = height;
    }

    public Integer getHeight() {
        return height;
    }

    public Integer getWidth() {
        return width;
    }

    //width is not final as the real width might be calculated later in the process
    public void setWidth(Integer width) {
        this.width = width;
    }

    public String getMimeType() {
        return mimeType;
    }

    public String getType() {
        return type;
    }

    @Override
    public String toString() {
        return "FileReferenceMetaInfoDTO{"
               + "checksum='"
               + checksum
               + '\''
               + ", algorithm='"
               + algorithm
               + '\''
               + ", fileName='"
               + fileName
               + '\''
               + ", fileSize="
               + fileSize
               + ", height="
               + height
               + ", width="
               + width
               + ", mimeType="
               + mimeType
               + ", type='"
               + type
               + '\''
               + '}';
    }
}
