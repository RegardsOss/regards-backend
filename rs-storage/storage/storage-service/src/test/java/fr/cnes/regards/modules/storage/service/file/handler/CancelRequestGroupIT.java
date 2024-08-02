package fr.cnes.regards.modules.storage.service.file.handler;/*
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

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.fileaccess.dto.FileRequestStatus;
import fr.cnes.regards.modules.storage.domain.database.request.FileStorageRequestAggregation;
import fr.cnes.regards.modules.storage.service.AbstractStorageIT;
import fr.cnes.regards.modules.storage.service.file.request.RequestsGroupService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

/**
 * @author Sébastien Binda
 **/
@ActiveProfiles({ "noscheduler" })
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=cancel_tests" },
                    locations = { "classpath:application-test.properties" })
public class CancelRequestGroupIT extends AbstractStorageIT {

    @Autowired
    private RequestsGroupService requestsGroupService;

    @Before
    @Override
    public void init() throws ModuleException {
        super.init();
    }

    @Test
    public void test_group_cancel() throws ExecutionException, InterruptedException {
        this.generateStoredFileReference(UUID.randomUUID().toString(),
                                         "regards",
                                         "file.test",
                                         ONLINE_CONF_LABEL,
                                         Optional.empty(),
                                         Optional.empty(),
                                         "SESSION_OWNER_1",
                                         "SESSION_1");
        FileStorageRequestAggregation fr = this.generateStoreFileError("regards",
                                                                       ONLINE_CONF_LABEL,
                                                                       "SESSION_OWNER_1",
                                                                       "SESSION_1");
        Assert.assertEquals("There should be one request in error status", 1L, fileStorageRequestRepo.count());
        // Nb store requests : 2 INC --> 2 storage requests sent
        // Nb running requests : 2 INC - 2 DEC
        // Nb stored Files : 1 INC --> Only one request in success stored 1 file
        // Nb error : 1 INC
        checkSessionEvents(8, 2, 0, 2, 2, 1, 1, 0);
        requestsGroupService.cancelRequestGroup(fr.getGroupIds().stream().findFirst().get());
        Assert.assertEquals("There should be no remaining requests", 0L, fileStorageRequestRepo.count());
        // Nb store requests :  1 DEC --> After error cancel
        // Nb error : 1 DEC --> Error decremented after cancel
        checkSessionEvents(2, 0, 1, 0, 0, 0, 0, 1);

    }

    @Test
    public void test_group_cancel_on_pending_request() throws ExecutionException, InterruptedException {
        this.generateStoredFileReference(UUID.randomUUID().toString(),
                                         "regards",
                                         "file.test",
                                         ONLINE_CONF_LABEL,
                                         Optional.empty(),
                                         Optional.empty(),
                                         "SESSION_OWNER_1",
                                         "SESSION_1");
        FileStorageRequestAggregation fr = this.generateStoreFileError("regards",
                                                                       ONLINE_CONF_LABEL,
                                                                       "SESSION_OWNER_1",
                                                                       "SESSION_1");
        fr.setStatus(FileRequestStatus.PENDING);
        fileStorageRequestRepo.save(fr);
        // Nb store requests : 2 INC --> 2 storage requests sent
        // Nb running requests : 2 INC - 2 DEC
        // Nb stored Files : 1 INC --> Only one request in success stored 1 file
        // Nb error : 1 INC
        checkSessionEvents(8, 2, 0, 2, 2, 1, 1, 0);
        Assert.assertEquals("There should be one request in pending status", 1L, fileStorageRequestRepo.count());
        requestsGroupService.cancelRequestGroup(fr.getGroupIds().stream().findFirst().get());
        Assert.assertEquals("There should be no remaining requests", 1L, fileStorageRequestRepo.count());
    }

}
