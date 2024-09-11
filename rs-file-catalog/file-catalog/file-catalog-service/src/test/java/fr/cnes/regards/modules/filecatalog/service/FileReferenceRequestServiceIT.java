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
package fr.cnes.regards.modules.filecatalog.service;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.filecatalog.domain.FileReference;
import fr.cnes.regards.modules.filecatalog.domain.request.FileStorageRequestAggregation;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

/**
 * Test class
 *
 * @author Sébastien Binda
 */
@ActiveProfiles({ "noscheduler" })
@TestPropertySource(properties = { "spring.jpa.properties.hibernate"
                                   + ".default_schema=file_catalog_reference_requests_tests" },
                    locations = { "classpath:application-test.properties" })
public class FileReferenceRequestServiceIT extends AbstractFileCatalogIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileReferenceRequestServiceIT.class);

    private static final String SESSION_OWNER_1 = "SOURCE 1";

    private static final String SESSION_OWNER_2 = "SOURCE 2";

    private static final String SESSION_1 = "SESSION 1";

    @Before
    @Override
    public void init() throws ModuleException {
        super.init();
    }

    @Ignore("Not Implemented yet")
    @Test
    public void referenceFileDuringDeletion() throws InterruptedException, ExecutionException {
        //FIXME this will need to be tested once deletion is implemented
    }

    @Requirement("REGARDS_DSL_STOP_AIP_070")
    @Purpose("System can reference file without moving files and save the files checksum.")
    @Test
    public void referenceFileWithoutStorage() {
        String owner = "someone";
        Optional<FileReference> oFileRef = referenceRandomFile(owner,
                                                               null,
                                                               "file.test",
                                                               ONLINE_CONF_LABEL,
                                                               SESSION_OWNER_1,
                                                               SESSION_1,
                                                               null);
        Assert.assertTrue("File reference should have been created", oFileRef.isPresent());
        Collection<FileStorageRequestAggregation> storageReqs = fileStorageRequestService.search(oFileRef.get()
                                                                                                         .getLocation()
                                                                                                         .getStorage(),
                                                                                                 oFileRef.get()
                                                                                                         .getMetaInfo()
                                                                                                         .getChecksum());
        Assert.assertTrue("File reference request should not exists anymore as file is well referenced",
                          storageReqs.isEmpty());
    }

}
