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
package fr.cnes.regards.modules.workermanager.service.requests.scan;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.integration.test.job.AbstractMultitenantServiceWithJobIT;
import fr.cnes.regards.framework.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.modules.workermanager.dao.IRequestRepository;
import fr.cnes.regards.modules.workermanager.domain.request.Request;
import fr.cnes.regards.modules.workermanager.dto.WorkerConfigDto;
import fr.cnes.regards.modules.workermanager.dto.requests.RequestStatus;
import fr.cnes.regards.modules.workermanager.service.config.WorkerConfigService;
import fr.cnes.regards.modules.workermanager.service.config.WorkerManagerConfigManager;
import fr.cnes.regards.modules.workermanager.service.flow.RequestHandlerConfiguration;
import fr.cnes.regards.modules.workermanager.service.requests.RequestService;
import org.assertj.core.util.Lists;
import org.junit.After;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * @author Léo Mieulet
 */
@TestPropertySource(properties = { "regards.amqp.enabled=false" },
    locations = { "classpath:application-test.properties" })
@ActiveProfiles({ "noscheduler" })
@ContextConfiguration(classes = { RequestHandlerConfiguration.class })
public abstract class AbstractRequestJobIT extends AbstractMultitenantServiceWithJobIT {

    @Autowired
    protected IRequestRepository requestRepository;

    @Autowired
    private IJobInfoRepository jobInfoRepository;

    @Autowired
    private WorkerConfigService workerConfigService;

    @Autowired
    private WorkerManagerConfigManager configManager;

    @Autowired
    protected RequestService requestService;

    @Before
    public void doInit() {

        runtimeTenantResolver.forceTenant(getDefaultTenant());

        simulateApplicationReadyEvent();
        simulateApplicationStartedEvent();
        cleanRepository();
        configManager.resetConfiguration();

        workerConfigService.importConfiguration(Sets.newHashSet(new WorkerConfigDto(RequestHandlerConfiguration.AVAILABLE_WORKER_TYPE,
                                                                                    Sets.newHashSet(
                                                                                        RequestHandlerConfiguration.AVAILABLE_CONTENT_TYPE))));
    }

    @After
    public void doAfter() {
        // cleanRepository();
    }

    public void cleanRepository() {
        // Clean everything
        requestRepository.deleteAll();
        jobInfoRepository.deleteAll();
    }

    public void createRequests(int nbRequests) {
        List<Request> requests = Lists.newArrayList();
        for (int i = 0; i < nbRequests; i++) {
            Request request = new Request();
            request.setRequestId("requestId" + i);
            request.setCreationDate(OffsetDateTime.now());
            request.setContentType(RequestHandlerConfiguration.AVAILABLE_CONTENT_TYPE);
            request.setSource("source1");
            request.setSession("session1");
            request.setStatus(RequestStatus.NO_WORKER_AVAILABLE);
            request.setContent("blbl".getBytes());
            request.setError("error");
            requests.add(request);
        }
        requestRepository.saveAll(requests);
    }
}
