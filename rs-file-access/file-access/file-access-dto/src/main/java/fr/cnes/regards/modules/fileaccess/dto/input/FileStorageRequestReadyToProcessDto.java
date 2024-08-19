/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.fileaccess.dto.input;

import java.util.Objects;

/**
 * Information about a new file storage request in the file-access microservice.<br/>
 * This request will be processed by file-access, the file will be stored.
 *
 * @author Thibaud Michaudel
 */
public class FileStorageRequestReadyToProcessDto {

    private final Long requestId;

    private final String checksum;

    private final String algorithm;

    private final String originUrl;

    private final String storage;

    private final String subDirectory;

    private final String owner;

    private final String session;

    private final boolean activateSmallFilePackaging;

    private final FileStorageMetaInfoDto metadata;

    private final boolean reference;

    public FileStorageRequestReadyToProcessDto(Long requestId,
                                               String checksum,
                                               String algorithm,
                                               String originUrl,
                                               String storage,
                                               String subDirectory,
                                               String owner,
                                               String session,
                                               boolean activateSmallFilePackaging,
                                               FileStorageMetaInfoDto metadata,
                                               boolean reference) {
        this.requestId = requestId;
        this.checksum = checksum;
        this.algorithm = algorithm;
        this.originUrl = originUrl;
        this.storage = storage;
        this.subDirectory = subDirectory;
        this.owner = owner;
        this.session = session;
        this.activateSmallFilePackaging = activateSmallFilePackaging;
        this.metadata = metadata;
        this.reference = reference;
    }

    public Long getRequestId() {
        return requestId;
    }

    public String getChecksum() {
        return checksum;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public String getOriginUrl() {
        return originUrl;
    }

    public String getStorage() {
        return storage;
    }

    public String getSubDirectory() {
        return subDirectory;
    }

    public String getOwner() {
        return owner;
    }

    public String getSession() {
        return session;
    }

    public FileStorageMetaInfoDto getMetadata() {
        return metadata;
    }

    public boolean isActivateSmallFilePackaging() {
        return activateSmallFilePackaging;
    }

    public boolean isReference() {
        return reference;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FileStorageRequestReadyToProcessDto that = (FileStorageRequestReadyToProcessDto) o;
        return activateSmallFilePackaging == that.activateSmallFilePackaging
               && Objects.equals(requestId,
                                 that.requestId)
               && Objects.equals(checksum, that.checksum)
               && Objects.equals(algorithm, that.algorithm)
               && Objects.equals(originUrl, that.originUrl)
               && Objects.equals(storage, that.storage)
               && Objects.equals(subDirectory, that.subDirectory)
               && Objects.equals(owner, that.owner)
               && Objects.equals(session, that.session)
               && Objects.equals(metadata, that.metadata)
               && Objects.equals(reference, that.reference);
    }

    @Override
    public int hashCode() {
        return Objects.hash(requestId,
                            checksum,
                            algorithm,
                            originUrl,
                            storage,
                            subDirectory,
                            owner,
                            session,
                            activateSmallFilePackaging,
                            metadata,
                            reference);
    }

    @Override
    public String toString() {
        return "FileStorageRequestReadyToProcessDto{"
               + "requestId="
               + requestId
               + ", checksum='"
               + checksum
               + '\''
               + ", algorithm='"
               + algorithm
               + '\''
               + ", originUrl='"
               + originUrl
               + '\''
               + ", storage='"
               + storage
               + '\''
               + ", subDirectory='"
               + subDirectory
               + '\''
               + ", owner='"
               + owner
               + '\''
               + ", session='"
               + session
               + '\''
               + ", activateSmallFilePackaging="
               + activateSmallFilePackaging
               + ", metadata="
               + metadata
               + ", reference="
               + reference
               + '}';
    }
}
