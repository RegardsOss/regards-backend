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
import fr.cnes.regards.modules.fileaccess.dto.request.FileStorageRequestDto;
import fr.cnes.regards.modules.filecatalog.amqp.input.FilesStorageRequestEvent;
import fr.cnes.regards.modules.storage.domain.database.FileReferenceMetaInfo;
import fr.cnes.regards.modules.storage.domain.database.request.FileStorageRequestAggregation;
import fr.cnes.regards.modules.storage.domain.database.request.RequestGroup;
import fr.cnes.regards.modules.storage.domain.database.request.RequestResultInfo;
import org.junit.Assert;
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
import java.util.Optional;
import java.util.UUID;

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
public class RequestsGroupServiceWithStorageRequestIT extends AbstractRequestGroupServiceIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestsGroupServiceWithStorageRequestIT.class);

    @Autowired
    private FileStorageRequestService storageReqService;

    @Test
    public void testPerfCheckGrp() {

        for (int i = 0; i < 2000; i++) {
            // Simulate a request ends success
            String groupId = UUID.randomUUID().toString();

            // Simulate a running request
            if (i < 1000) {
                saveNewRequest(groupId, null, null);
            }
            saveNewRequest(groupId, FileRequestStatus.ERROR, "toto la belle erreur");

            // Grant a group requests
            reqGrpService.granted(groupId, FileRequestType.STORAGE, 5, OffsetDateTime.now().plusDays(120));
            requestSuccess(groupId, FileRequestType.STORAGE);
            requestSuccess(groupId, FileRequestType.STORAGE);
            requestSuccess(groupId, FileRequestType.STORAGE);
            requestSuccess(groupId, FileRequestType.STORAGE);
            if (i >= 10) {
                requestSuccess(groupId, FileRequestType.STORAGE);
            }
        }
        long start = System.currentTimeMillis();
        reqGrpService.checkRequestsGroupsDone();
        LOGGER.info("DONE in {} ms", System.currentTimeMillis() - start);
    }

    @Override
    protected void saveNewRequest(String groupId, FileRequestStatus requestStatus, String errorCause) {

        final FileReferenceMetaInfo metaInfo = newFileReferenceMetaInfo();
        storageReqService.createNewFileStorageRequest(OWNERS1,
                                                      metaInfo,
                                                      groupId,
                                                      ONLINE_CONF_LABEL,
                                                      Optional.empty(),
                                                      groupId,
                                                      Optional.ofNullable(errorCause),
                                                      Optional.ofNullable(requestStatus),
                                                      SESSION_OWNER_1,
                                                      SESSION_1);
    }

    @Test
    public void checkGroupExpired() {
        // GIVEN An expired group
        final String groupId = UUID.randomUUID().toString();
        final String storage = ONLINE_CONF_LABEL;
        final String checksum = RandomChecksumUtils.generateRandomChecksum();

        // 1. Run a storage request
        final FileStorageRequestDto storageRequest = FileStorageRequestDto.build("filename",
                                                                                 checksum,
                                                                                 "UUID",
                                                                                 MediaType.APPLICATION_JSON.toString(),
                                                                                 "owner",
                                                                                 SESSION_OWNER_1,
                                                                                 SESSION_1,
                                                                                 "file://somewhere/file.test",
                                                                                 storage,
                                                                                 Optional.empty());
        final List<FilesStorageRequestEvent> items = List.of(new FilesStorageRequestEvent(storageRequest, groupId));
        storageReqService.store(items);

        // 2. Simulate response info added for this group
        reqInfoRepo.save(new RequestResultInfo(groupId, FileRequestType.STORAGE, checksum, storage, null, OWNERS2));

        Collection<FileStorageRequestAggregation> requests = storageReqService.search(storage, checksum);
        assumeThat(requests).hasSize(1);
        FileStorageRequestAggregation request = requests.iterator().next();
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

        requests = storageReqService.search(storage, checksum);
        assumeThat(requests).hasSize(1);
        request = requests.iterator().next();
        assertThat(request.getStatus()).as("Requests status should be ERROR").isEqualTo(FileRequestStatus.ERROR);

        assertThat(reqInfoRepo.count()).as("There should be no requests infos for expired group").isEqualTo(0);
    }

    @Test
    public void checkGroupPending() {
        String groupId = UUID.randomUUID().toString();
        String destStorage = ONLINE_CONF_LABEL;
        String checksum = RandomChecksumUtils.generateRandomChecksum();
        final FileReferenceMetaInfo fileMetaInfo = new FileReferenceMetaInfo(checksum,
                                                                             "UUID",
                                                                             "file.test",
                                                                             0L,
                                                                             MediaType.APPLICATION_JSON);
        storageReqService.createNewFileStorageRequest(OWNERS2,
                                                      fileMetaInfo,
                                                      "file://somewhere/file.test",
                                                      destStorage,
                                                      Optional.empty(),
                                                      groupId,
                                                      Optional.empty(),
                                                      Optional.of(FileRequestStatus.PENDING),
                                                      SESSION_OWNER_1,
                                                      SESSION_1);

        Assert.assertEquals("Requests should be pending",
                            FileRequestStatus.PENDING,
                            storageReqService.search(destStorage, checksum).stream().findFirst().get().getStatus());
        reqGrpService.granted(groupId, FileRequestType.STORAGE, 1, OffsetDateTime.now().plusSeconds(120));
        Assert.assertTrue("Error during group request creation", reqGrpRepository.findById(groupId).isPresent());
        reqGrpService.checkRequestsGroupsDone();
        Assert.assertTrue("Request group should still exists as it is not expired",
                          reqGrpRepository.findById(groupId).isPresent());
        Assert.assertEquals("Requests should still be pending as group is not expired",
                            FileRequestStatus.PENDING,
                            storageReqService.search(destStorage, checksum).stream().findFirst().get().getStatus());
    }

}
