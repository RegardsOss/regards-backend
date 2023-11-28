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
package fr.cnes.regards.modules.storage.rest;

import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.framework.test.integration.RequestBuilderCustomizer;
import fr.cnes.regards.modules.filecatalog.dto.FileRequestType;
import fr.cnes.regards.modules.storage.dao.IFileStorageRequestRepository;
import fr.cnes.regards.modules.storage.domain.database.FileReferenceMetaInfo;
import fr.cnes.regards.modules.storage.domain.database.request.FileStorageRequestAggregation;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.annotation.DirtiesContext.HierarchyMode;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Optional;
import java.util.UUID;

/**
 * Test class for {@link FileRequestsController}
 *
 * @author Sébastien Binda
 */
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext(classMode = ClassMode.AFTER_CLASS, hierarchyMode = HierarchyMode.EXHAUSTIVE)
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=storage_rest_it" })
@ActiveProfiles(value = { "default", "test" }, inheritProfiles = false)
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
        FileReferenceMetaInfo meta = new FileReferenceMetaInfo(UUID.randomUUID().toString(),
                                                               "MD5",
                                                               "file.txt",
                                                               10L,
                                                               MediaType.APPLICATION_JSON_UTF8);
        FileStorageRequestAggregation req = new FileStorageRequestAggregation("regards",
                                                                              meta,
                                                                              "file://somewhere/file.txt",
                                                                              "somewhere",
                                                                              Optional.empty(),
                                                                              UUID.randomUUID().toString(),
                                                                              "source1",
                                                                              "session1");
        RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatusOk().expectIsEmpty("$.content");
        performDefaultGet(FileRequestsController.REQUESTS_PATH
                          + FileRequestsController.STORAGE_PATH
                          + FileRequestsController.TYPE_PATH,
                          requestBuilderCustomizer,
                          "Expect ok status.",
                          storage,
                          FileRequestType.STORAGE.toString());
        tenantResolver.forceTenant(getDefaultTenant());
        storageReqRepo.save(req);
        requestBuilderCustomizer = customizer().expectStatusOk()
                                               .expectValue("$.content[0].content.type",
                                                            FileRequestType.STORAGE.toString());
        performDefaultGet(FileRequestsController.REQUESTS_PATH
                          + FileRequestsController.STORAGE_PATH
                          + FileRequestsController.TYPE_PATH,
                          requestBuilderCustomizer,
                          "Expect ok status.",
                          storage,
                          FileRequestType.STORAGE.toString());
    }

}
