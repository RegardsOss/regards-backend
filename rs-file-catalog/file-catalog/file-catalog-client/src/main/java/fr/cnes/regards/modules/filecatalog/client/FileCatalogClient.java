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
package fr.cnes.regards.modules.filecatalog.client;

import com.google.common.collect.Lists;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.modules.fileaccess.dto.request.FileCopyDto;
import fr.cnes.regards.modules.fileaccess.dto.request.FileDeletionDto;
import fr.cnes.regards.modules.fileaccess.dto.request.FileReferenceRequestDto;
import fr.cnes.regards.modules.fileaccess.dto.request.FileStorageRequestDto;
import fr.cnes.regards.modules.filecatalog.amqp.input.*;

import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;

/**
 * Client interface for requesting the file catalog service
 * <p>
 * Client requests are done asynchronously.
 * To listen to the feedback messages, you have to implement your own message handler listening to {@link fr.cnes.regards.modules.filecatalog.amqp.output.FileReferenceEvent}.
 * Be sure to check that the message is intended for you by validating the owner.
 * Look at {@link fr.cnes.regards.modules.filecatalog.client.listener.IStorageRequestListener} to adapt your behavior after global request finished status.
 * Look at {@link fr.cnes.regards.modules.filecatalog.client.listener.IStorageFileListener} to adapt your behavior after each file modification done.
 *
 * @author Thibaud Michaudel
 **/
public class FileCatalogClient implements IFileCatalogClient {

    private final IPublisher publisher;

    public FileCatalogClient(IPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public RequestInfo copy(FileCopyDto file) {
        RequestInfo requestInfo = RequestInfo.build();
        publisher.publish(new FilesCopyEvent(file, requestInfo.getGroupId()));
        return requestInfo;
    }

    @Override
    public Collection<RequestInfo> copy(Collection<FileCopyDto> files) {
        return publish(FilesCopyEvent::new, files, FilesCopyEvent.MAX_REQUEST_PER_GROUP);
    }

    @Override
    public RequestInfo delete(FileDeletionDto file) {
        RequestInfo requestInfo = RequestInfo.build();
        publisher.publish(new FilesDeletionEvent(file, requestInfo.getGroupId()));
        return requestInfo;
    }

    @Override
    public Collection<RequestInfo> delete(Collection<FileDeletionDto> files) {
        return publish(FilesDeletionEvent::new, files, FilesDeletionEvent.MAX_REQUEST_PER_GROUP);
    }

    @Override
    public RequestInfo reference(FileReferenceRequestDto file) {
        RequestInfo requestInfo = RequestInfo.build();
        publisher.publish(new FilesStorageRequestEvent(FileStorageRequestDto.buildReference(file),
                                                       requestInfo.getGroupId()));

        return requestInfo;
    }

    @Override
    public Collection<RequestInfo> reference(Collection<FileReferenceRequestDto> files) {
        return publish(FilesStorageRequestEvent::new,
                       files.stream().map(FileStorageRequestDto::buildReference).toList(),
                       FilesReferenceEvent.MAX_REQUEST_PER_GROUP);
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
            List<List<String>> checksumsGroups = Lists.partition(checksums.stream().toList(),
                                                                 FilesRestorationRequestEvent.MAX_REQUEST_PER_GROUP);
            checksumsGroups.forEach(group -> {
                RequestInfo requestInfo = RequestInfo.build();
                publisher.publish(new FilesRestorationRequestEvent(group, availabilityHours, requestInfo.getGroupId()));
                requestInfos.add(requestInfo);
            });
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
        List<List<T>> filesGroups = Lists.partition(files.stream().toList(), maxFilesPerRequest);
        filesGroups.forEach(group -> {
            RequestInfo requestInfo = RequestInfo.build();
            publisher.publish(func.apply(group, requestInfo.getGroupId()));
            requestInfos.add(requestInfo);
        });
        return requestInfos;
    }
}
