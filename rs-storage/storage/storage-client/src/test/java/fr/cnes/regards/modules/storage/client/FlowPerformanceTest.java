/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.util.Set;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.annotation.DirtiesContext.HierarchyMode;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import fr.cnes.regards.framework.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.modules.storage.dao.IFileReferenceRepository;
import fr.cnes.regards.modules.storage.dao.IGroupRequestInfoRepository;
import fr.cnes.regards.modules.storage.domain.database.FileLocation;
import fr.cnes.regards.modules.storage.domain.database.FileReference;
import fr.cnes.regards.modules.storage.domain.database.FileReferenceMetaInfo;
import fr.cnes.regards.modules.storage.domain.dto.request.FileReferenceRequestDTO;
import fr.cnes.regards.modules.storage.service.file.handler.FileReferenceEventHandler;

/**
 * Performances tests for creating and store new file references.
 * @author Sébastien Binda
 */
@ActiveProfiles(value = { "default", "test", "testAmqp", "storageTest" }, inheritProfiles = false)
@DirtiesContext(classMode = ClassMode.AFTER_CLASS, hierarchyMode = HierarchyMode.EXHAUSTIVE)
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=storage_client_tests",
        "regards.storage.cache.path=target/cache", "regards.amqp.enabled=true", "regards.storage.schedule.delay:1000",
        "regards.storage.location.schedule.delay:600000", "regards.storage.reference.items.bulk.size:10" },
        locations = { "classpath:application-local.properties" })
@Ignore("Performances tests")
public class FlowPerformanceTest extends AbstractRegardsTransactionalIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowPerformanceTest.class);

    @Autowired
    protected FileReferenceEventHandler fileRefEventHandler;

    @Autowired
    protected IFileReferenceRepository fileRefRepo;

    @Autowired
    protected IJobInfoRepository jobInfoRepo;

    @Autowired
    protected IGroupRequestInfoRepository grpReqInfoRepo;

    @Autowired
    private StorageClient client;

    @Autowired
    private StorageListener listener;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Before
    public void init() {
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        grpReqInfoRepo.deleteAll();
        jobInfoRepo.deleteAll();

        // populate();

    }

    private void populate() {
        // Populate
        if (fileRefRepo.count() == 0) {
            // Insert many refs
            Set<FileReference> toSave = Sets.newHashSet();
            for (Long i = 0L; i < 1_000_000; i++) {
                int count = 1;
                FileReferenceMetaInfo metaInfo = new FileReferenceMetaInfo(UUID.randomUUID().toString(), "UUID",
                        "file_" + i + ".test", i, MediaType.APPLICATION_OCTET_STREAM);
                FileLocation location = new FileLocation("storage-" + count, "storage://plop/file");
                FileReference fileRef = new FileReference(Lists.newArrayList("owner"), metaInfo, location);
                toSave.add(fileRef);
                if (toSave.size() >= 10_000) {
                    count++;
                    long start = System.currentTimeMillis();
                    fileRefRepo.saveAll(toSave);
                    LOGGER.info("Saves {} done in {}ms", toSave.size(), System.currentTimeMillis() - start);
                    toSave.clear();
                }
            }

            // Init nearline stored refs
            long start = System.currentTimeMillis();
            fileRefRepo.saveAll(toSave);
            LOGGER.info("Saves {} done in {}ms", toSave.size(), System.currentTimeMillis() - start);
        }
    }

    private void waitRequestEnds(int nbrequests, int maxDurationSec) throws InterruptedException {
        int loopDuration = 2_000;
        int nbLoop = ((maxDurationSec * 1000) / loopDuration);
        int loop = 0;
        while ((listener.getNbRequestEnds() < nbrequests) && (loop < nbLoop)) {
            loop++;
            Thread.sleep(loopDuration);
        }
        if (listener.getNbRequestEnds() < nbrequests) {
            String message = String.format("Number of requests requested for end not reached %d/%d",
                                           listener.getNbRequestEnds(), nbrequests);
            Assert.fail(message);
        }
    }

    @Test
    public void referenceFiles() throws InterruptedException {
        LOGGER.info(" --------     REFERENCE TEST     --------- ");
        String refStorage = "storage-1";
        int nbRrequests = 0;
        long start = System.currentTimeMillis();
        for (int i = 0; i < 5000; i++) {
            String newOwner = "owner-" + UUID.randomUUID().toString();
            String sessionOwner = "source-" + i;
            String session = "session-" + i;

            Set<FileReferenceRequestDTO> requests = Sets.newHashSet();
            requests.add(FileReferenceRequestDTO.build("quicklook.1-" + i, UUID.randomUUID().toString(), "MD5",
                                                       "application/octet-stream", 10L, newOwner, refStorage,
                                                       "file://storage/location/quicklook1", sessionOwner, session));
            requests.add(FileReferenceRequestDTO.build("quicklook.2-" + i, UUID.randomUUID().toString(), "MD5",
                                                       "application/octet-stream", 10L, newOwner, refStorage,
                                                       "file://storage/location/quicklook1", sessionOwner, session));
            requests.add(FileReferenceRequestDTO.build("quicklook.3-" + i, UUID.randomUUID().toString(), "MD5",
                                                       "application/octet-stream", 10L, newOwner, refStorage,
                                                       "file://storage/location/quicklook1", sessionOwner, session));
            requests.add(FileReferenceRequestDTO.build("quicklook.4-" + i, UUID.randomUUID().toString(), "MD5",
                                                       "application/octet-stream", 10L, newOwner, refStorage,
                                                       "file://storage/location/quicklook1", sessionOwner, session));
            // Create a new bus message File reference request
            requests.add(FileReferenceRequestDTO.build("file.name-" + i, UUID.randomUUID().toString(), "MD5",
                                                       "application/octet-stream", 10L, newOwner, refStorage,
                                                       "file://storage/location/file.name", sessionOwner, session));
            nbRrequests++;
            client.reference(requests);
            LOGGER.info(" {} requests sent ....", nbRrequests);
        }

        waitRequestEnds(nbRrequests, 1200);
        LOGGER.info("{} requests handled in {} ms", nbRrequests, System.currentTimeMillis() - start);
    }

}
