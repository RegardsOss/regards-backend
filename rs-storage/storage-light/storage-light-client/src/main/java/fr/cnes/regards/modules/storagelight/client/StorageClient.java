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

import java.time.OffsetDateTime;
import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.modules.storagelight.domain.dto.FileCopyRequestDTO;
import fr.cnes.regards.modules.storagelight.domain.dto.FileDeletionRequestDTO;
import fr.cnes.regards.modules.storagelight.domain.dto.FileReferenceRequestDTO;
import fr.cnes.regards.modules.storagelight.domain.dto.FileStorageRequestDTO;
import fr.cnes.regards.modules.storagelight.domain.flow.AvailabilityFlowItem;
import fr.cnes.regards.modules.storagelight.domain.flow.CopyFlowItem;
import fr.cnes.regards.modules.storagelight.domain.flow.DeletionFlowItem;
import fr.cnes.regards.modules.storagelight.domain.flow.ReferenceFlowItem;
import fr.cnes.regards.modules.storagelight.domain.flow.RetryFlowItem;
import fr.cnes.regards.modules.storagelight.domain.flow.StorageFlowItem;

/**
 * Asynchronous client implementation based on the message broker for requesting the file storage service.<br />
 * As this client use message broker to communicate with the storage service, responses are synchronous. NEvertheless,
 * you can easily listen for response by implementing your own {@link IStorageRequestListener}.
 *
 * @author Marc SORDI
 * @author Sébastien Binda
 *
 */
@Component
public class StorageClient implements IStorageClient {

    @Autowired
    private IPublisher publisher;

    @Override
    public RequestInfo copy(FileCopyRequestDTO file) {
        RequestInfo requestInfo = RequestInfo.build();
        publisher.publish(CopyFlowItem.build(file, requestInfo.getRequestId()));
        return requestInfo;
    }

    @Override
    public RequestInfo copy(Collection<FileCopyRequestDTO> files) {
        RequestInfo requestInfo = RequestInfo.build();
        publisher.publish(CopyFlowItem.build(files, requestInfo.getRequestId()));
        return requestInfo;
    }

    @Override
    public RequestInfo delete(FileDeletionRequestDTO file) {
        RequestInfo requestInfo = RequestInfo.build();
        publisher.publish(DeletionFlowItem.build(file, requestInfo.getRequestId()));
        return requestInfo;
    }

    @Override
    public RequestInfo delete(Collection<FileDeletionRequestDTO> files) {
        RequestInfo requestInfo = RequestInfo.build();
        publisher.publish(DeletionFlowItem.build(files, requestInfo.getRequestId()));
        return requestInfo;
    }

    @Override
    public RequestInfo makeAvailable(Collection<String> checksums, OffsetDateTime expirationDate) {
        RequestInfo requestInfo = RequestInfo.build();
        publisher.publish(AvailabilityFlowItem.build(checksums, expirationDate, requestInfo.getRequestId()));
        return requestInfo;
    }

    @Override
    public RequestInfo reference(FileReferenceRequestDTO file) {
        RequestInfo requestInfo = RequestInfo.build();
        publisher.publish(ReferenceFlowItem.build(file, requestInfo.getRequestId()));
        return requestInfo;
    }

    @Override
    public RequestInfo reference(Collection<FileReferenceRequestDTO> files) {
        RequestInfo requestInfo = RequestInfo.build();
        publisher.publish(ReferenceFlowItem.build(files, requestInfo.getRequestId()));
        return requestInfo;
    }

    @Override
    public void storeRetry(RequestInfo requestInfo) {
        publisher.publish(RetryFlowItem.buildStorageRetry(requestInfo.getRequestId()));
    }

    @Override
    public void storeRetry(Collection<String> owners) {
        publisher.publish(RetryFlowItem.buildStorageRetry(owners));
    }

    @Override
    public void availabilityRetry(RequestInfo requestInfo) {
        publisher.publish(RetryFlowItem.buildAvailabilityRetry(requestInfo.getRequestId()));
    }

    @Override
    public RequestInfo store(FileStorageRequestDTO file) {
        RequestInfo requestInfo = RequestInfo.build();
        publisher.publish(StorageFlowItem.build(file, requestInfo.getRequestId()));
        return requestInfo;
    }

    @Override
    public RequestInfo store(Collection<FileStorageRequestDTO> files) {
        RequestInfo requestInfo = RequestInfo.build();
        publisher.publish(StorageFlowItem.build(files, requestInfo.getRequestId()));
        return requestInfo;
    }
}
