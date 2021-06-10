/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.modules.session.management.service;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.configuration.AmqpConstants;
import fr.cnes.regards.framework.amqp.configuration.IAmqpAdmin;
import fr.cnes.regards.framework.amqp.configuration.IRabbitVirtualHostAdmin;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.event.Target;
import fr.cnes.regards.framework.jpa.multitenant.test.AbstractMultitenantServiceTest;
import fr.cnes.regards.framework.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.framework.modules.session.commons.dao.ISessionStepRepository;
import fr.cnes.regards.framework.modules.session.commons.dao.ISnapshotProcessRepository;
import fr.cnes.regards.framework.modules.session.commons.domain.SnapshotProcess;
import fr.cnes.regards.framework.modules.session.commons.domain.events.SessionDeleteEvent;
import fr.cnes.regards.framework.modules.session.commons.domain.events.SessionStepEvent;
import fr.cnes.regards.framework.modules.session.commons.domain.events.SourceDeleteEvent;
import fr.cnes.regards.framework.modules.session.commons.service.delete.SessionDeleteEventHandler;
import fr.cnes.regards.framework.modules.session.commons.service.delete.SourceDeleteEventHandler;
import fr.cnes.regards.framework.modules.session.management.dao.ISessionManagerRepository;
import fr.cnes.regards.framework.modules.session.management.dao.ISourceManagerRepository;
import fr.cnes.regards.framework.modules.session.management.domain.Session;
import fr.cnes.regards.framework.modules.session.management.domain.Source;
import fr.cnes.regards.framework.modules.session.management.service.handlers.SessionManagerHandler;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import java.util.List;
import java.util.Optional;
import org.junit.After;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpIOException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * @author Iliana Ghazali
 **/
@TestPropertySource(locations = { "classpath:application-test.properties" })
@ActiveProfiles({ "testAmqp", "noscheduler" })
public abstract class AbstractManagerServiceUtilsTest extends AbstractMultitenantServiceTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractManagerServiceUtilsTest.class);

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

    /**
     * Repositories
     */

    // JOBS
    @Autowired
    protected IJobInfoRepository jobInfoRepo;

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

    protected static final String SESSION_1 = "SESSION 1";

    protected static final String SESSION_2 = "SESSION 2";

    protected static final String SESSION_3 = "SESSION 3";

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
        clearQueues();
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

    private void clearQueues() throws InterruptedException {
        subscriber.unsubscribeFrom(SessionStepEvent.class);
        subscriber.unsubscribeFrom(SessionDeleteEvent.class);
        subscriber.unsubscribeFrom(SourceDeleteEvent.class);

        cleanAMQPQueues(SessionManagerHandler.class, Target.ONE_PER_MICROSERVICE_TYPE);
        cleanAMQPQueues(SourceDeleteEventHandler.class, Target.ONE_PER_MICROSERVICE_TYPE);
        cleanAMQPQueues(SessionDeleteEventHandler.class, Target.ONE_PER_MICROSERVICE_TYPE);
        Thread.sleep(5000L);
    }

    /**
     * Internal method to clean AMQP queues, if actives
     */
    private void cleanAMQPQueues(Class<? extends IHandler<?>> handler, Target target) {
        if (vhostAdmin != null) {
            // Purge event queue
            try {
                vhostAdmin.bind(AmqpConstants.AMQP_MULTITENANT_MANAGER);
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

    protected boolean waitForSessionStepEventsStored(int nbEvents) throws InterruptedException {
        long count, now = System.currentTimeMillis(), end = now + 200000L;
        LOGGER.info("Waiting for session steps to be saved ...");
        do {
            count = this.sessionStepRepo.count();
            now = System.currentTimeMillis();
            if (count != nbEvents) {
                Thread.sleep(5000L);
            }
        } while (count != nbEvents && now <= end);
        return count == nbEvents;
    }

    protected boolean waitForSnapshotUpdateSuccesses() throws InterruptedException {
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
        return count == processSize;
    }

    protected boolean waitForJobSuccesses(String jobName, int nbJobs, long timeout) throws InterruptedException {
        long count, now = System.currentTimeMillis(), end = now + timeout;
        LOGGER.info("Waiting for jobs to be in success state ...");
        do {
            count = jobInfoService.retrieveJobsCount(jobName, JobStatus.SUCCEEDED);
            now = System.currentTimeMillis();
            if (count != nbJobs) {
                Thread.sleep(5000L);
            }
        } while (count != nbJobs && now <= end);
        return count == nbJobs;
    }

    protected boolean waitForSourceDeleted(String sourceName, long timeout) throws InterruptedException {
        long now = System.currentTimeMillis(), end = now + timeout;
        Optional<Source> source;
        LOGGER.info("Waiting for source deletion ...");
        do {
            source = this.sourceRepo.findByName(sourceName);
            if (source.isPresent()) {
                Thread.sleep(10000L);
            }
            now = System.currentTimeMillis();
        } while (source.isPresent() && now <= end);

        return !source.isPresent();
    }

    protected boolean waitForSessionDeleted(String sourceName, String sessionName) throws InterruptedException {
        long now = System.currentTimeMillis(), end = now + 200000L;
        Optional<Session> session;
        LOGGER.info("Waiting for session deletion ...");
        do {
            session = this.sessionRepo.findBySourceAndName(sourceName, sessionName);
            if (session.isPresent()) {
                Thread.sleep(10000L);
            }
            now = System.currentTimeMillis();
        } while (session.isPresent() && now <= end);
        return !session.isPresent();
    }
}