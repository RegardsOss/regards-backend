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
package fr.cnes.regards.modules.storagelight.client.test;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MimeType;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.modules.storagelight.client.IStorageClient;
import fr.cnes.regards.modules.storagelight.client.RequestInfo;
import fr.cnes.regards.modules.storagelight.client.StorageClient;
import fr.cnes.regards.modules.storagelight.domain.database.FileLocation;
import fr.cnes.regards.modules.storagelight.domain.database.FileReference;
import fr.cnes.regards.modules.storagelight.domain.database.FileReferenceMetaInfo;
import fr.cnes.regards.modules.storagelight.domain.database.request.RequestResultInfo;
import fr.cnes.regards.modules.storagelight.domain.dto.request.FileCopyRequestDTO;
import fr.cnes.regards.modules.storagelight.domain.dto.request.FileDeletionRequestDTO;
import fr.cnes.regards.modules.storagelight.domain.dto.request.FileReferenceRequestDTO;
import fr.cnes.regards.modules.storagelight.domain.dto.request.FileStorageRequestDTO;
import fr.cnes.regards.modules.storagelight.domain.event.FileRequestType;
import fr.cnes.regards.modules.storagelight.domain.event.FileRequestsGroupEvent;
import fr.cnes.regards.modules.storagelight.domain.flow.FlowItemStatus;

/**
 * Provide a bean to replace the behavior of the {@link StorageClient} while testing
 * @author Léo Mieulet
 */
@Profile("StorageClientMock")
@Component
@Primary
@Transactional
public class StorageClientMock implements IStorageClient {

    @Autowired
    private IPublisher publisher;

    private Optional<Boolean> shouldReturnGranted;

    private Optional<Boolean> shouldReturnSuccess;

    /**
     *
     * @param shouldReturnGranted when true return granted, otherwise denied
     * @param shouldReturnSuccess when true return success, otherwise error
     */
    public void setBehavior(boolean shouldReturnGranted, boolean shouldReturnSuccess) {
        this.shouldReturnGranted = Optional.of(shouldReturnGranted);
        this.shouldReturnSuccess = Optional.of(shouldReturnSuccess);
    }

    @Override
    public RequestInfo store(FileStorageRequestDTO file) {
        checkInit();
        RequestInfo requestInfo = RequestInfo.build();

        // Send the first event
        FlowItemStatus firstStatus;
        if (shouldReturnGranted.get()) {
            firstStatus = FlowItemStatus.GRANTED;
        } else {
            firstStatus = FlowItemStatus.DENIED;
        }

        RequestResultInfo resultInfo = new RequestResultInfo(requestInfo.getGroupId(), FileRequestType.STORAGE,
                file.getChecksum(), file.getStorage(), file.getSubDirectory().orElse(null));

        resultInfo.setResultFile(new FileReference(file.getOwner(),
                new FileReferenceMetaInfo(file.getChecksum(), file.getAlgorithm(), file.getFileName(), 1000L,
                        MimeType.valueOf(file.getMimeType())),
                new FileLocation(file.getStorage(), "http://somedomain.com/api/v1/storage/file/2")));
        List<RequestResultInfo> requestInfos = Collections.singletonList(resultInfo);
        publisher.publish(FileRequestsGroupEvent.build(requestInfo.getGroupId(), FileRequestType.STORAGE, firstStatus,
                                                       requestInfos));

        // Send the second event if the first one is GRANTED
        if (shouldReturnGranted.get()) {
            FlowItemStatus secondStatus;
            if (shouldReturnSuccess.get()) {
                secondStatus = FlowItemStatus.SUCCESS;
            } else {
                secondStatus = FlowItemStatus.ERROR;
            }
            publisher.publish(FileRequestsGroupEvent.build(requestInfo.getGroupId(), FileRequestType.STORAGE,
                                                           secondStatus, requestInfos));
        }

        return requestInfo;
    }

    /**
     * Throw an exception if the setBehavior method have not been called
     */
    private void checkInit() {
        if (!shouldReturnGranted.isPresent() && !shouldReturnSuccess.isPresent()) {
            throw new RuntimeException("Please call setBehavior before using the StorageClientMock");
        }
    }

    @Override
    public RequestInfo store(Collection<FileStorageRequestDTO> files) {
        return store(files.iterator().next());
    }

    @Override
    public void storeRetry(RequestInfo requestInfo) {
        // Not implemented yet
        throw new UnsupportedOperationException("Not implemented yet !");
    }

    @Override
    public void storeRetry(Collection<String> owners) {
        // Not implemented yet
        throw new UnsupportedOperationException("Not implemented yet !");
    }

    @Override
    public void availabilityRetry(RequestInfo requestInfo) {
        // Not implemented yet
        throw new UnsupportedOperationException("Not implemented yet !");
    }

    @Override
    public RequestInfo reference(FileReferenceRequestDTO file) {
        // Not implemented yet
        throw new UnsupportedOperationException("Not implemented yet !");
    }

    @Override
    public RequestInfo reference(Collection<FileReferenceRequestDTO> files) {
        // Not implemented yet
        throw new UnsupportedOperationException("Not implemented yet !");
    }

    @Override
    public RequestInfo delete(FileDeletionRequestDTO file) {
        checkInit();
        RequestInfo requestInfo = RequestInfo.build();

        // Send the first event
        FlowItemStatus firstStatus;
        if (shouldReturnGranted.get()) {
            firstStatus = FlowItemStatus.GRANTED;
        } else {
            firstStatus = FlowItemStatus.DENIED;
        }

        RequestResultInfo resultInfo = new RequestResultInfo(requestInfo.getGroupId(), FileRequestType.DELETION,
                file.getChecksum(), file.getStorage(), null);

        resultInfo.setResultFile(
                                 new FileReference(file.getOwner(),
                                         new FileReferenceMetaInfo(file.getChecksum(), "some algo", "some file", 1000L,
                                                 MimeType.valueOf("application/pdf")),
                                         new FileLocation(file.getStorage(), null)));
        List<RequestResultInfo> requestInfos = Collections.singletonList(resultInfo);
        publisher.publish(FileRequestsGroupEvent.build(requestInfo.getGroupId(), FileRequestType.DELETION, firstStatus,
                                                       requestInfos));

        // Send the second event if the first one is GRANTED
        if (shouldReturnGranted.get()) {
            FlowItemStatus secondStatus;
            if (shouldReturnSuccess.get()) {
                secondStatus = FlowItemStatus.SUCCESS;
            } else {
                secondStatus = FlowItemStatus.ERROR;
            }
            publisher.publish(FileRequestsGroupEvent.build(requestInfo.getGroupId(), FileRequestType.DELETION,
                                                           secondStatus, requestInfos));
        }

        return requestInfo;
    }

    @Override
    public RequestInfo delete(Collection<FileDeletionRequestDTO> files) {
        return delete(files.iterator().next());
    }

    @Override
    public RequestInfo copy(FileCopyRequestDTO file) {
        // Not implemented yet
        throw new UnsupportedOperationException("Not implemented yet !");
    }

    @Override
    public RequestInfo copy(Collection<FileCopyRequestDTO> files) {
        // Not implemented yet
        throw new UnsupportedOperationException("Not implemented yet !");
    }

    @Override
    public RequestInfo makeAvailable(Collection<String> checksums, OffsetDateTime expirationDate) {
        throw new UnsupportedOperationException();
    }
}
