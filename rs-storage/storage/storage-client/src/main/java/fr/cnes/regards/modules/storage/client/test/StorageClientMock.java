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
package fr.cnes.regards.modules.storage.client.test;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.modules.filecatalog.amqp.output.FileRequestsGroupEvent;
import fr.cnes.regards.modules.filecatalog.client.RequestInfo;
import fr.cnes.regards.modules.filecatalog.dto.FileLocationDto;
import fr.cnes.regards.modules.filecatalog.dto.FileReferenceDto;
import fr.cnes.regards.modules.filecatalog.dto.FileReferenceMetaInfoDto;
import fr.cnes.regards.modules.filecatalog.dto.FileRequestType;
import fr.cnes.regards.modules.filecatalog.dto.request.*;
import fr.cnes.regards.modules.storage.client.IStorageClient;
import fr.cnes.regards.modules.storage.client.StorageClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Provide a bean to replace the behavior of the {@link StorageClient} while testing
 *
 * @author Léo Mieulet
 */
@Profile("StorageClientMock")
@Component
@Primary
@Transactional
public class StorageClientMock implements IStorageClient {

    @Autowired
    private IPublisher publisher;

    private Optional<Boolean> shouldReturnGranted = Optional.empty();

    private Optional<Boolean> shouldReturnSuccess = Optional.empty();

    private final static String UNSUPORTED = "Not implemented yet !";

    /**
     * @param shouldReturnGranted when true return granted, otherwise denied
     * @param shouldReturnSuccess when true return success, otherwise error
     */
    public void setBehavior(boolean shouldReturnGranted, boolean shouldReturnSuccess) {
        this.shouldReturnGranted = Optional.of(shouldReturnGranted);
        this.shouldReturnSuccess = Optional.of(shouldReturnSuccess);
    }

    @Override
    public RequestInfo store(FileStorageRequestDto file) {
        return store(Sets.newHashSet(file).iterator().next());
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
    public Collection<RequestInfo> store(Collection<FileStorageRequestDto> files) {
        checkInit();
        RequestInfo requestInfo = RequestInfo.build();

        // Send the first event
        FileGroupRequestStatus firstStatus;
        if (shouldReturnGranted.get()) {
            firstStatus = FileGroupRequestStatus.GRANTED;
        } else {
            firstStatus = FileGroupRequestStatus.DENIED;
        }

        List<RequestResultInfoDto> requestInfos = new ArrayList<>();
        for (FileStorageRequestDto file : files) {
            FileReferenceMetaInfoDto fileReferenceMetaInfoDto = new FileReferenceMetaInfoDto(file.getChecksum(),
                                                                                             file.getAlgorithm(),
                                                                                             file.getFileName(),
                                                                                             1000L,
                                                                                             null,
                                                                                             null,
                                                                                             file.getMimeType(),
                                                                                             null);
            FileLocationDto FileLocationDto = new FileLocationDto(file.getStorage(),
                                                                  "http://somedomain.com/api/v1/storage/file/2");

            FileReferenceDto reference = new FileReferenceDto(OffsetDateTime.now(),
                                                              fileReferenceMetaInfoDto,
                                                              FileLocationDto,
                                                              Stream.of(file.getOwner()).collect(Collectors.toSet()));

            RequestResultInfoDto resultInfo = RequestResultInfoDto.build(requestInfo.getGroupId(),
                                                                         file.getChecksum(),
                                                                         file.getStorage(),
                                                                         file.getOptionalSubDirectory().orElse(null),
                                                                         Sets.newHashSet(file.getOwner()),
                                                                         reference,
                                                                         null);

            requestInfos.add(resultInfo);

        }

        publisher.publish(FileRequestsGroupEvent.build(requestInfo.getGroupId(),
                                                       FileRequestType.STORAGE,
                                                       firstStatus,
                                                       requestInfos));

        // Send the second event if the first one is GRANTED
        if (shouldReturnGranted.get()) {
            FileGroupRequestStatus secondStatus;
            if (shouldReturnSuccess.get()) {
                secondStatus = FileGroupRequestStatus.SUCCESS;
            } else {
                secondStatus = FileGroupRequestStatus.ERROR;
            }
            publisher.publish(FileRequestsGroupEvent.build(requestInfo.getGroupId(),
                                                           FileRequestType.STORAGE,
                                                           secondStatus,
                                                           requestInfos));
        }

        return Sets.newHashSet(requestInfo);
    }

    @Override
    public void storeRetry(RequestInfo requestInfo) {
        // Not implemented yet
        throw new UnsupportedOperationException(UNSUPORTED);
    }

    @Override
    public void storeRetry(Collection<String> owners) {
        // Not implemented yet
        throw new UnsupportedOperationException(UNSUPORTED);
    }

    @Override
    public void availabilityRetry(RequestInfo requestInfo) {
        // Not implemented yet
        throw new UnsupportedOperationException(UNSUPORTED);
    }

    @Override
    public RequestInfo reference(FileReferenceRequestDto file) {
        RequestInfo requestInfo = RequestInfo.build();

        //TODO call storage
        return requestInfo;
    }

    @Override
    public Collection<RequestInfo> reference(Collection<FileReferenceRequestDto> files) {
        Collection<RequestInfo> info = new ArrayList<>();
        info.add(RequestInfo.build());
        return info;
    }

    @Override
    public RequestInfo delete(FileDeletionRequestDto file) {
        checkInit();
        RequestInfo requestInfo = RequestInfo.build();

        // Send the first event
        FileGroupRequestStatus firstStatus;
        if (shouldReturnGranted.get()) {
            firstStatus = FileGroupRequestStatus.GRANTED;
        } else {
            firstStatus = FileGroupRequestStatus.DENIED;
        }

        FileReferenceMetaInfoDto fileReferenceMetaInfoDto = new FileReferenceMetaInfoDto(file.getChecksum(),
                                                                                         "some algo",
                                                                                         "some file",
                                                                                         1000L,
                                                                                         null,
                                                                                         null,
                                                                                         "application/pdf",
                                                                                         null);

        FileLocationDto FileLocationDto = new FileLocationDto(file.getStorage(),
                                                              "http://somedomain.com/api/v1/storage/file/2");

        FileReferenceDto reference = new FileReferenceDto(OffsetDateTime.now(),
                                                          fileReferenceMetaInfoDto,
                                                          FileLocationDto,
                                                          Stream.of(file.getOwner()).collect(Collectors.toSet()));

        RequestResultInfoDto resultInfo = RequestResultInfoDto.build(requestInfo.getGroupId(),
                                                                     file.getChecksum(),
                                                                     file.getStorage(),
                                                                     null,
                                                                     Sets.newHashSet(file.getOwner()),
                                                                     reference,
                                                                     null);

        List<RequestResultInfoDto> requestInfos = Collections.singletonList(resultInfo);
        publisher.publish(FileRequestsGroupEvent.build(requestInfo.getGroupId(),
                                                       FileRequestType.DELETION,
                                                       firstStatus,
                                                       requestInfos));

        // Send the second event if the first one is GRANTED
        if (shouldReturnGranted.get()) {
            FileGroupRequestStatus secondStatus;
            if (shouldReturnSuccess.get()) {
                secondStatus = FileGroupRequestStatus.SUCCESS;
            } else {
                secondStatus = FileGroupRequestStatus.ERROR;
            }
            publisher.publish(FileRequestsGroupEvent.build(requestInfo.getGroupId(),
                                                           FileRequestType.DELETION,
                                                           secondStatus,
                                                           requestInfos));
        }

        return requestInfo;
    }

    @Override
    public Collection<RequestInfo> delete(Collection<FileDeletionRequestDto> files) {
        return Sets.newHashSet(delete(files.iterator().next()));
    }

    @Override
    public RequestInfo copy(FileCopyRequestDto file) {
        // Not implemented yet
        throw new UnsupportedOperationException(UNSUPORTED);
    }

    @Override
    public Collection<RequestInfo> copy(Collection<FileCopyRequestDto> files) {
        // Not implemented yet
        throw new UnsupportedOperationException(UNSUPORTED);
    }

    @Override
    public Collection<RequestInfo> makeAvailable(Collection<String> checksums, int availabilityHours) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void cancelRequests(Collection<String> requestGroups) {
        // do nothing
    }

}
