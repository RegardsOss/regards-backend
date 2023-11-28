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
package fr.cnes.regards.modules.filecatalog.dto.request;

import fr.cnes.regards.modules.filecatalog.dto.FileReferenceMetaInfoDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import java.util.Optional;

/**
 * Information about a file for a store request.<br/>
 * Mandatory information are : <ul>
 * <li> Filename</li>
 * <li> Checksum</li>
 * <li> Checksum algorithm </li>
 * <li> mimeType </li>
 * <li> Storage location where to delete the file</li>
 * <li> Owner of the file who ask for storage </li>
 * <li> originUrl where to access file to store. Must be locally accessible (file protocol for example) </li>
 * </ul>
 * See StorageFlowItem for more information about storage request process.
 *
 * @author Sébastien Binda
 */
public class FileStorageRequestDto {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileStorageRequestDto.class);

    private String fileName;

    private String checksum;

    private String algorithm;

    private String mimeType;

    private String owner;

    private String type;

    private String originUrl;

    private String storage;

    private FileReferenceMetaInfoDto metaInfo;

    private String subDirectory;

    private String sessionOwner;

    private String session;

    public static FileStorageRequestDto build(String fileName,
                                              String checksum,
                                              String algorithm,
                                              String mimeType,
                                              String owner,
                                              String sessionOwner,
                                              String session,
                                              String originUrl,
                                              String storage,
                                              FileReferenceMetaInfoDto metaInfo,
                                              Optional<String> subDirectory) {

        Assert.notNull(fileName, "File name is mandatory.");
        Assert.notNull(checksum, "Checksum is mandatory.");
        Assert.notNull(algorithm, "Algorithm is mandatory.");
        Assert.notNull(mimeType, "MimeType is mandatory.");
        Assert.notNull(owner, "Owner is mandatory.");
        Assert.notNull(originUrl, "Origin url is mandatory.");
        Assert.notNull(storage, "Destination storage location is mandatory");

        FileStorageRequestDto request = new FileStorageRequestDto();
        request.fileName = fileName;
        request.checksum = checksum;
        request.algorithm = algorithm;
        request.mimeType = mimeType;
        request.owner = owner;
        request.originUrl = originUrl;
        request.storage = storage;
        request.metaInfo = metaInfo;
        request.sessionOwner = sessionOwner;
        request.session = session;

        if (subDirectory != null) {
            request.subDirectory = subDirectory.orElse(null);
        }
        return request;
    }

    public static FileStorageRequestDto build(String fileName,
                                              String checksum,
                                              String algorithm,
                                              String mimeType,
                                              String owner,
                                              String sessionOwner,
                                              String session,
                                              String originUrl,
                                              String storage,
                                              Optional<String> subDirectory) {

        Assert.notNull(fileName, "File name is mandatory.");
        Assert.notNull(checksum, "Checksum is mandatory.");
        Assert.notNull(algorithm, "Algorithm is mandatory.");
        Assert.notNull(mimeType, "MimeType is mandatory.");
        Assert.notNull(owner, "Owner is mandatory.");
        Assert.notNull(originUrl, "Origin url is mandatory.");
        Assert.notNull(storage, "Destination storage location is mandatory");

        FileStorageRequestDto request = new FileStorageRequestDto();
        request.fileName = fileName;
        request.checksum = checksum;
        request.algorithm = algorithm;
        request.mimeType = mimeType;
        request.owner = owner;
        request.originUrl = originUrl;
        request.storage = storage;
        request.metaInfo = new FileReferenceMetaInfoDto(checksum,
                                                        algorithm,
                                                        fileName,
                                                        null,
                                                        null,
                                                        null,
                                                        mimeType,
                                                        null);
        request.session = session;
        request.sessionOwner = sessionOwner;
        if (subDirectory != null) {
            request.subDirectory = subDirectory.orElse(null);
        }
        return request;
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

    public FileReferenceMetaInfoDto getMetaInfo() {
        return metaInfo;
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
     * Add optional type to current {@link FileStorageRequestDto}
     *
     * @return current {@link FileStorageRequestDto}
     */
    public FileStorageRequestDto withType(String type) {
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
        return "FileStorageRequestDto [" + fn + cs + algo + mt + ow + so + s + t + url + sto + sd + "]";
    }

}
