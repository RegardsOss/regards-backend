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
package fr.cnes.regards.modules.storagelight.rest;

import java.util.Optional;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;

import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.framework.test.integration.RequestBuilderCustomizer;
import fr.cnes.regards.modules.storagelight.dao.IFileStorageRequestRepository;
import fr.cnes.regards.modules.storagelight.domain.database.FileReferenceMetaInfo;
import fr.cnes.regards.modules.storagelight.domain.database.request.FileStorageRequest;
import fr.cnes.regards.modules.storagelight.domain.event.FileRequestType;

/**
 * Test class for {@link FileRequestsController}
 *
 * @author Sébastien Binda
 *
 */
@Profile("noschedule")
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=storage_rest_it",
        "regards.storage.cache.path=target/cache" })
public class FileRequestsControllerIT extends AbstractRegardsTransactionalIT {

    @Autowired
    private IFileStorageRequestRepository storageReqRepo;

    @Autowired
    private IRuntimeTenantResolver tenantResolver;

    @Before
    public void init() {
        tenantResolver.forceTenant(getDefaultTenant());
        storageReqRepo.deleteAll();
    }

    @Test
    public void search() {
        String storage = "somewhere";
        FileReferenceMetaInfo meta = new FileReferenceMetaInfo(UUID.randomUUID().toString(), "MD5", "file.txt", 10L,
                MediaType.APPLICATION_JSON_UTF8);
        FileStorageRequest req = new FileStorageRequest("regards", meta, "file://somewhere/file.txt", "somewhere",
                Optional.empty(), UUID.randomUUID().toString());
        RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatusOk().expectIsEmpty("$.content");
        performDefaultGet(FileRequestsController.REQUESTS_PATH + FileRequestsController.STORAGE_PATH
                + FileRequestsController.TYPE_PATH, requestBuilderCustomizer, "Expect ok status.", storage,
                          FileRequestType.STORAGE.toString());
        tenantResolver.forceTenant(getDefaultTenant());
        storageReqRepo.save(req);
        requestBuilderCustomizer = customizer().expectStatusOk().expectValue("$.content[0].content.type",
                                                                             FileRequestType.STORAGE.toString());
        performDefaultGet(FileRequestsController.REQUESTS_PATH + FileRequestsController.STORAGE_PATH
                + FileRequestsController.TYPE_PATH, requestBuilderCustomizer, "Expect ok status.", storage,
                          FileRequestType.STORAGE.toString());
    }

}
