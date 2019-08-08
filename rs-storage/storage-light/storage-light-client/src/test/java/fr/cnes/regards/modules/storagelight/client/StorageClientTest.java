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
package fr.cnes.regards.modules.storagelight.client;

import java.util.UUID;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import fr.cnes.regards.framework.jpa.multitenant.test.AbstractMultitenantServiceTest;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.storagelight.domain.dto.FileReferenceRequestDTO;

/**
 * @author sbinda
 *
 */
@ActiveProfiles("testAmqp")
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=storage_client_tests",
        "regards.storage.cache.path=target/cache", "regards.amqp.enabled=true" })
public class StorageClientTest extends AbstractMultitenantServiceTest {

    @Autowired
    private StorageListener listener;

    @Autowired
    private StorageClient client;

    @Autowired
    IRuntimeTenantResolver runtimeTenantResolver;

    @Before
    public void init() {
        simulateApplicationReadyEvent();
    }

    @Test
    public void storeFile() throws InterruptedException {
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        RequestInfo info = client.reference(FileReferenceRequestDTO
                .build("file.test", UUID.randomUUID().toString(), "UUID", "application/octet-stream", 10L, "owner",
                       "somewhere", "somewhere://in/here/file.test"));
        Thread.sleep(10_000);
        Assert.assertTrue("Request should be successful", listener.getSuccess().contains(info));
        Assert.assertFalse("Request should not be error", listener.getErrors().containsKey(info));
    }

}
