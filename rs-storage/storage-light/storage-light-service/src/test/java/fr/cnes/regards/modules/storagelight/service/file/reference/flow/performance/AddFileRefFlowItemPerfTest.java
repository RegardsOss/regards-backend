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
package fr.cnes.regards.modules.storagelight.service.file.reference.flow.performance;

import java.util.Set;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.storagelight.domain.database.FileLocation;
import fr.cnes.regards.modules.storagelight.domain.database.FileReference;
import fr.cnes.regards.modules.storagelight.domain.database.FileReferenceMetaInfo;
import fr.cnes.regards.modules.storagelight.domain.flow.AddFileRefFlowItem;
import fr.cnes.regards.modules.storagelight.domain.plugin.StorageType;
import fr.cnes.regards.modules.storagelight.service.file.reference.AbstractFileReferenceTest;
import fr.cnes.regards.modules.storagelight.service.file.reference.FileReferenceService;
import fr.cnes.regards.modules.storagelight.service.file.reference.FileStorageRequestService;
import fr.cnes.regards.modules.storagelight.service.file.reference.flow.AddFileReferenceFlowItemHandler;

/**
 *
 * @author Sébastien Binda
 */
@ActiveProfiles({ "noscheduler" })
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=storage_tests",
        "regards.storage.cache.path=target/cache" })
@Ignore("Performance test for asynchronous bulk requests")
public class AddFileRefFlowItemPerfTest extends AbstractFileReferenceTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(AddFileRefFlowItemPerfTest.class);

    @Autowired
    private AddFileReferenceFlowItemHandler handler;

    @Autowired
    FileReferenceService fileRefService;

    @Autowired
    FileStorageRequestService fileStorageRequestService;

    @SpyBean
    public IPublisher publisher;

    @Before
    public void initialize() throws ModuleException {
        Mockito.clearInvocations(publisher);

        jobInfoRepo.deleteAll();
        prioritizedDataStorageService.search(StorageType.ONLINE).forEach(c -> {
            try {
                prioritizedDataStorageService.delete(c.getId());
            } catch (ModuleException e) {
                Assert.fail(e.getMessage());
            }
        });
        prioritizedDataStorageService.search(StorageType.NEARLINE).forEach(c -> {
            try {
                prioritizedDataStorageService.delete(c.getId());
            } catch (ModuleException e) {
                Assert.fail(e.getMessage());
            }
        });
        initDataStoragePluginConfiguration(ONLINE_CONF_LABEL);
        initDataStorageNLPluginConfiguration(NEARLINE_CONF_LABEL);
        storageHandler.refresh();

        if (fileRefRepo.count() == 0) {
            // Insert many refs
            Set<FileReference> toSave = Sets.newHashSet();
            for (Long i = 0L; i < 100_000; i++) {
                FileReferenceMetaInfo metaInfo = new FileReferenceMetaInfo(UUID.randomUUID().toString(), "UUID",
                        "file_" + i + ".test", i, MediaType.APPLICATION_OCTET_STREAM);
                FileLocation location = new FileLocation("storage_" + i, "storage://plop/file");
                FileReference fileRef = new FileReference(Lists.newArrayList("owner"), metaInfo, location);
                toSave.add(fileRef);
                if (toSave.size() > 10_000) {
                    long start = System.currentTimeMillis();
                    fileRefRepo.saveAll(toSave);
                    LOGGER.info("Saves {} done ine {}ms", toSave.size(), System.currentTimeMillis() - start);
                    toSave.clear();
                }
            }
            long start = System.currentTimeMillis();
            fileRefRepo.saveAll(toSave);
            LOGGER.info("Saves {} done ine {}ms", toSave.size(), System.currentTimeMillis() - start);
        }
    }

    @Test
    public void addFileRefFlowItems() throws InterruptedException {
        LOGGER.info(" ----------------------------------- ");
        LOGGER.info(" ----------------------------------- ");
        LOGGER.info(" ----------------------------------- ");
        LOGGER.info(" --------     Starting     --------- ");
        LOGGER.info(" ----------------------------------- ");
        LOGGER.info(" ----------------------------------- ");
        LOGGER.info(" ----------------------------------- ");
        String storage = "storage";
        for (int i = 0; i < 5000; i++) {
            String checksum = UUID.randomUUID().toString();
            // Create a new bus message File reference request
            AddFileRefFlowItem item = new AddFileRefFlowItem("file.name", checksum, "MD5", "application/octet-stream",
                    10L, "owner-test", storage, "file://storage/location/file.name", storage,
                    "file://storage/location/file.name");
            TenantWrapper<AddFileRefFlowItem> wrapper = new TenantWrapper<>(item, getDefaultTenant());
            // Publish request
            handler.handle(wrapper);
        }
        Thread.sleep(30000);
    }

}
