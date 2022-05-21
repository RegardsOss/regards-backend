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
package fr.cnes.regards.modules.storage.domain.dto;

import fr.cnes.regards.modules.storage.domain.database.FileReferenceMetaInfo;
import org.springframework.util.MimeType;

/**
 * DTO represents meta information about a file referenced in storage catalog.
 *
 * @author Sébastien Binda
 */
public class FileReferenceMetaInfoDTO {

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
    private MimeType mimeType;

    /**
     * File type
     */
    private String type;

    public static FileReferenceMetaInfoDTO build(String checksum,
                                                 String algorithm,
                                                 String fileName,
                                                 Long fileSize,
                                                 Integer height,
                                                 Integer width,
                                                 MimeType mimeType,
                                                 String type) {
        FileReferenceMetaInfoDTO dto = new FileReferenceMetaInfoDTO();
        dto.checksum = checksum;
        dto.algorithm = algorithm;
        dto.fileName = fileName;
        dto.fileSize = fileSize;
        dto.height = height;
        dto.width = width;
        dto.mimeType = mimeType;
        dto.type = type;
        return dto;
    }

    public static FileReferenceMetaInfoDTO build(FileReferenceMetaInfo fileReferenceMetaInfo) {
        FileReferenceMetaInfoDTO dto = new FileReferenceMetaInfoDTO();
        dto.checksum = fileReferenceMetaInfo.getChecksum();
        dto.algorithm = fileReferenceMetaInfo.getAlgorithm();
        dto.fileName = fileReferenceMetaInfo.getFileName();
        dto.fileSize = fileReferenceMetaInfo.getFileSize();
        dto.height = fileReferenceMetaInfo.getHeight();
        dto.width = fileReferenceMetaInfo.getWidth();
        dto.mimeType = fileReferenceMetaInfo.getMimeType();
        dto.type = fileReferenceMetaInfo.getType();
        return dto;
    }

    public String getChecksum() {
        return checksum;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public String getFileName() {
        return fileName;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public Integer getHeight() {
        return height;
    }

    public Integer getWidth() {
        return width;
    }

    public MimeType getMimeType() {
        return mimeType;
    }

    public String getType() {
        return type;
    }

}
