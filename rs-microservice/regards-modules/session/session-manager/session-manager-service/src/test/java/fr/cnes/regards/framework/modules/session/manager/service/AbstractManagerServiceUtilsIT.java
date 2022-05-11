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
package fr.cnes.regards.framework.modules.session.manager.service;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.configuration.AmqpChannel;
import fr.cnes.regards.framework.amqp.configuration.IAmqpAdmin;
import fr.cnes.regards.framework.amqp.configuration.IRabbitVirtualHostAdmin;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.event.Target;
import fr.cnes.regards.framework.integration.test.job.JobTestCleaner;
import fr.cnes.regards.framework.jpa.multitenant.test.AbstractMultitenantServiceIT;
import fr.cnes.regards.framework.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.framework.modules.session.commons.dao.ISessionStepRepository;
import fr.cnes.regards.framework.modules.session.commons.dao.ISnapshotProcessRepository;
import fr.cnes.regards.framework.modules.session.commons.domain.SnapshotProcess;
import fr.cnes.regards.framework.modules.session.manager.dao.ISessionManagerRepository;
import fr.cnes.regards.framework.modules.session.manager.dao.ISourceManagerRepository;
import fr.cnes.regards.framework.modules.session.manager.service.clean.snapshotprocess.ManagerCleanSnapshotProcessService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpIOException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import java.util.Arrays;
import java.util.List;

/**
 * @author Iliana Ghazali
 **/
@TestPropertySource(locations = { "classpath:application-test.properties" })
@ContextConfiguration(classes = { JobTestCleaner.class })
public abstract class AbstractManagerServiceUtilsIT extends AbstractMultitenantServiceIT {

    protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractManagerServiceUtilsIT.class);

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
    private IRabbitVirtualHostAdmin vhostAdmin;

    @Autowired
    private IAmqpAdmin amqpAdmin;

    @SpyBean
    protected IPublisher publisher;

    @Autowired
    private ISubscriber subscriber;

    // SESSION
    @Autowired
    protected ManagerCleanSnapshotProcessService managerCleanService;

    /**
     * Repositories
     */

    // JOBS
    @Autowired
    protected IJobInfoRepository jobInfoRepo;
    
    @Autowired
    private JobTestCleaner jobTestCleaner;

    // SESSION MANAGER
    @Autowired
    protected ISessionStepRepository sessionStepRepo;

    @Autowired
    protected ISessionManagerRepository sessionRepo;

    @Autowired
    protected ISourceManagerRepository sourceRepo;

    @Autowired
    protected ISnapshotProcessRepository snapshotProcessRepo;

    /**
     * Parameters
     */

    protected static final String SOURCE_1 = "SOURCE 1";

    protected static final String SOURCE_2 = "SOURCE 2";

    protected static final String SOURCE_3 = "SOURCE 3";

    protected static final String SOURCE_4 = "SOURCE_4";

    protected static final String SOURCE_5 = "SOURCE_5";

    protected static final String SOURCE_6 = "SOURCE_6";

    protected static final String SESSION_1 = "SESSION 1";

    protected static final String SESSION_2 = "SESSION 2";

    protected static final String SESSION_3 = "SESSION 3";

    protected static final String SESSION_4 = "SESSION 4";

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

        cleanRepositories();
        // override this method to custom action performed before
        doInit();
    }

    /**
     * Custom test initialization to override
     *
     * @throws Exception
     */
    protected void doInit() throws Exception {
        // Override to init something
    }

    // -------------
    // AFTER METHODS
    // -------------

    @After
    public void after() throws Exception {
        jobTestCleaner.cleanJob();
        doAfter();
    }

    /**
     * Custom test cleaning to override
     *
     * @throws Exception
     */
    protected void doAfter() throws Exception {
        // Override to init something
    }

    // -------------
    //     AMQP
    // -------------

    /**
     * Internal method to clean AMQP queues, if actives
     */
    private void cleanAMQPQueues(Class<? extends IHandler<?>> handler, Target target) {
        if (vhostAdmin != null) {
            // Purge event queue
            try {
                vhostAdmin.bind(AmqpChannel.AMQP_MULTITENANT_MANAGER);
                amqpAdmin.purgeQueue(amqpAdmin.getSubscriptionQueueName(handler, target), false);
            } catch (AmqpIOException e) {
                LOGGER.warn("Failed to clean AMQP queues");
            } finally {
                vhostAdmin.unbind();
            }
        }
    }

    // -------------
    //     REPO
    // -------------
    private void cleanRepositories() {
        this.sessionStepRepo.deleteAll();
        this.sessionRepo.deleteAll();
        this.sourceRepo.deleteAll();
        this.snapshotProcessRepo.deleteAll();
        this.jobInfoRepo.deleteAll();
    }

    // --------------
    //  SESSION UTILS
    // --------------

    protected void waitForSessionStepEventsStored(int nbEvents) throws InterruptedException {
        long count, now = System.currentTimeMillis(), end = now + 200000L;
        LOGGER.info("Waiting for session steps to be saved ...");
        do {
            count = this.sessionStepRepo.count();
            now = System.currentTimeMillis();
            if (count != nbEvents) {
                Thread.sleep(5000L);
            }
        } while (count != nbEvents && now <= end);

        if (count != nbEvents) {
            Assert.fail(String.format("Events were not stored in database. Expected %d events but was %d.", nbEvents,
                                      count));
        }
    }

    protected void waitForSnapshotUpdateSuccesses() throws InterruptedException {
        long count = 0;
        long now = System.currentTimeMillis(), end = now + 200000L;
        List<SnapshotProcess> snapshotProcessList = this.snapshotProcessRepo.findAll();
        int processSize = snapshotProcessList.size();
        LOGGER.info("Waiting for snapshot update ...");
        do {
            for (SnapshotProcess snapshotProcess : snapshotProcessList) {
                if (snapshotProcess.getLastUpdateDate() != null && snapshotProcess.getJobId() == null) {
                    count++;
                }
            }
            snapshotProcessList = this.snapshotProcessRepo.findAll();
            now = System.currentTimeMillis();
            if (count != processSize) {
                Thread.sleep(5000L);
            }
        } while (count != processSize && now <= end);

        if (count != processSize) {
            Assert.fail("Snapshot processes were not updated correctly. Check in the snapshot that the "
                                + "lastUpdateDate is not null and jobId is null");
        }

    }

    protected void waitForJobStates(String jobName, int nbJobs, long timeout, JobStatus[] jobStatuses)
            throws InterruptedException {
        long count, now = System.currentTimeMillis(), end = now + timeout;
        LOGGER.info("Waiting for {} jobs of type {} to be in at least one of the following states {} ...", nbJobs,
                    jobName, Arrays.toString(jobStatuses));
        do {
            count = jobInfoService.retrieveJobsCount(jobName, jobStatuses);
            now = System.currentTimeMillis();
            if (count != nbJobs) {
                Thread.sleep(100L);
            }
        } while (count != nbJobs && now <= end);

        if (count != nbJobs) {
            Assert.fail(
                    String.format("Unexpected number of snapshot jobs created. Expected %d jobs but was %d.", nbJobs,
                                  this.jobInfoRepo.countByClassNameAndStatusStatusIn(jobName, jobStatuses)));
        }
    }
}