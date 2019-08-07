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
package fr.cnes.regards.modules.storagelight.domain.dto;

import java.net.URL;
import java.util.Optional;

import org.springframework.http.MediaType;
import org.springframework.util.Assert;

import fr.cnes.regards.modules.storagelight.domain.database.FileReferenceMetaInfo;

/**
 * @author sbinda
 *
 */
public class FileStorageRequestDTO {

    private String fileName;

    private String checksum;

    private String algorithm;

    private String mimeType;

    private String owner;

    private String type;

    private URL originUrl;

    private String storage;

    private Optional<String> subDirectory;

    public static FileStorageRequestDTO build(String fileName, String checksum, String algorithm, String mimeType,
            String owner, URL originUrl, String storage, Optional<String> subDirectory) {

        Assert.notNull(fileName, "File name is mandatory.");
        Assert.notNull(checksum, "Checksum is mandatory.");
        Assert.notNull(algorithm, "Algorithm is mandatory.");
        Assert.notNull(mimeType, "MimeType is mandatory.");
        Assert.notNull(owner, "Owner is mandatory.");
        Assert.notNull(originUrl, "Origin url is mandatory.");
        Assert.notNull(storage, "Destination storage location is mandatory");

        FileStorageRequestDTO request = new FileStorageRequestDTO();
        request.fileName = fileName;
        request.checksum = checksum;
        request.algorithm = algorithm;
        request.mimeType = mimeType;
        request.owner = owner;
        request.originUrl = originUrl;
        request.storage = storage;
        request.subDirectory = subDirectory;
        return request;
    }

    public FileReferenceMetaInfo buildMetaInfo() {
        return new FileReferenceMetaInfo(checksum, algorithm, fileName, null, MediaType.valueOf(mimeType))
                .withType(type);
    }

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

    public String getOwner() {
        return owner;
    }

    public URL getOriginUrl() {
        return originUrl;
    }

    public String getStorage() {
        return storage;
    }

    public Optional<String> getSubDirectory() {
        return subDirectory;
    }

    public FileStorageRequestDTO withType(String type) {
        this.mimeType = type;
        return this;
    }

    public String getType() {
        return type;
    }

}
