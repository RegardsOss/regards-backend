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
import fr.cnes.regards.modules.filecatalog.amqp.input.*;
import fr.cnes.regards.modules.filecatalog.client.RequestInfo;
import fr.cnes.regards.modules.filecatalog.client.listener.IStorageRequestListener;
import fr.cnes.regards.modules.filecatalog.dto.request.FileCopyRequestDto;
import fr.cnes.regards.modules.filecatalog.dto.request.FileDeletionRequestDto;
import fr.cnes.regards.modules.filecatalog.dto.request.FileReferenceRequestDto;
import fr.cnes.regards.modules.filecatalog.dto.request.FileStorageRequestDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
    public RequestInfo copy(FileCopyRequestDto file) {
        RequestInfo requestInfo = RequestInfo.build();
        publisher.publish(new FilesCopyEvent(file, requestInfo.getGroupId()));
        return requestInfo;
    }

    @Override
    public Collection<RequestInfo> copy(Collection<FileCopyRequestDto> files) {
        return publish(FilesCopyEvent::new, files, FilesCopyEvent.MAX_REQUEST_PER_GROUP);
    }

    @Override
    public RequestInfo delete(FileDeletionRequestDto file) {
        RequestInfo requestInfo = RequestInfo.build();
        publisher.publish(new FilesDeletionEvent(file, requestInfo.getGroupId()));
        return requestInfo;
    }

    @Override
    public Collection<RequestInfo> delete(Collection<FileDeletionRequestDto> files) {
        return publish(FilesDeletionEvent::new, files, FilesDeletionEvent.MAX_REQUEST_PER_GROUP);
    }

    @Override
    public RequestInfo reference(FileReferenceRequestDto file) {
        RequestInfo requestInfo = RequestInfo.build();
        publisher.publish(new FilesReferenceEvent(file, requestInfo.getGroupId()));
        return requestInfo;
    }

    @Override
    public Collection<RequestInfo> reference(Collection<FileReferenceRequestDto> files) {
        return publish(FilesReferenceEvent::new, files, FilesReferenceEvent.MAX_REQUEST_PER_GROUP);
    }

    @Override
    public void storeRetry(RequestInfo requestInfo) {
        publisher.publish(FilesRetryRequestEvent.buildStorageRetry(requestInfo.getGroupId()));
    }

    @Override
    public void storeRetry(Collection<String> owners) {
        publisher.publish(FilesRetryRequestEvent.buildStorageRetry(owners));
    }

    @Override
    public void availabilityRetry(RequestInfo requestInfo) {
        publisher.publish(FilesRetryRequestEvent.buildAvailabilityRetry(requestInfo.getGroupId()));
    }

    @Override
    public RequestInfo store(FileStorageRequestDto file) {
        RequestInfo requestInfo = RequestInfo.build();
        publisher.publish(new FilesStorageRequestEvent(file, requestInfo.getGroupId()));
        return requestInfo;
    }

    @Override
    public Collection<RequestInfo> store(Collection<FileStorageRequestDto> files) {
        return publish(FilesStorageRequestEvent::new, files, FilesStorageRequestEvent.MAX_REQUEST_PER_GROUP);
    }

    @Override
    public Collection<RequestInfo> makeAvailable(Collection<String> checksums, int availabilityHours) {
        Collection<RequestInfo> requestInfos = Lists.newArrayList();
        // If number of files in the request is less than the maximum allowed by request then publish it
        if (checksums.size() <= FilesRestorationRequestEvent.MAX_REQUEST_PER_GROUP) {
            RequestInfo requestInfo = RequestInfo.build();
            publisher.publish(new FilesRestorationRequestEvent(checksums, availabilityHours, requestInfo.getGroupId()));
            requestInfos.add(requestInfo);
        } else {
            // Else publish as many requests as needed.
            List<String> group = Lists.newArrayList();
            Iterator<String> it = checksums.iterator();
            while (it.hasNext()) {
                group.add(it.next());
                if (group.size() >= FilesRestorationRequestEvent.MAX_REQUEST_PER_GROUP) {
                    RequestInfo requestInfo = RequestInfo.build();
                    publisher.publish(new FilesRestorationRequestEvent(group,
                                                                       availabilityHours,
                                                                       requestInfo.getGroupId()));
                    requestInfos.add(requestInfo);
                    group.clear();
                }
            }
            if (!group.isEmpty()) {
                RequestInfo requestInfo = RequestInfo.build();
                publisher.publish(new FilesRestorationRequestEvent(group, availabilityHours, requestInfo.getGroupId()));
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
