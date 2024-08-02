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

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.fileaccess.dto.FileRequestStatus;
import fr.cnes.regards.modules.storage.dao.IFileReferenceRepository;
import fr.cnes.regards.modules.storage.domain.database.FileLocation;
import fr.cnes.regards.modules.storage.domain.database.FileReference;
import fr.cnes.regards.modules.storage.domain.database.FileReferenceMetaInfo;
import fr.cnes.regards.modules.storage.domain.database.request.FileStorageRequestAggregation;
import fr.cnes.regards.modules.storage.service.AbstractStorageIT;
import fr.cnes.regards.modules.storage.service.file.FileReferenceService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.*;
import java.util.stream.IntStream;

/**
 * @author Sébastien Binda
 **/
@ActiveProfiles({ "noscheduler", "nojobs" })
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=storage_status_tests" },
                    locations = { "classpath:application-test.properties" })
public class RequestStatusServiceIT extends AbstractStorageIT {

    @Autowired
    RequestStatusService requestStatusService;

    @Autowired
    IFileReferenceRepository fileReferenceRepository;

    @Autowired
    FileReferenceService fileReferenceService;

    @Autowired
    FileStorageRequestService fileStorageRequestService;

    @Before
    public void init() throws ModuleException {
        super.init();
    }

    @Test
    public void test_delayed_requests() {
        // Given
        List<FileStorageRequestAggregation> newRequests = new ArrayList<>();
        String id = UUID.randomUUID().toString();
        IntStream.range(0, 10)
                 .forEach(i -> newRequests.add(generateRandomStorageRequest(id, id, FileRequestStatus.DELAYED)));
        fileStorageRequestRepo.saveAll(newRequests);

        // With try to un delayed possible requests
        requestStatusService.checkDelayedStorageRequests(null);

        // Then only one request should be un delayed
        Assert.assertEquals(0L,
                            fileStorageRequestRepo.countByStorageAndStatus(ONLINE_CONF_LABEL, FileRequestStatus.DELAYED)
                                                  .longValue());
        // Then there is one TO_DO request containing all 10 requests
        List<FileStorageRequestAggregation> requests = fileStorageRequestRepo.findAllByStorageAndStatus(
            ONLINE_CONF_LABEL,
            FileRequestStatus.TO_DO,
            Pageable.ofSize(10)).getContent();

        Assert.assertEquals(1L, requests.size());
        Assert.assertEquals(10, requests.get(0).getGroupIds().size());
        Assert.assertEquals(10, requests.get(0).getOwners().size());
    }

    @Test
    public void test_delayed_success_requests() {
        // Given
        String id = UUID.randomUUID().toString();
        FileReferenceMetaInfo metaInfo = new FileReferenceMetaInfo(id,
                                                                   "MD5",
                                                                   id,
                                                                   1000L,
                                                                   MediaType.valueOf("text/plain"));
        String url = "file:///test/toto/" + id;

        // Save existing file reference
        Long existingFileId = fileReferenceRepository.save(new FileReference("oldOwner",
                                                                             metaInfo,
                                                                             new FileLocation(ONLINE_CONF_LABEL,
                                                                                              url,
                                                                                              false))).getId();

        // Create delayed request
        FileStorageRequestAggregation fr = new FileStorageRequestAggregation("newOwner",
                                                                             metaInfo,
                                                                             url,
                                                                             ONLINE_CONF_LABEL,
                                                                             Optional.empty(),
                                                                             "groupId",
                                                                             "sessionOwner",
                                                                             "session");
        fr.setStatus(FileRequestStatus.DELAYED);
        fileStorageRequestRepo.save(fr);

        // Resume delayed request
        requestStatusService.checkDelayedStorageRequests(fileStorageRequestService);

        // Then
        // Check that the new owner has been added to the file reference
        Collection<String> owners = fileReferenceService.getOwners(existingFileId);

        Assert.assertEquals("There should be 2 owners", 2, owners.size());
    }
}
