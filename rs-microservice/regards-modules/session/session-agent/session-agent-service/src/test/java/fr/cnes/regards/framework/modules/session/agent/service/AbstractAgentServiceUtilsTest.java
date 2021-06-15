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
package fr.cnes.regards.framework.modules.session.agent.service;

import com.google.common.base.Strings;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.configuration.AmqpConstants;
import fr.cnes.regards.framework.amqp.configuration.IAmqpAdmin;
import fr.cnes.regards.framework.amqp.configuration.IRabbitVirtualHostAdmin;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.event.Target;
import fr.cnes.regards.framework.amqp.event.WorkerMode;
import fr.cnes.regards.framework.jpa.multitenant.test.AbstractMultitenantServiceTest;
import fr.cnes.regards.framework.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;
import fr.cnes.regards.framework.modules.jobs.domain.event.JobEvent;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.framework.modules.session.agent.dao.IStepPropertyUpdateRequestRepository;
import fr.cnes.regards.framework.modules.session.agent.domain.events.StepPropertyUpdateRequestEvent;
import fr.cnes.regards.framework.modules.session.agent.service.clean.sessionstep.AgentCleanSessionStepService;
import fr.cnes.regards.framework.modules.session.agent.service.update.AgentSnapshotService;
import fr.cnes.regards.framework.modules.session.commons.dao.ISessionStepRepository;
import fr.cnes.regards.framework.modules.session.commons.dao.ISnapshotProcessRepository;
import fr.cnes.regards.framework.modules.session.commons.domain.SnapshotProcess;
import fr.cnes.regards.framework.modules.session.commons.service.jobs.SnapshotJobEventHandler;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpIOException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.TestPropertySource;

/**
 * Utils for tests
 *
 * @author Iliana Ghazali
 **/
@TestPropertySource(locations = { "classpath:application-test.properties" })
public abstract class AbstractAgentServiceUtilsTest extends AbstractMultitenantServiceTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractAgentServiceUtilsTest.class);

    /**
     * Services
     */

    // JOBS
    @Autowired
    private IJobInfoService jobInfoService;

    // AMQP
    @Autowired
    private IRabbitVirtualHostAdmin vhostAdmin;

    @Autowired
    private IAmqpAdmin amqpAdmin;

    @SpyBean
    protected IPublisher publisher;

    @Autowired
    private ISubscriber subscriber;

    // SESSION AGENT

    @Autowired
    protected AgentSnapshotService agentSnapshotService;

    @Autowired
    protected AgentCleanSessionStepService agentCleanSessionStepService;


    /**
     * Repositories
     */

    // JOBS
    @Autowired
    protected IJobInfoRepository jobInfoRepo;

    // SESSION AGENT
    @Autowired
    protected ISessionStepRepository sessionStepRepo;

    @Autowired
    protected IStepPropertyUpdateRequestRepository stepPropertyRepo;

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

    protected static final String OWNER_1 = "OWNER 1";

    protected static final String OWNER_2 = "OWNER 2";

    protected static final String OWNER_3 = "OWNER 3";


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
        subscriber.unsubscribeFrom(StepPropertyUpdateRequestEvent.class);
        subscriber.unsubscribeFrom(JobEvent.class);
        cleanAMQPQueues(StepPropertyUpdateRequestEvent.class, Target.MICROSERVICE, WorkerMode.UNICAST);
        cleanAMQPQueues(SnapshotJobEventHandler.class, Target.MICROSERVICE);
        Thread.sleep(5000L);
    };

    /**
     * Clean AMQP by default with {@link WorkerMode#BROADCAST}
     */
    public void cleanAMQPQueues(Class<?> type, Target target) {
        cleanAMQPQueues(type, target, WorkerMode.BROADCAST);
    }

    /**
     * Internal method to clean AMQP queues, if actives
     * @param type handler or event class, depending on the type of event
     */
    public void cleanAMQPQueues(Class<?> type, Target target, WorkerMode mode) {
        if (vhostAdmin != null) {
            // Re-set tenant because above simulation clear it!

            // Purge event queue
            try {
                vhostAdmin.bind(AmqpConstants.AMQP_MULTITENANT_MANAGER);
                // get queue name
                String queueName = null;
                if (mode.equals(WorkerMode.BROADCAST)) {
                    queueName = amqpAdmin.getSubscriptionQueueName((Class<? extends IHandler<?>>) type, target);
                } else if(mode.equals(WorkerMode.UNICAST)){
                    queueName = amqpAdmin.getUnicastQueueName(runtimeTenantResolver.getTenant(), type, target);
                }
                // clean queue
                if (!Strings.isNullOrEmpty(queueName)) {
                    amqpAdmin.purgeQueue(queueName, false);
                    LOGGER.info("Queue {} was cleaned", queueName);
                }
            } catch (AmqpIOException e) {
                LOGGER.warn("Failed to clean AMQP queues", e);
            } finally {
                vhostAdmin.unbind();
            }
        }
    }

    // -------------
    //     REPO
    // -------------

    private void cleanRepositories() {
        this.stepPropertyRepo.deleteAll();
        this.sessionStepRepo.deleteAll();
        this.snapshotProcessRepo.deleteAll();
        this.jobInfoRepo.deleteAll();
    }

    // --------------
    //  SESSION UTILS
    // --------------

    protected boolean waitForStepPropertyEventsStored(int nbEvents) throws InterruptedException {
        long count, now = System.currentTimeMillis(), end = now + 200000L;
        LOGGER.info("Waiting for step property requests to be saved ...");
        do {
            count = this.stepPropertyRepo.count();
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
}