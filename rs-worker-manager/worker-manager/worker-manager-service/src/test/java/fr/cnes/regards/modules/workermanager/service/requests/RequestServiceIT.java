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
package fr.cnes.regards.modules.workermanager.service.requests;

import fr.cnes.regards.modules.workermanager.dao.IRequestRepository;
import fr.cnes.regards.modules.workermanager.domain.database.LightRequest;
import fr.cnes.regards.modules.workermanager.domain.request.Request;
import fr.cnes.regards.modules.workermanager.domain.request.SearchRequestParameters;
import fr.cnes.regards.modules.workermanager.dto.requests.RequestStatus;
import fr.cnes.regards.modules.workermanager.service.cache.AbstractWorkerManagerServiceUtilsIT;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * @author Théo Lasserre
 */
@ContextConfiguration(classes = { RequestServiceIT.Config.class })
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=request_service_it" })
public class RequestServiceIT extends AbstractWorkerManagerServiceUtilsIT {

    @Configuration
    public static class Config {

    }

    protected final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private RequestService requestService;

    @Autowired
    private IRequestRepository requestRepository;

    /**
     * Custom test initialization to override
     *
     * @throws Exception
     */
    protected void doInit() throws Exception {
        // Override to init something
        LOGGER.info("=========================> BEGIN INIT DATA FOR TESTS <=====================");
        createRequests("requestId1",
                       OffsetDateTime.now(),
                       "contentType1",
                       "source1",
                       "session1",
                       RequestStatus.DISPATCHED,
                       "blbl".getBytes(),
                       "error1",
                       1);
        createRequests("requestId2",
                       OffsetDateTime.now(),
                       "contentType2",
                       "source2",
                       "session2",
                       RequestStatus.INVALID_CONTENT,
                       "blbl".getBytes(),
                       "error2",
                       2);
        createRequests("requestId3",
                       OffsetDateTime.now(),
                       "contentType3",
                       "source3",
                       "session3",
                       RequestStatus.NO_WORKER_AVAILABLE,
                       "blbl".getBytes(),
                       "error3",
                       3);
        createRequests("requestId4",
                       OffsetDateTime.now(),
                       "contentType4",
                       "source4",
                       "session4",
                       RequestStatus.ERROR,
                       "blbl".getBytes(),
                       "error4",
                       4);
        createRequests("requestId5",
                       OffsetDateTime.now(),
                       "contentType5",
                       "source5",
                       "session5",
                       RequestStatus.RUNNING,
                       "blbl".getBytes(),
                       "error5",
                       5);
        createRequests("requestId6",
                       OffsetDateTime.now(),
                       "contentType6",
                       "source6",
                       "session6",
                       RequestStatus.SUCCESS,
                       "blbl".getBytes(),
                       "error6",
                       6);
        LOGGER.info("=========================> END INIT DATA FOR TESTS <=====================");
    }

    @Test
    public void testSearchRequests() {
        List<Request> requestsInRepo = requestRepository.findAll();
        PageRequest pr = PageRequest.of(0, 100);
        SearchRequestParameters srp = new SearchRequestParameters();

        LOGGER.info("=========================> BEGIN SEARCH REQUESTS <=====================");
        srp.withStatusesIncluded(RequestStatus.DISPATCHED);
        Page<LightRequest> requests = requestService.searchLightRequests(srp, pr);
        Assert.assertEquals("Error searching DISPATCHED requests", 1, requests.getTotalElements());

        srp.withStatusesIncluded(RequestStatus.INVALID_CONTENT);
        requests = requestService.searchLightRequests(srp, pr);
        Assert.assertEquals("Error searching INVALID_CONTENT requests", 2, requests.getTotalElements());

        srp.withStatusesIncluded(RequestStatus.NO_WORKER_AVAILABLE);
        requests = requestService.searchLightRequests(srp, pr);
        Assert.assertEquals("Error searching NO_WORKER_AVAILABLE requests", 3, requests.getTotalElements());

        srp.withStatusesIncluded(RequestStatus.ERROR);
        requests = requestService.searchLightRequests(srp, pr);
        Assert.assertEquals("Error searching ERROR requests", 4, requests.getTotalElements());

        srp.withStatusesIncluded(RequestStatus.RUNNING);
        requests = requestService.searchLightRequests(srp, pr);
        Assert.assertEquals("Error searching RUNNING requests", 5, requests.getTotalElements());

        srp.withStatusesIncluded(RequestStatus.SUCCESS);
        requests = requestService.searchLightRequests(srp, pr);
        Assert.assertEquals("Error searching SUCCESS requests", 6, requests.getTotalElements());

        srp.setStatuses(null); // clear status
        OffsetDateTime createdBefore = OffsetDateTime.now().plusDays(2);
        srp.withCreationDateBefore(createdBefore);
        requests = requestService.searchLightRequests(srp, pr);
        Assert.assertEquals("Error searching request before date", 21, requests.getTotalElements());

        OffsetDateTime createdAfter = OffsetDateTime.now().plusDays(1);
        srp.withCreateDateBeforeAndAfter(createdBefore, createdAfter);
        requests = requestService.searchLightRequests(srp, pr);
        Assert.assertEquals("Error searching request between dates", 0, requests.getTotalElements());

        createdAfter = OffsetDateTime.now().minusDays(1);
        srp.withCreateDateBeforeAndAfter(createdBefore, createdAfter);
        requests = requestService.searchLightRequests(srp, pr);
        Assert.assertEquals("Error searching request between dates", 21, requests.getTotalElements());

        createdAfter = OffsetDateTime.now().minusDays(1);
        srp.withCreationDateAfter(createdAfter);
        requests = requestService.searchLightRequests(srp, pr);
        Assert.assertEquals("Error searching request after date", 21, requests.getTotalElements());

        srp.withIdsIncluded(requestsInRepo.get(0).getId(), requestsInRepo.get(1).getId());
        requests = requestService.searchLightRequests(srp, pr);
        Assert.assertEquals("Error searching a collection of requestId", 2, requests.getTotalElements());

        srp.withIdsExcluded(requestsInRepo.get(0).getId());
        requests = requestService.searchLightRequests(srp, pr);
        Assert.assertEquals("Error searching a collection of requestId", 20, requests.getTotalElements());
        LOGGER.info("=========================> END SEARCH REQUESTS <=====================");
    }

    @Test
    public void testSourceSearchFilter() {
        Page<LightRequest> requests = requestService.searchLightRequests(new SearchRequestParameters().withSource(
            "source2"), PageRequest.of(0, 100));
        Assert.assertEquals("Error searching a collection of requestId", 2, requests.getTotalElements());
    }

}
