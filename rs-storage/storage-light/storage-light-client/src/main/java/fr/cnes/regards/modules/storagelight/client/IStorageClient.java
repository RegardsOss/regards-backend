/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.storagelight.client;

import java.net.URL;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Optional;

import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.modules.storagelight.domain.event.FileReferenceEvent;
import fr.cnes.regards.modules.storagelight.domain.event.FileReferenceEventState;
import fr.cnes.regards.modules.storagelight.domain.plugin.IStorageLocation;

/**
 * Client interface for requesting the file storage service
 *
 * Client requests are done asynchronously.
 * To listen to the feedback messages, you have to implement your own message handler listening to {@link FileReferenceEvent}.
 * Be sure to check that the message is intended for you by validating the owner.
 * Look at {@link FileReferenceEventState} to adapt your behaviour.
 */
public interface IStorageClient {

    /**
     * Requests the copy of a file identified by his checksum to a destination storage location.
     * <br/>
     *
     * @param fileName filename
     * @param checksum checksum for related algorithm
     * @param algorithm checksum algorithm
     * @param mimeType file MIME type
     * @param fileSize file size
     * @param owner
     * @param storage {@link PluginConfiguration#getBusinessId()} of {@link IStorageLocation} plugin.
     * @param subDirectory optional subdirectory in destination storage location
     */
    void copy(String fileName, String checksum, String owner, String storage, Optional<String> subDirectory);

    /**
     * Requests storage of a file from an localy accessible URL to a destination storage defined
     * by {@link PluginConfiguration#getBusinessId()} of {@link IStorageLocation} plugin.
     * <br/>
     *
     * @param fileName
     * @param checksum
     * @param algorithm
     * @param mimeType
     * @param owner
     * @param originUrl Accessible file URL.
     * @param storage Plugin configuration business id for destination storage
     * @param subDirectory Optional sub directory into destination storage
     */
    void store(String fileName, String checksum, String algorithm, String mimeType, String owner, URL originUrl,
            String storage, Optional<String> subDirectory);

    /**
     * Requests to reference a file at a given storage location. With this request, file is not moved but referenced.
     * <br/>.
     *
     * @param fileName
     * @param checksum
     * @param algorithm
     * @param mimeType
     * @param fileSize
     * @param owner
     * @param storage file storage location
     * @param url file url expected by associated storage location
     */
    void reference(String fileName, String checksum, String algorithm, String mimeType, Long fileSize, String owner,
            String storage, String url);

    /**
     * Requests the deletion of the file identified by its checksum on the specified storage.<br/>
     * It is necessary to specify the owner as the file can be owned by several owners (multiple references).<br/>
     * As a result, the file will be really deleted if and only if no other owner remains!
     *
     * @param checksum file checksum
     * @param storage storage on which to delete the file
     * @param owner file owner
     */
    void delete(String checksum, String storage, String owner);

    /**
     * Requests that files identified by their checksums be put online so that they can be downloaded by a third party component.
     *
     * @param checksums list of file checksums
     * @param expirationDate date until which the file must be available
     * (after this date, the system could proceed to a possible cleaning of its cache, only offline files are concerned!)
     */
    void makeAvailable(Collection<String> checksums, OffsetDateTime expirationDate);
}
