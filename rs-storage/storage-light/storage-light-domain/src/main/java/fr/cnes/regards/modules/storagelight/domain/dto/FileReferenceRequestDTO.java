/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License; or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful;
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not; see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.modules.storagelight.domain.dto;

import org.springframework.http.MediaType;
import org.springframework.util.Assert;

import fr.cnes.regards.modules.storagelight.domain.database.FileReferenceMetaInfo;

/**
 * @author sbinda
 *
 */
public class FileReferenceRequestDTO {

    private String fileName;

    private String checksum;

    private String algorithm;

    private String mimeType;

    private Long fileSize;

    private Integer height;

    private Integer width;

    private String owner;

    private String storage;

    private String url;

    private String type;

    public String getFileName() {
        return fileName;
    }

    public String getChecksum() {
        return checksum;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public String getMimeType() {
        return mimeType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public String getOwner() {
        return owner;
    }

    public String getStorage() {
        return storage;
    }

    public String getUrl() {
        return url;
    }

    public String getType() {
        return type;
    }

    public Integer getHeight() {
        return height;
    }

    public Integer getWidth() {
        return width;
    }

    public static FileReferenceRequestDTO build(String fileName, String checksum, String algorithm, String mimeType,
            Long fileSize, String owner, String storage, String url) {

        Assert.notNull(fileName, "File name is mandatory.");
        Assert.notNull(checksum, "Checksum is mandatory.");
        Assert.notNull(algorithm, "Algorithm is mandatory.");
        Assert.notNull(mimeType, "MimeType is mandatory.");
        Assert.notNull(fileSize, "FileSize is mandatory.");
        Assert.notNull(owner, "Owner is mandatory.");
        Assert.notNull(storage, "Destination storage location is mandatory");
        Assert.notNull(url, "Url storage location is mandatory");

        FileReferenceRequestDTO request = new FileReferenceRequestDTO();
        request.fileName = fileName;
        request.checksum = checksum;
        request.algorithm = algorithm;
        request.mimeType = mimeType;
        request.fileSize = fileSize;
        request.owner = owner;
        request.storage = storage;
        request.url = url;
        return request;
    }

    public FileReferenceRequestDTO withType(String type) {
        this.type = type;
        return this;
    }

    public FileReferenceRequestDTO withHeight(Integer height) {
        this.height = height;
        return this;
    }

    public FileReferenceRequestDTO withWidth(Integer width) {
        this.width = width;
        return this;
    }

    public FileReferenceMetaInfo buildMetaInfo() {
        FileReferenceMetaInfo metaInfo = new FileReferenceMetaInfo(checksum, algorithm, fileName, fileSize,
                MediaType.valueOf(mimeType));
        metaInfo.setHeight(height);
        metaInfo.setWidth(width);
        metaInfo.setType(type);
        return metaInfo;
    }

}
