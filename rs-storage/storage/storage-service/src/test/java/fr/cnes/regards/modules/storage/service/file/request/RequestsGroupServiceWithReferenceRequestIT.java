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
package fr.cnes.regards.modules.storage.service.file.request;

import fr.cnes.regards.framework.test.integration.RandomChecksumUtils;
import fr.cnes.regards.modules.fileaccess.dto.FileRequestStatus;
import fr.cnes.regards.modules.fileaccess.dto.FileRequestType;
import fr.cnes.regards.modules.fileaccess.dto.request.FileReferenceRequestDto;
import fr.cnes.regards.modules.filecatalog.amqp.input.FilesReferenceEvent;
import fr.cnes.regards.modules.storage.dao.IFileReferenceRequestRepository;
import fr.cnes.regards.modules.storage.domain.database.request.FileReferenceRequestAggregation;
import fr.cnes.regards.modules.storage.domain.database.request.RequestGroup;
import fr.cnes.regards.modules.storage.domain.database.request.RequestResultInfo;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static fr.cnes.regards.modules.storage.service.file.fixture.FileReferenceConstants.OWNER1;
import static fr.cnes.regards.modules.storage.service.file.fixture.FileReferenceConstants.OWNER2;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

/**
 * Test class for service {@link RequestsGroupService}
 *
 * @author Sébastien Binda
 */
@ActiveProfiles({ "noscheduler" })
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=storage_groups_tests" },
                    locations = { "classpath:application-test.properties" })
public class RequestsGroupServiceWithReferenceRequestIT extends AbstractRequestGroupServiceIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestsGroupServiceWithReferenceRequestIT.class);

    @Autowired
    private FileReferenceRequestService referenceRequestService;

    @Autowired
    private IFileReferenceRequestRepository referenceRequestRepository;

    @Override
    protected void saveNewRequest(String groupId, FileRequestStatus status, String errorCause) {

        final String storage = ONLINE_CONF_LABEL;

        final FileReferenceRequestAggregation request = new FileReferenceRequestAggregation(OWNER1,
                                                                                            newFileReferenceMetaInfo(),
                                                                                            "file://somewhere/file.test",
                                                                                            storage,
                                                                                            null,
                                                                                            groupId,
                                                                                            SESSION_OWNER_1,
                                                                                            SESSION_1);

        request.setStatus(status);
        request.setErrorCause(errorCause);
        referenceRequestRepository.save(request);
    }

    @Test
    public void checkGroupExpired() {
        // GIVEN An expired group
        final String groupId = UUID.randomUUID().toString();
        final String storage = ONLINE_CONF_LABEL;
        final String checksum = RandomChecksumUtils.generateRandomChecksum();

        // 1. Run a reference request
        final FileReferenceRequestDto referenceRequestDto = FileReferenceRequestDto.build("filename",
                                                                                          checksum,
                                                                                          "UUID",
                                                                                          MediaType.APPLICATION_JSON.toString(),
                                                                                          0L,
                                                                                          OWNER2,
                                                                                          storage,
                                                                                          "file://somewhere/file.test",
                                                                                          SESSION_OWNER_1,
                                                                                          SESSION_1);
        final List<FilesReferenceEvent> items = List.of(new FilesReferenceEvent(referenceRequestDto, groupId));
        referenceRequestService.createNewFileReferenceRequests(items);

        // 2. Simulate response info added for this group
        reqInfoRepo.save(new RequestResultInfo(groupId, FileRequestType.REFERENCE, checksum, storage, null, OWNERS2));

        Collection<FileReferenceRequestAggregation> requests = referenceRequestRepository.findByMetaInfoChecksumAndStorage(
            checksum,
            storage);
        assumeThat(requests).hasSize(1);
        FileReferenceRequestAggregation request = requests.iterator().next();
        assumeThat(request.getStatus()).as("Requests status should be TO_DO").isEqualTo(FileRequestStatus.TO_DO);

        RequestGroup reqGroup = reqGrpRepository.findById(groupId).orElse(null);
        assumeThat(reqInfoRepo.count()).as("There should be a request info created").isEqualTo(1);
        assumeThat(reqGroup).as("There should be a requests group created").isNotNull();

        // 3. Simulate group expiration
        reqGroup.setExpirationDate(OffsetDateTime.now().minusSeconds(10));
        reqGrpRepository.save(reqGroup);

        // WHEN check requests groups
        reqGrpService.checkRequestsGroupsDone();

        // THEN expect group to be removed
        reqGroup = reqGrpRepository.findById(groupId).orElse(null);
        assertThat(reqGroup).as("Request group should be deleted cause the group is expired").isNull();

        requests = referenceRequestRepository.findByMetaInfoChecksumAndStorage(checksum, storage);
        assumeThat(requests).hasSize(1);
        request = requests.iterator().next();
        assertThat(request.getStatus()).as("Requests status should be ERROR").isEqualTo(FileRequestStatus.ERROR);

        assertThat(reqInfoRepo.count()).as("There should be no requests infos for expired group").isEqualTo(0);
    }

    @Test
    public void checkGroupPending() {
        // GIVEN

        String groupId = UUID.randomUUID().toString();

        // a reference request in a PENDING status with a granted group
        String storage = ONLINE_CONF_LABEL;
        String checksum = RandomChecksumUtils.generateRandomChecksum();
        final FileReferenceRequestDto referenceRequestDto = FileReferenceRequestDto.build("file.test",
                                                                                          checksum,
                                                                                          "UUID",
                                                                                          MediaType.APPLICATION_JSON.toString(),
                                                                                          0L,
                                                                                          OWNER2,
                                                                                          storage,
                                                                                          "file://somewhere/file.test",
                                                                                          SESSION_OWNER_1,
                                                                                          SESSION_1);
        final List<FilesReferenceEvent> items = List.of(new FilesReferenceEvent(referenceRequestDto, groupId));
        referenceRequestService.createNewFileReferenceRequests(items);

        FileReferenceRequestAggregation request = referenceRequestRepository.findByStorageAndMetaInfoChecksum(storage,
                                                                                                              checksum)
                                                                            .orElse(null);

        request.setStatus(FileRequestStatus.TO_DO);
        referenceRequestRepository.save(request);

        // a granted group
        // reqGrpService.granted(groupId, FileRequestType.REFERENCE, 1, OffsetDateTime.now().plusSeconds(120));
        assumeThat(reqGrpRepository.findById(groupId)).as("Request Group %s should exists", groupId).isPresent();

        // WHEN check and remove all the terminated group
        reqGrpService.checkRequestsGroupsDone();

        // THEN
        // group is not terminated neither expired thus still exists
        assertThat(reqGrpRepository.findById(groupId)).as("Request Group %s should exists", groupId).isPresent();

        // request is still PENDING since the group is not expired
        request = referenceRequestRepository.findByStorageAndMetaInfoChecksum(storage, checksum).orElse(null);
        assertThat(request).isNotNull();
        assertThat(request.getStatus()).as("Request should still be PENDING as group is not expired")
                                       .isEqualTo(FileRequestStatus.TO_DO);

    }

}
