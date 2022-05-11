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
package fr.cnes.regards.modules.storage.service.file.request;

import java.time.OffsetDateTime;
import java.util.*;

import fr.cnes.regards.modules.storage.domain.database.request.FileStorageRequest;
import org.apache.commons.compress.utils.Sets;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.storage.dao.IGroupRequestInfoRepository;
import fr.cnes.regards.modules.storage.dao.IRequestGroupRepository;
import fr.cnes.regards.modules.storage.domain.database.FileReferenceMetaInfo;
import fr.cnes.regards.modules.storage.domain.database.request.FileRequestStatus;
import fr.cnes.regards.modules.storage.domain.database.request.RequestGroup;
import fr.cnes.regards.modules.storage.domain.database.request.RequestResultInfo;
import fr.cnes.regards.modules.storage.domain.dto.request.FileStorageRequestDTO;
import fr.cnes.regards.modules.storage.domain.event.FileRequestType;
import fr.cnes.regards.modules.storage.domain.flow.StorageFlowItem;
import fr.cnes.regards.modules.storage.service.AbstractStorageIT;

/**
 * Test class for service {@link RequestsGroupService}
 *
 * @author Sébastien Binda
 */
@ActiveProfiles({ "noscheduler" })
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=storage_groups_tests"},
        locations = { "classpath:application-test.properties" })
public class RequestsGroupServiceIT extends AbstractStorageIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestsGroupServiceIT.class);

    private static final  String SESSION_OWNER_1 = "SOURCE 1";

    private static final String SESSION_1 = "SESSION 1";

    @Autowired
    private RequestsGroupService reqGrpService;

    @Autowired
    private IRequestGroupRepository reqGrpRepository;

    @Autowired
    private IGroupRequestInfoRepository reqInfoRepo;

    @Autowired
    private FileStorageRequestService storageReqService;

    @Before
    public void initialize() throws ModuleException {
        super.init();
        reqGrpRepository.deleteAll();
    }

    @Test
    public void testPerfCheckGrp() {

        for (int i = 0; i < 2000; i++) {
            // Simulate a request ends success
            String groupId = UUID.randomUUID().toString();

            // Simulate a running request
            if (i < 1000) {
                FileStorageRequest request = storageReqService
                        .createNewFileStorageRequest(Sets.newHashSet("someone"),
                                                     new FileReferenceMetaInfo(UUID.randomUUID().toString(), "MD5",
                                                                               "plop", 10L,
                                                                               MediaType.APPLICATION_ATOM_XML), groupId,
                                                     ONLINE_CONF_LABEL, null, groupId, Optional.empty(),
                                                     Optional.empty(), SESSION_OWNER_1, SESSION_1);
            }
            FileStorageRequest request = storageReqService
                        .createNewFileStorageRequest(Sets.newHashSet("someone"),
                                                     new FileReferenceMetaInfo(UUID.randomUUID().toString(), "MD5",
                                                                               "plop", 10L,
                                                                               MediaType.APPLICATION_ATOM_XML), groupId,
                                                     ONLINE_CONF_LABEL, null, groupId, Optional.of("toto la belle erreur"),
                                                     Optional.of(FileRequestStatus.ERROR), SESSION_OWNER_1, SESSION_1);
            // Grant a group requests
            reqGrpService.granted(groupId, FileRequestType.STORAGE, 5, OffsetDateTime.now().plusDays(120));

            reqGrpService.requestSuccess(groupId, FileRequestType.STORAGE, UUID.randomUUID().toString(),
                                         ONLINE_CONF_LABEL, null, Sets.newHashSet("someone"), null);
            reqGrpService.requestSuccess(groupId, FileRequestType.STORAGE, UUID.randomUUID().toString(),
                                         ONLINE_CONF_LABEL, null, Sets.newHashSet("someone"), null);
            reqGrpService.requestSuccess(groupId, FileRequestType.STORAGE, UUID.randomUUID().toString(),
                                         ONLINE_CONF_LABEL, null, Sets.newHashSet("someone"), null);
            reqGrpService.requestSuccess(groupId, FileRequestType.STORAGE, UUID.randomUUID().toString(),
                                         ONLINE_CONF_LABEL, null, Sets.newHashSet("someone"), null);
            if (i >= 10) {
                reqGrpService.requestSuccess(groupId, FileRequestType.STORAGE, UUID.randomUUID().toString(),
                                             ONLINE_CONF_LABEL, null, Sets.newHashSet("someone"), null);
            }
        }
        long start = System.currentTimeMillis();
        reqGrpService.checkRequestsGroupsDone();
        LOGGER.info("DONE in {} ms", System.currentTimeMillis() - start);
    }

    @Test
    public void checkGroupDone() {
        for (FileRequestType type : FileRequestType.values()) {
            String groupId = UUID.randomUUID().toString();
            // Grant a group requests
            reqGrpService.granted(groupId, type, 2, OffsetDateTime.now().plusSeconds(120));
            // Simulate a request ends success
            reqGrpService.requestSuccess(groupId, type, UUID.randomUUID().toString(), ONLINE_CONF_LABEL, null,
                                         Sets.newHashSet("someone"), null);
            // Simulate a requests ends error
            reqGrpService.requestError(groupId, type, UUID.randomUUID().toString(), ONLINE_CONF_LABEL, null,
                                       Sets.newHashSet("someone"), null);
            // Group should be created
            Assert.assertTrue("Error during group request creation", reqGrpRepository.findById(groupId).isPresent());
            Assert.assertEquals("There be requests infos for expired group", 2, reqInfoRepo.count());
            // Check group is terminated
            reqGrpService.checkRequestsGroupsDone();
            // Group should not exists anymore
            Assert.assertFalse("Request group should be deleted as no requests are associated",
                               reqGrpRepository.findById(groupId).isPresent());
            // No request info should remains
            Assert.assertTrue("There should ne remaining request infos in success",
                              reqInfoRepo.findByGroupIdAndError(groupId, false).isEmpty());
            Assert.assertTrue("There should ne remaining request infos in error",
                              reqInfoRepo.findByGroupIdAndError(groupId, true).isEmpty());

        }
    }

    @Test
    public void checkGroupExpired() {
        String groupId = UUID.randomUUID().toString();
        String destStorage = ONLINE_CONF_LABEL;
        String checksum = UUID.randomUUID().toString();
        List<StorageFlowItem> items = new ArrayList<>();

        // 1. Run a storage request
        items.add(StorageFlowItem.build(FileStorageRequestDTO.build("filename", checksum, "UUID",
                                                                    MediaType.APPLICATION_JSON.toString(), "owner",
                                                                    SESSION_OWNER_1, SESSION_1,
                                                                    "file://somewhere/file.test", destStorage,
                                                                    Optional.empty()), groupId));
        storageReqService.store(items);

        // 2. Simulate response info added for this group
        reqInfoRepo.save(new RequestResultInfo(groupId, FileRequestType.STORAGE, checksum, destStorage, null,
                Sets.newHashSet("owner")));

        Assert.assertEquals("Requests should be pending", FileRequestStatus.TO_DO,
                            storageReqService.search(destStorage, checksum).stream().findFirst().get().getStatus());

        Optional<RequestGroup> oReqGroup = reqGrpRepository.findById(groupId);
        Assert.assertEquals("There should be a request info created", 1, reqInfoRepo.count());
        Assert.assertTrue("There should be a requests group created", oReqGroup.isPresent());

        // Update expiration date to simulate group expired
        RequestGroup reqGroup = oReqGroup.get();
        reqGroup.setExpirationDate(OffsetDateTime.now().minusSeconds(10));
        reqGrpRepository.save(reqGroup);

        // Run check requests groups
        reqGrpService.checkRequestsGroupsDone();
        oReqGroup = reqGrpRepository.findById(groupId);
        Assert.assertFalse("Request group should be deleted cause the group is expired", oReqGroup.isPresent());
        Assert.assertEquals("Requests should be error", FileRequestStatus.ERROR,
                            storageReqService.search(destStorage, checksum).stream().findFirst().get().getStatus());
        Assert.assertEquals("There not be requests infos for expired group", 0, reqInfoRepo.count());
    }

    @Test
    public void checkGroupPending() {
        String groupId = UUID.randomUUID().toString();
        String destStorage = ONLINE_CONF_LABEL;
        String checksum = UUID.randomUUID().toString();
        storageReqService.createNewFileStorageRequest(Sets.newHashSet("owner"),
                                                      new FileReferenceMetaInfo(checksum, "UUID", "file.test", 0L,
                                                                                MediaType.APPLICATION_JSON),
                                                      "file://somewhere/file.test", destStorage, Optional.empty(),
                                                      groupId, Optional.empty(), Optional.of(FileRequestStatus.PENDING),
                                                      SESSION_OWNER_1, SESSION_1);
        Assert.assertEquals("Requests should be pending", FileRequestStatus.PENDING,
                            storageReqService.search(destStorage, checksum).stream().findFirst().get().getStatus());
        reqGrpService.granted(groupId, FileRequestType.STORAGE, 1, OffsetDateTime.now().plusSeconds(120));
        Assert.assertTrue("Error during group request creation", reqGrpRepository.findById(groupId).isPresent());
        reqGrpService.checkRequestsGroupsDone();
        Assert.assertTrue("Request group should still exists as it is not expired",
                          reqGrpRepository.findById(groupId).isPresent());
        Assert.assertEquals("Requests should still be pending as group is not expired", FileRequestStatus.PENDING,
                            storageReqService.search(destStorage, checksum).stream().findFirst().get().getStatus());
    }

}
