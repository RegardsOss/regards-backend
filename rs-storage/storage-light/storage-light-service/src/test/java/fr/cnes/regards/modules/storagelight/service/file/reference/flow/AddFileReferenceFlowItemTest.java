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

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.modules.storagelight.domain.flow.AddFileRefFlowItem;
import fr.cnes.regards.modules.storagelight.service.file.reference.AbstractFileReferenceTest;
import fr.cnes.regards.modules.storagelight.service.file.reference.FileReferenceService;

/**
 * @author sbinda
 *
 */
@ActiveProfiles({ "noscheduler" })
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=storage_tests",
        "regards.storage.cache.path=target/cache", "regards.storage.cache.minimum.time.to.live.hours=12" })
public class AddFileReferenceFlowItemTest extends AbstractFileReferenceTest {

    @Autowired
    private AddFileReferenceFlowItemHandler handler;

    @Autowired
    FileReferenceService fileRefService;

    @Test
    public void addFileRefFlowItem() {
        Instant start = Instant.now();
        String checksum = UUID.randomUUID().toString();
        String storage = "storage";
        AddFileRefFlowItem item = new AddFileRefFlowItem("file.name", checksum, "MD5", "application/octet-stream", 10L,
                "owner-test", storage, "file://storage/location/file.name", storage,
                "file://storage/location/file.name");
        TenantWrapper<AddFileRefFlowItem> wrapper = new TenantWrapper<>(item, getDefaultTenant());
        handler.handle(wrapper);
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        Assert.assertTrue("File should be referenced", fileRefService.search(storage, checksum).isPresent());
        Instant end = Instant.now();
        System.out.println("Time taken: " + Duration.between(start, end).toMillis() + " milliseconds");
    }

}
