package fr.cnes.regards.modules.acquisition.service;/*
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

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.configuration.IAmqpAdmin;
import fr.cnes.regards.framework.amqp.configuration.IRabbitVirtualHostAdmin;
import fr.cnes.regards.framework.jpa.multitenant.test.AbstractMultitenantServiceIT;
import fr.cnes.regards.framework.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.framework.modules.plugins.dao.IPluginConfigurationRepository;
import fr.cnes.regards.framework.modules.session.agent.dao.IStepPropertyUpdateRequestRepository;
import fr.cnes.regards.framework.modules.session.agent.service.update.AgentSnapshotService;
import fr.cnes.regards.framework.modules.session.commons.dao.ISessionStepRepository;
import fr.cnes.regards.framework.modules.session.commons.dao.ISnapshotProcessRepository;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.acquisition.dao.*;
import org.junit.After;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

/**
 * Utils for tests
 *
 * @author Iliana Ghazali
 **/
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS,
                hierarchyMode = DirtiesContext.HierarchyMode.EXHAUSTIVE)
public abstract class DataproviderMultitenantServiceIT extends AbstractMultitenantServiceIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataproviderMultitenantServiceIT.class);

    /**
     * Services
     */

    @Autowired
    protected IRuntimeTenantResolver runtimeTenantResolver;

    // JOBS
    @Autowired
    protected IJobInfoService jobInfoService;

    // AMQP
    @Autowired
    protected ISubscriber subscriber;

    @Autowired
    protected IPublisher publisher;

    @Autowired(required = false)
    protected IAmqpAdmin amqpAdmin;

    @Autowired(required = false)
    protected IRabbitVirtualHostAdmin vhostAdmin;

    // CHAINS
    @Autowired
    protected IAcquisitionProcessingService processingService;

    @Autowired
    protected IProductService productService;

    // SESSION AGENT
    @Autowired
    protected AgentSnapshotService agentService;

    /**
     * Repositories
     */
    // JOBS
    @Autowired
    protected IJobInfoRepository jobInfoRepo;

    // SESSION AGENT
    @Autowired
    protected IStepPropertyUpdateRequestRepository stepRepo;

    @Autowired
    protected ISessionStepRepository sessionStepRepo;

    @Autowired
    protected ISnapshotProcessRepository snapshotRepo;

    // CHAINS
    @Autowired
    protected IAcquisitionFileInfoRepository fileInfoRepository;

    @Autowired
    protected IScanDirectoriesInfoRepository scanDirectoriesInfo;

    @Autowired
    protected IAcquisitionProcessingChainRepository acquisitionProcessingChainRepository;

    @Autowired
    protected IProductRepository productRepository;

    @Autowired
    protected IPluginConfigurationRepository pluginConfRepository;

    @Autowired
    protected IAcquisitionFileRepository acqFileRepository;

    // -------------
    // BEFORE METHODS
    // -------------

    @Before
    public void init() throws Exception {
        // simulate application started and ready
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        simulateApplicationStartedEvent();
        simulateApplicationReadyEvent();
        runtimeTenantResolver.forceTenant(getDefaultTenant());

        // override this method to custom action performed before
        doInit();
        cleanRepositories();
        Thread.sleep(5000L);
        LOGGER.info("|-----------------------------> TEST RESET DONE <-----------------------------------------|");
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
        // override this method to custom action performed after
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
        // session agent
        snapshotRepo.deleteAllInBatch();
        stepRepo.deleteAllInBatch();
        sessionStepRepo.deleteAllInBatch();

        // acquisitions
        acqFileRepository.deleteAllInBatch();
        productRepository.deleteAllInBatch();
        scanDirectoriesInfo.deleteAllInBatch();
        fileInfoRepository.deleteAllInBatch();
        acquisitionProcessingChainRepository.deleteAllInBatch();
        pluginConfRepository.deleteAllInBatch();

        // jobs
        jobInfoRepo.deleteAll();
    }
}