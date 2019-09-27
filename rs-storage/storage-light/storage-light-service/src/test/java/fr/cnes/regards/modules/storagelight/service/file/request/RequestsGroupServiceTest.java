/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.storagelight.service.file.request;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.compress.utils.Sets;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.storagelight.dao.IGroupRequestInfoRepository;
import fr.cnes.regards.modules.storagelight.dao.IRequestGroupRepository;
import fr.cnes.regards.modules.storagelight.domain.database.FileReferenceMetaInfo;
import fr.cnes.regards.modules.storagelight.domain.database.request.FileRequestStatus;
import fr.cnes.regards.modules.storagelight.domain.database.request.FileStorageRequest;
import fr.cnes.regards.modules.storagelight.domain.database.request.RequestGroup;
import fr.cnes.regards.modules.storagelight.domain.event.FileRequestType;
import fr.cnes.regards.modules.storagelight.service.AbstractStorageTest;

/**
 * Test class for service {@link RequestsGroupService}
 *
 * @author Sébastien Binda
 */
@ActiveProfiles({ "noscheduler" })
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=storage_groups_tests",
        "regards.storage.cache.path=target/cache" })
public class RequestsGroupServiceTest extends AbstractStorageTest {

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
    }

    @Test
    public void checkGroupDone() {
        for (FileRequestType type : FileRequestType.values()) {
            String groupId = UUID.randomUUID().toString();
            // Grant a group requests
            reqGrpService.granted(groupId, type, 2);
            // Simulate a request ends success
            reqGrpService.requestSuccess(groupId, type, UUID.randomUUID().toString(), ONLINE_CONF_LABEL, null);
            // Simulate a requests ends error
            reqGrpService.requestError(groupId, type, UUID.randomUUID().toString(), ONLINE_CONF_LABEL, null);
            // Group should be created
            Assert.assertTrue("Error during group request creation", reqGrpRepository.findById(groupId).isPresent());
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
        Optional<FileStorageRequest> oReq = storageReqService
                .create(Sets.newHashSet("owner"),
                        new FileReferenceMetaInfo(checksum, "UUID", "file.test", 0L, MediaType.APPLICATION_JSON),
                        "file://somewhere/file.test", destStorage, Optional.empty(), FileRequestStatus.PENDING,
                        groupId);
        Assert.assertTrue("Request should be created", oReq.isPresent());
        Assert.assertEquals("Requests should be pending", FileRequestStatus.PENDING,
                            storageReqService.search(destStorage, checksum).get().getStatus());
        RequestGroup grp = RequestGroup.build(groupId, FileRequestType.STORAGE);
        grp.setCreationDate(OffsetDateTime.now().minusDays(3));
        reqGrpRepository.save(grp);
        Assert.assertTrue("Error during group request creation", reqGrpRepository.findById(groupId).isPresent());
        reqGrpService.checkRequestsGroupsDone();
        Assert.assertFalse("Request group should be deleted as no it is expired are associated",
                           reqGrpRepository.findById(groupId).isPresent());
        Assert.assertEquals("Requests should be error", FileRequestStatus.ERROR,
                            storageReqService.search(destStorage, checksum).get().getStatus());
    }

    @Test
    public void checkGroupPending() {
        String groupId = UUID.randomUUID().toString();
        String destStorage = ONLINE_CONF_LABEL;
        String checksum = UUID.randomUUID().toString();
        Optional<FileStorageRequest> oReq = storageReqService
                .create(Sets.newHashSet("owner"),
                        new FileReferenceMetaInfo(checksum, "UUID", "file.test", 0L, MediaType.APPLICATION_JSON),
                        "file://somewhere/file.test", destStorage, Optional.empty(), FileRequestStatus.PENDING,
                        groupId);
        Assert.assertTrue("Request should be created", oReq.isPresent());
        Assert.assertEquals("Requests should be pending", FileRequestStatus.PENDING,
                            storageReqService.search(destStorage, checksum).get().getStatus());
        reqGrpService.granted(groupId, FileRequestType.STORAGE, 1);
        Assert.assertTrue("Error during group request creation", reqGrpRepository.findById(groupId).isPresent());
        reqGrpService.checkRequestsGroupsDone();
        Assert.assertTrue("Request group should still exists as it is not expired",
                          reqGrpRepository.findById(groupId).isPresent());
        Assert.assertEquals("Requests should still be pending as group is not expired", FileRequestStatus.PENDING,
                            storageReqService.search(destStorage, checksum).get().getStatus());
    }

}
