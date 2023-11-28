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
package fr.cnes.regards.modules.order.test;

import com.google.common.collect.Sets;
import fr.cnes.regards.modules.filecatalog.amqp.input.FilesAvailabilityRequestEvent;
import fr.cnes.regards.modules.filecatalog.amqp.output.FileReferenceEvent;
import fr.cnes.regards.modules.filecatalog.amqp.output.FileReferenceEventType;
import fr.cnes.regards.modules.filecatalog.client.RequestInfo;
import fr.cnes.regards.modules.filecatalog.client.listener.IStorageFileListener;
import fr.cnes.regards.modules.filecatalog.dto.FileLocationDto;
import fr.cnes.regards.modules.filecatalog.dto.FileReferenceMetaInfoDto;
import fr.cnes.regards.modules.filecatalog.dto.request.FileCopyRequestDto;
import fr.cnes.regards.modules.filecatalog.dto.request.FileDeletionRequestDto;
import fr.cnes.regards.modules.filecatalog.dto.request.FileReferenceRequestDto;
import fr.cnes.regards.modules.filecatalog.dto.request.FileStorageRequestDto;
import fr.cnes.regards.modules.storage.client.IStorageClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class StorageClientMock implements IStorageClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(StorageClientMock.class);

    @Autowired
    private final IStorageFileListener listener;

    private boolean isAvailable = true;

    private boolean waitMode = false;

    public StorageClientMock(IStorageFileListener listener, boolean isAvailable) {
        super();
        this.listener = listener;
        this.isAvailable = isAvailable;
    }

    @Override
    public RequestInfo store(FileStorageRequestDto file) {
        return null;
    }

    @Override
    public Collection<RequestInfo> store(Collection<FileStorageRequestDto> files) {
        return null;
    }

    @Override
    public void storeRetry(RequestInfo requestInfo) {

    }

    @Override
    public void storeRetry(Collection<String> owners) {
    }

    @Override
    public void availabilityRetry(RequestInfo requestInfo) {
    }

    @Override
    public RequestInfo reference(FileReferenceRequestDto file) {
        return null;
    }

    @Override
    public Collection<RequestInfo> reference(Collection<FileReferenceRequestDto> files) {
        return null;
    }

    @Override
    public RequestInfo delete(FileDeletionRequestDto file) {
        return null;
    }

    @Override
    public Collection<RequestInfo> delete(Collection<FileDeletionRequestDto> files) {
        return null;
    }

    @Override
    public RequestInfo copy(FileCopyRequestDto file) {
        return null;
    }

    @Override
    public Collection<RequestInfo> copy(Collection<FileCopyRequestDto> files) {
        return null;
    }

    @Override
    public Collection<RequestInfo> makeAvailable(Collection<String> checksums, OffsetDateTime expirationDate) {
        Collection<String> groupIds = Sets.newHashSet();
        int count = 0;
        if (!waitMode) {
            LOGGER.info("Simulate storage responses !!!!!!!");
            List<FileReferenceEvent> notAvailable = new ArrayList<>();
            List<FileReferenceEvent> available = new ArrayList<>();
            for (String c : checksums) {
                if (count > FilesAvailabilityRequestEvent.MAX_REQUEST_PER_GROUP) {
                    count = 0;
                }
                if (count == 0) {
                    groupIds.add(UUID.randomUUID().toString());
                }
                if (!isAvailable) {
                    notAvailable.add(FileReferenceEvent.build(c,
                                                              null,
                                                              FileReferenceEventType.AVAILABILITY_ERROR,
                                                              null,
                                                              "",
                                                              new FileLocationDto("storageName", "url"),
                                                              new FileReferenceMetaInfoDto("checksum",
                                                                                           "MD5",
                                                                                           "fileName",
                                                                                           null,
                                                                                           null,
                                                                                           null,
                                                                                           null,
                                                                                           null),
                                                              groupIds));
                } else {
                    available.add(FileReferenceEvent.build(c,
                                                           null,
                                                           FileReferenceEventType.AVAILABLE,
                                                           null,
                                                           "",
                                                           new FileLocationDto("storageName", "url"),
                                                           new FileReferenceMetaInfoDto("checksum",
                                                                                        "MD5",
                                                                                        "fileName",
                                                                                        null,
                                                                                        null,
                                                                                        null,
                                                                                        null,
                                                                                        null),
                                                           groupIds));
                }
            }
            listener.onFileNotAvailable(notAvailable);
            listener.onFileAvailable(available);
        }

        return groupIds.stream().map(RequestInfo::build).collect(Collectors.toSet());
    }

    @Override
    public void cancelRequests(Collection<String> requestGroups) {
    }

    public boolean isWaitMode() {
        return waitMode;
    }

    public void setWaitMode(boolean waitMode) {
        this.waitMode = waitMode;
    }

}
