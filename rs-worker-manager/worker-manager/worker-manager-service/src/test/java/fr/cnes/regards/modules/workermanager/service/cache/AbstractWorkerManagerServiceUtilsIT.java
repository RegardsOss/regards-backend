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
package fr.cnes.regards.modules.workermanager.service.cache;

import fr.cnes.regards.framework.jpa.multitenant.test.AbstractMultitenantServiceIT;
import fr.cnes.regards.framework.modules.session.agent.dao.IStepPropertyUpdateRequestRepository;
import fr.cnes.regards.framework.modules.session.commons.dao.ISessionStepRepository;
import fr.cnes.regards.framework.modules.session.commons.dao.ISnapshotProcessRepository;
import fr.cnes.regards.framework.modules.tenant.settings.dao.IDynamicTenantSettingRepository;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.workermanager.amqp.events.EventHeadersHelper;
import fr.cnes.regards.modules.workermanager.dao.IRequestRepository;
import fr.cnes.regards.modules.workermanager.dao.IWorkerConfigRepository;
import fr.cnes.regards.modules.workermanager.dao.IWorkflowRepository;
import fr.cnes.regards.modules.workermanager.domain.request.Request;
import fr.cnes.regards.modules.workermanager.dto.requests.RequestStatus;
import fr.cnes.regards.modules.workermanager.service.config.WorkerConfigCacheService;
import fr.cnes.regards.modules.workermanager.service.config.WorkerManagerConfigManager;
import fr.cnes.regards.modules.workermanager.service.config.WorkflowConfigCacheService;
import fr.cnes.regards.modules.workermanager.service.requests.RequestService;
import org.assertj.core.util.Lists;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Léo Mieulet
 **/
@TestPropertySource(properties = { "regards.amqp.enabled=false" },
                    locations = { "classpath:application-test.properties" })
public abstract class AbstractWorkerManagerServiceUtilsIT extends AbstractMultitenantServiceIT {

    protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractWorkerManagerServiceUtilsIT.class);

    /**
     * Services
     */

    @Autowired
    protected IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    protected IDynamicTenantSettingRepository dynamicTenantSettingRepository;

    @Autowired
    protected IWorkerConfigRepository workerConfigRepository;

    @Autowired
    protected IWorkflowRepository workflowRepository;

    @Autowired
    protected WorkerManagerConfigManager configManager;

    @Autowired
    protected IRequestRepository requestRepository;

    @Autowired
    protected ISessionStepRepository sessionStepRepository;

    @Autowired
    protected ISnapshotProcessRepository sessionSnapshotRepository;

    @Autowired
    protected IStepPropertyUpdateRequestRepository stepPropertyUpdateRepository;

    @Autowired
    protected RequestService requestService;

    @Autowired
    protected WorkerCacheService workerCacheService;

    @Autowired
    protected WorkerConfigCacheService workerConfigCacheService;

    @Autowired
    private WorkflowConfigCacheService workflowConfigCacheService;

    // -------------
    // BEFORE METHODS
    // -------------

    @Before
    public void init() throws Exception {

        runtimeTenantResolver.forceTenant(getDefaultTenant());
        // Remove any old value
        cleanRepositories();

        // Clean cache
        configManager.resetConfiguration();
        workerConfigCacheService.cleanCache();
        workflowConfigCacheService.cleanCache();

        // simulate application started and ready
        simulateApplicationStartedEvent();
        simulateApplicationReadyEvent();

        runtimeTenantResolver.forceTenant(getDefaultTenant());

        // override this method to custom action performed before
        doInit();
    }

    /**
     * Custom test initialization to override
     */
    protected void doInit() throws Exception {
        // Override to init something
    }

    // -------------
    // AFTER METHODS
    // -------------

    @After
    public void after() throws Exception {
        doAfter();
    }

    /**
     * Custom test cleaning to override
     */
    protected void doAfter() throws Exception {
        // Override to init something
    }

    // -------------
    //     REPO
    // -------------
    private void cleanRepositories() {
        LOGGER.info("Clean repository");
        dynamicTenantSettingRepository.deleteAll();
        workerConfigRepository.deleteAll();
        requestRepository.deleteAll();
        stepPropertyUpdateRepository.deleteAll();
        sessionSnapshotRepository.deleteAll();
        sessionStepRepository.deleteAll();
        workflowRepository.deleteAllInBatch();
    }

    // --------------
    //  WORKER MANAGER UTILS
    // --------------

    protected List<Message> createRequestEvents(String requestId,
                                                String contentType,
                                                String source,
                                                String session,
                                                byte[] content,
                                                int nbRequests) {
        List<Message> messages = new ArrayList<>(nbRequests);
        for (int i = 0; i < nbRequests; i++) {
            MessageProperties properties = new MessageProperties();
            properties.setHeader(EventHeadersHelper.REQUEST_ID_HEADER, requestId + i);
            properties.setHeader(EventHeadersHelper.CONTENT_TYPE_HEADER, contentType);
            properties.setHeader(EventHeadersHelper.OWNER_HEADER, source);
            properties.setHeader(EventHeadersHelper.SESSION_HEADER, session);
            messages.add(new Message(content, properties));
        }
        return messages;
    }

    protected List<Request> createRequests(String requestId,
                                           OffsetDateTime creationDate,
                                           String contentType,
                                           String source,
                                           String session,
                                           RequestStatus status,
                                           byte[] content,
                                           String error,
                                           int nbRequests) {
        List<Request> requests = Lists.newArrayList();
        for (int i = 0; i < nbRequests; i++) {
            Request request = new Request();
            request.setRequestId(requestId + i);
            request.setCreationDate(creationDate);
            request.setContentType(contentType);
            request.setSource(source);
            request.setSession(session);
            request.setStatus(status);
            request.setContent(content);
            request.setOriginalContent(content);
            request.setError(error);
            requests.add(request);
        }
        requests = requestRepository.saveAll(requests);
        Assert.assertEquals(nbRequests, requests.size());
        return requests;
    }

}