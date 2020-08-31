/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Sets;
import fr.cnes.regards.modules.storage.client.FileReferenceEventDTO;
import fr.cnes.regards.modules.storage.client.IStorageClient;
import fr.cnes.regards.modules.storage.client.IStorageFileListener;
import fr.cnes.regards.modules.storage.client.RequestInfo;
import fr.cnes.regards.modules.storage.domain.dto.request.FileCopyRequestDTO;
import fr.cnes.regards.modules.storage.domain.dto.request.FileDeletionRequestDTO;
import fr.cnes.regards.modules.storage.domain.dto.request.FileReferenceRequestDTO;
import fr.cnes.regards.modules.storage.domain.dto.request.FileStorageRequestDTO;
import fr.cnes.regards.modules.storage.domain.event.FileReferenceEvent;
import fr.cnes.regards.modules.storage.domain.event.FileReferenceEventType;
import fr.cnes.regards.modules.storage.domain.flow.AvailabilityFlowItem;

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
    public RequestInfo store(FileStorageRequestDTO file) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection<RequestInfo> store(Collection<FileStorageRequestDTO> files) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void storeRetry(RequestInfo requestInfo) {
        // TODO Auto-generated method stub

    }

    @Override
    public void storeRetry(Collection<String> owners) {
        // TODO Auto-generated method stub

    }

    @Override
    public void availabilityRetry(RequestInfo requestInfo) {
        // TODO Auto-generated method stub

    }

    @Override
    public RequestInfo reference(FileReferenceRequestDTO file) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection<RequestInfo> reference(Collection<FileReferenceRequestDTO> files) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public RequestInfo delete(FileDeletionRequestDTO file) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection<RequestInfo> delete(Collection<FileDeletionRequestDTO> files) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public RequestInfo copy(FileCopyRequestDTO file) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection<RequestInfo> copy(Collection<FileCopyRequestDTO> files) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection<RequestInfo> makeAvailable(Collection<String> checksums, OffsetDateTime expirationDate) {
        Collection<String> groupIds = Sets.newHashSet();
        int count = 0;
        if (!waitMode) {
            LOGGER.info("Simulate storage responses !!!!!!!");
            List<FileReferenceEventDTO> notAvailable = new ArrayList<>();
            List<FileReferenceEventDTO> available = new ArrayList<>();
            for (String c : checksums) {
                if (count > AvailabilityFlowItem.MAX_REQUEST_PER_GROUP) {
                    count = 0;
                }
                if (count == 0) {
                    groupIds.add(UUID.randomUUID().toString());
                }
                if (!isAvailable) {
                    notAvailable.add(new FileReferenceEventDTO(FileReferenceEvent.build(c,
                                                                                        null,
                                                                                        FileReferenceEventType.AVAILABILITY_ERROR,
                                                                                        null,
                                                                                        "",
                                                                                        null,
                                                                                        null,
                                                                                        groupIds)));
                } else {
                    available.add(new FileReferenceEventDTO(FileReferenceEvent.build(c,
                                                                                        null,
                                                                                        FileReferenceEventType.AVAILABLE,
                                                                                        null,
                                                                                        "",
                                                                                        null,
                                                                                        null,
                                                                                        groupIds)));
                }
            }
            listener.onFileNotAvailable(notAvailable);
            listener.onFileAvailable(available);
        }

        return groupIds.stream().map(RequestInfo::build).collect(Collectors.toSet());
    }

    public boolean isWaitMode() {
        return waitMode;
    }

    public void setWaitMode(boolean waitMode) {
        this.waitMode = waitMode;
    }

}
