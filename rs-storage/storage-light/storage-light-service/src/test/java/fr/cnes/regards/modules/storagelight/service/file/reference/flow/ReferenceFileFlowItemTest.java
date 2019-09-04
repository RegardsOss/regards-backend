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
package fr.cnes.regards.modules.storagelight.service.file.reference.flow;

import java.util.UUID;
import java.util.concurrent.ExecutionException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.storagelight.domain.database.FileReference;
import fr.cnes.regards.modules.storagelight.domain.dto.FileReferenceRequestDTO;
import fr.cnes.regards.modules.storagelight.domain.event.FileReferenceEvent;
import fr.cnes.regards.modules.storagelight.domain.event.FileReferenceEventType;
import fr.cnes.regards.modules.storagelight.domain.flow.ReferenceFlowItem;
import fr.cnes.regards.modules.storagelight.service.file.FileRequestService;
import fr.cnes.regards.modules.storagelight.service.file.flow.ReferenceFlowItemHandler;
import fr.cnes.regards.modules.storagelight.service.file.reference.AbstractFileReferenceTest;
import fr.cnes.regards.modules.storagelight.service.file.request.FileStorageRequestService;

/**
 *
 * @author Sébastien Binda
 */
@ActiveProfiles({ "noscheduler" })
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=storage_tests",
        "regards.storage.cache.path=target/cache" })
public class ReferenceFileFlowItemTest extends AbstractFileReferenceTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReferenceFileFlowItemTest.class);

    @Autowired
    private ReferenceFlowItemHandler handler;

    @Autowired
    FileRequestService fileRefService;

    @Autowired
    FileStorageRequestService fileStorageRequestService;

    @Before
    public void initialize() throws ModuleException {
        Mockito.clearInvocations(publisher);
        super.init();
    }

    /**
     * Test request to reference a file already stored.
     * The file is not stored by the service as the origin storage and the destination storage are identical
     * @throws InterruptedException
     */
    @Test
    public void addFileRefFlowItem() throws InterruptedException {
        String checksum = UUID.randomUUID().toString();
        String storage = "storage";
        // Create a new bus message File reference request
        ReferenceFlowItem item = ReferenceFlowItem
                .build(FileReferenceRequestDTO.build("file.name", checksum, "MD5", "application/octet-stream", 10L,
                                                     "owner-test", storage, "file://storage/location/file.name"),
                       UUID.randomUUID().toString());
        TenantWrapper<ReferenceFlowItem> wrapper = new TenantWrapper<>(item, getDefaultTenant());
        long start = System.currentTimeMillis();
        // Publish request
        handler.handleSync(wrapper);
        long finish = System.currentTimeMillis();
        LOGGER.info("Add file reference duration {}ms", finish - start);
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        // Check file is well referenced
        Assert.assertTrue("File should be referenced", fileRefService.search(storage, checksum).isPresent());
        // Now check for event published
        ArgumentCaptor<ISubscribable> argumentCaptor = ArgumentCaptor.forClass(ISubscribable.class);
        Mockito.verify(this.publisher, Mockito.times(1)).publish(Mockito.any(FileReferenceEvent.class));
        Mockito.verify(this.publisher, Mockito.atLeastOnce()).publish(argumentCaptor.capture());
        Assert.assertEquals("File reference event STORED should be published", FileReferenceEventType.STORED,
                            getFileReferenceEvent(argumentCaptor.getAllValues()).getType());
    }

    /**
     * Test request to reference a file already stored.
     * The file is not stored by the service as the origin storage and the destination storage are identical
     * @throws ExecutionException
     * @throws InterruptedException
     */
    @Test
    public void addFileRefFlowItemAlreadyExists() throws InterruptedException, ExecutionException {
        String checksum = UUID.randomUUID().toString();
        String owner = "new-owner";
        FileReference fileRef = this.generateStoredFileReference(checksum, owner, "file.test", ONLINE_CONF_LABEL);
        String storage = fileRef.getLocation().getStorage();
        // Create a new bus message File reference request
        ReferenceFlowItem item = ReferenceFlowItem
                .build(FileReferenceRequestDTO.build("file.name", checksum, "MD5", "application/octet-stream", 10L,
                                                     "owner-test", storage, "file://storage/location/file.name"),
                       UUID.randomUUID().toString());
        TenantWrapper<ReferenceFlowItem> wrapper = new TenantWrapper<>(item, getDefaultTenant());
        // Publish request
        handler.handleSync(wrapper);
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        // Check file is well referenced
        Assert.assertTrue("File should be referenced", fileRefService.search(storage, checksum).isPresent());
        // Now check for event published. One for each referenced file
        ArgumentCaptor<ISubscribable> argumentCaptor = ArgumentCaptor.forClass(ISubscribable.class);
        Mockito.verify(this.publisher, Mockito.times(2)).publish(Mockito.any(FileReferenceEvent.class));
        Mockito.verify(this.publisher, Mockito.atLeastOnce()).publish(argumentCaptor.capture());
        Assert.assertEquals("File reference event STORED should be published", FileReferenceEventType.STORED,
                            getFileReferenceEvent(argumentCaptor.getAllValues()).getType());
    }

    /**
     * Test request to reference a file already stored.
     * The file is not stored by the service as the origin storage and the destination storage are identical
     * @throws ExecutionException
     * @throws InterruptedException
     */
    @Test
    public void addFileRefFlowItemWithSameChecksum() throws InterruptedException, ExecutionException {
        String checksum = UUID.randomUUID().toString();
        String owner = "new-owner";
        String storage = "aStorage";
        this.generateStoredFileReference(checksum, owner, "file.test", ONLINE_CONF_LABEL);
        // Create a new bus message File reference request
        ReferenceFlowItem item = ReferenceFlowItem
                .build(FileReferenceRequestDTO.build("file.name", checksum, "MD5", "application/octet-stream", 10L,
                                                     "owner-test", storage, "file://storage/location/file.name"),
                       UUID.randomUUID().toString());
        TenantWrapper<ReferenceFlowItem> wrapper = new TenantWrapper<>(item, getDefaultTenant());
        // Publish request
        handler.handleSync(wrapper);
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        // Check file is well referenced
        Assert.assertTrue("File should be referenced", fileRefService.search(storage, checksum).isPresent());
        // Now check for event published. One for each referenced file
        ArgumentCaptor<ISubscribable> argumentCaptor = ArgumentCaptor.forClass(ISubscribable.class);
        Mockito.verify(this.publisher, Mockito.times(2)).publish(Mockito.any(FileReferenceEvent.class));
        Mockito.verify(this.publisher, Mockito.atLeastOnce()).publish(argumentCaptor.capture());
        Assert.assertEquals("File reference event STORED should be published", FileReferenceEventType.STORED,
                            getFileReferenceEvent(argumentCaptor.getAllValues()).getType());
    }
}
