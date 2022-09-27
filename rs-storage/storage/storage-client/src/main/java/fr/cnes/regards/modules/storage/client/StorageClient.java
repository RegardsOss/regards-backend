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
package fr.cnes.regards.modules.storage.client;

import com.google.common.collect.Lists;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.modules.storage.domain.dto.request.FileCopyRequestDTO;
import fr.cnes.regards.modules.storage.domain.dto.request.FileDeletionRequestDTO;
import fr.cnes.regards.modules.storage.domain.dto.request.FileReferenceRequestDTO;
import fr.cnes.regards.modules.storage.domain.dto.request.FileStorageRequestDTO;
import fr.cnes.regards.modules.storage.domain.event.CancelRequestEvent;
import fr.cnes.regards.modules.storage.domain.flow.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiFunction;

/**
 * Asynchronous client implementation based on the message broker for requesting the file storage service.<br />
 * As this client use message broker to communicate with the storage service, responses are asynchronous. Nevertheless,
 * you can easily listen responses by implementing your own {@link IStorageRequestListener}.
 *
 * @author Marc SORDI
 * @author Sébastien Binda
 */
@Component
public class StorageClient implements IStorageClient {

    @Autowired
    private IPublisher publisher;

    @Override
    public RequestInfo copy(FileCopyRequestDTO file) {
        RequestInfo requestInfo = RequestInfo.build();
        publisher.publish(CopyFlowItem.build(file, requestInfo.getGroupId()));
        return requestInfo;
    }

    @Override
    public Collection<RequestInfo> copy(Collection<FileCopyRequestDTO> files) {
        return publish(CopyFlowItem::build, files, CopyFlowItem.MAX_REQUEST_PER_GROUP);
    }

    @Override
    public RequestInfo delete(FileDeletionRequestDTO file) {
        RequestInfo requestInfo = RequestInfo.build();
        publisher.publish(DeletionFlowItem.build(file, requestInfo.getGroupId()));
        return requestInfo;
    }

    @Override
    public Collection<RequestInfo> delete(Collection<FileDeletionRequestDTO> files) {
        return publish(DeletionFlowItem::build, files, DeletionFlowItem.MAX_REQUEST_PER_GROUP);
    }

    @Override
    public RequestInfo reference(FileReferenceRequestDTO file) {
        RequestInfo requestInfo = RequestInfo.build();
        publisher.publish(ReferenceFlowItem.build(file, requestInfo.getGroupId()));
        return requestInfo;
    }

    @Override
    public Collection<RequestInfo> reference(Collection<FileReferenceRequestDTO> files) {
        return publish(ReferenceFlowItem::build, files, ReferenceFlowItem.MAX_REQUEST_PER_GROUP);
    }

    @Override
    public void storeRetry(RequestInfo requestInfo) {
        publisher.publish(RetryFlowItem.buildStorageRetry(requestInfo.getGroupId()));
    }

    @Override
    public void storeRetry(Collection<String> owners) {
        publisher.publish(RetryFlowItem.buildStorageRetry(owners));
    }

    @Override
    public void availabilityRetry(RequestInfo requestInfo) {
        publisher.publish(RetryFlowItem.buildAvailabilityRetry(requestInfo.getGroupId()));
    }

    @Override
    public RequestInfo store(FileStorageRequestDTO file) {
        RequestInfo requestInfo = RequestInfo.build();
        publisher.publish(StorageFlowItem.build(file, requestInfo.getGroupId()));
        return requestInfo;
    }

    @Override
    public Collection<RequestInfo> store(Collection<FileStorageRequestDTO> files) {
        return publish(StorageFlowItem::build, files, StorageFlowItem.MAX_REQUEST_PER_GROUP);
    }

    @Override
    public Collection<RequestInfo> makeAvailable(Collection<String> checksums, OffsetDateTime expirationDate) {
        Collection<RequestInfo> requestInfos = Lists.newArrayList();
        // If number of files in the request is less than the maximum allowed by request then publish it
        if (checksums.size() <= AvailabilityFlowItem.MAX_REQUEST_PER_GROUP) {
            RequestInfo requestInfo = RequestInfo.build();
            publisher.publish(AvailabilityFlowItem.build(checksums, expirationDate, requestInfo.getGroupId()));
            requestInfos.add(requestInfo);
        } else {
            // Else publish as many requests as needed.
            List<String> group = Lists.newArrayList();
            Iterator<String> it = checksums.iterator();
            while (it.hasNext()) {
                group.add(it.next());
                if (group.size() >= AvailabilityFlowItem.MAX_REQUEST_PER_GROUP) {
                    RequestInfo requestInfo = RequestInfo.build();
                    publisher.publish(AvailabilityFlowItem.build(group, expirationDate, requestInfo.getGroupId()));
                    requestInfos.add(requestInfo);
                    group.clear();
                }
            }
            if (!group.isEmpty()) {
                RequestInfo requestInfo = RequestInfo.build();
                publisher.publish(AvailabilityFlowItem.build(group, expirationDate, requestInfo.getGroupId()));
                requestInfos.add(requestInfo);
            }
        }
        return requestInfos;
    }

    @Override
    public void cancelRequests(Collection<String> requestGroups) {
        if (!requestGroups.isEmpty()) {
            publisher.publish(new CancelRequestEvent(requestGroups));
        }
    }

    private <T> Collection<RequestInfo> publish(BiFunction<Collection<T>, String, ISubscribable> func,
                                                Collection<T> files,
                                                int maxFilesPerRequest) {
        Collection<RequestInfo> requestInfos = Lists.newArrayList();
        // If number of files in the request is less than the maximum allowed by request then publish it
        if (files.size() <= maxFilesPerRequest) {
            RequestInfo requestInfo = RequestInfo.build();
            publisher.publish(func.apply(files, requestInfo.getGroupId()));
            requestInfos.add(requestInfo);
        } else {
            // Else publish as many requests as needed.
            List<T> group = Lists.newArrayList();
            Iterator<T> it = files.iterator();
            while (it.hasNext()) {
                group.add(it.next());
                if (group.size() >= maxFilesPerRequest) {
                    RequestInfo requestInfo = RequestInfo.build();
                    publisher.publish(func.apply(group, requestInfo.getGroupId()));
                    requestInfos.add(requestInfo);
                    group.clear();
                }
            }
            if (!group.isEmpty()) {
                RequestInfo requestInfo = RequestInfo.build();
                publisher.publish(func.apply(group, requestInfo.getGroupId()));
                requestInfos.add(requestInfo);
            }
        }
        return requestInfos;
    }
}
