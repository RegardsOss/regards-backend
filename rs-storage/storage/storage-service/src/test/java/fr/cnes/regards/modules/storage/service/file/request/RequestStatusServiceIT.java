/*
 * Copyright 2017-2023 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
import fr.cnes.regards.modules.filecatalog.dto.FileRequestStatus;
import fr.cnes.regards.modules.storage.domain.database.request.FileStorageRequestAggregation;
import fr.cnes.regards.modules.storage.service.AbstractStorageIT;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
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

    @Before
    public void init() throws ModuleException {
        super.init();
    }

    @Test
    public void test_delayed_requests() {
        // Given
        List<FileStorageRequestAggregation> requests = new ArrayList<>();
        String id = UUID.randomUUID().toString();
        IntStream.range(0, 10).forEach(i -> requests.add(generateRandomStorageRequest(id, FileRequestStatus.DELAYED)));
        fileStorageRequestRepo.saveAll(requests);

        // With try to un delayed possible requests
        requestStatusService.checkDelayedStorageRequests();

        // Then only one request should be un delayed
        Assert.assertEquals(9L,
                            fileStorageRequestRepo.countByStorageAndStatus(ONLINE_CONF_LABEL, FileRequestStatus.DELAYED)
                                                  .longValue());

        // With new try to un delayed possible requests
        requestStatusService.checkDelayedStorageRequests();

        // Then there is always only one request un delayed
        Assert.assertEquals(9L,
                            fileStorageRequestRepo.countByStorageAndStatus(ONLINE_CONF_LABEL, FileRequestStatus.DELAYED)
                                                  .longValue());
    }
}
