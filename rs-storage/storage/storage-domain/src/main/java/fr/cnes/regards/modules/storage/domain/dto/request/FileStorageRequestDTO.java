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
package fr.cnes.regards.modules.storage.domain.dto.request;

import java.util.Optional;

import javax.annotation.Nullable;
import javax.validation.constraints.NotBlank;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.util.InvalidMimeTypeException;

import fr.cnes.regards.modules.storage.domain.database.FileReferenceMetaInfo;
import fr.cnes.regards.modules.storage.domain.flow.StorageFlowItem;

/**
 * Information about a file for a store request.<br/>
 * Mandatory information are : <ul>
 *  <li> Filename</li>
 *  <li> Checksum</li>
 *  <li> Checksum algorithm </li>
 *  <li> mimeType </li>
 *  <li> Storage location where to delete the file</li>
 *  <li> Owner of the file who ask for storage </li>
 *  <li> originUrl where to access file to store. Must be locally accessible (file protocol for example) </li>
 * </ul>
 * See {@link StorageFlowItem} for more information about storage request process.
 *
 * @author Sébastien Binda
 */
public class FileStorageRequestDTO {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileStorageRequestDTO.class);

    @NotBlank(message = "File name is mandatory")
    private String fileName;

    @NotBlank(message = "Checksum is mandatory")
    private String checksum;

    @NotBlank(message = "Algorithm is mandatory")
    private String algorithm;

    @NotBlank(message = "MimeType is mandatory")
    private String mimeType;

    @NotBlank(message = "Owner is mandatory")
    private String owner;

    private String type;

    @NotBlank(message = "Origine URL is mandatory")
    private String originUrl;

    @NotBlank(message = "Storage is mandatory")
    private String storage;

    @Nullable
    private String subDirectory;

    private String sessionOwner;

    private String session;

    public static FileStorageRequestDTO build(String fileName, String checksum, String algorithm, String mimeType,
            String owner, String sessionOwner, String session, String originUrl, String storage, Optional<String> subDirectory) {

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
        request.sessionOwner = sessionOwner;
        request.session = session;

        if (subDirectory != null) {
            request.subDirectory = subDirectory.orElse(null);
        }
        return request;
    }

    /**
     * Build a {@link FileReferenceMetaInfo} with the current request information.
     * @return {@link FileReferenceMetaInfo}
     */
    public FileReferenceMetaInfo buildMetaInfo() {
        MediaType mt = MediaType.APPLICATION_OCTET_STREAM;
        try {
            mt = MediaType.valueOf(mimeType);
        } catch (InvalidMimeTypeException e) {
            LOGGER.error("Invalid media type for new file reference : %s .Falling back to default media type application/octet-stream",
                         e);
        }
        return new FileReferenceMetaInfo(checksum, algorithm, fileName, null, mt).withType(type);
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

    public String getOriginUrl() {
        return originUrl;
    }

    public String getStorage() {
        return storage;
    }

    public Optional<String> getOptionalSubDirectory() {
        return Optional.ofNullable(subDirectory);
    }

    public String getSubDirectory() {
        return subDirectory;
    }

    public String getType() {
        return type;
    }

    public String getSessionOwner() {
        return sessionOwner;
    }

    public String getSession() {
        return session;
    }

    /**
     * Add optional type to current {@link FileStorageRequestDTO}
     * @param type
     * @return current {@link FileStorageRequestDTO}
     */
    public FileStorageRequestDTO withType(String type) {
        this.type = type;
        return this;
    }

    @Override
    public String toString() {
        String fn = (fileName != null ? "fileName=" + fileName + ", " : "");
        String cs = (checksum != null ? "checksum=" + checksum + ", " : "");
        String algo = (algorithm != null ? "algorithm=" + algorithm + ", " : "");
        String mt = (mimeType != null ? "mimeType=" + mimeType + ", " : "");
        String ow = (owner != null ? "owner=" + owner + ", " : "");
        String so = (sessionOwner != null ? "sessionOwner=" + sessionOwner + ", " : "");
        String s = (session != null ? "session=" + session + ", " : "");
        String t = (type != null ? "type=" + type + ", " : "");
        String url = (originUrl != null ? "originUrl=" + originUrl + ", " : "");
        String sto = (storage != null ? "storage=" + storage + ", " : "");
        String sd = (subDirectory != null ? "subDirectory=" + subDirectory : "");
        return "FileStorageRequestDTO [" + fn + cs + algo + mt + ow + so + s + t + url + sto + sd + "]";
    }

}
