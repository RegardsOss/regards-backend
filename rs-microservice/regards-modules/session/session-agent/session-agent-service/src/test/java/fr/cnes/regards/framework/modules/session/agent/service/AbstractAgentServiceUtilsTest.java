package fr.cnes.regards.framework.modules.session.agent.service;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.configuration.AmqpConstants;
import fr.cnes.regards.framework.amqp.configuration.IAmqpAdmin;
import fr.cnes.regards.framework.amqp.configuration.IRabbitVirtualHostAdmin;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.event.Target;
import fr.cnes.regards.framework.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;
import fr.cnes.regards.framework.modules.jobs.domain.event.JobEvent;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.framework.modules.session.agent.dao.IStepPropertyUpdateRequestRepository;
import fr.cnes.regards.framework.modules.session.agent.domain.events.StepPropertyUpdateRequestEvent;
import fr.cnes.regards.framework.modules.session.agent.service.handlers.SessionAgentEventHandler;
import fr.cnes.regards.framework.modules.session.commons.dao.ISessionStepRepository;
import fr.cnes.regards.framework.modules.session.commons.dao.ISnapshotProcessRepository;
import fr.cnes.regards.framework.modules.session.commons.domain.SnapshotProcess;
import fr.cnes.regards.framework.modules.session.commons.service.jobs.SnapshotJobEventHandler;
import fr.cnes.regards.framework.test.integration.AbstractRegardsServiceTransactionalIT;
import java.util.List;
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
public abstract class AbstractAgentServiceUtilsTest extends AbstractRegardsServiceTransactionalIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractAgentServiceUtilsTest.class);

    @Autowired
    private IRabbitVirtualHostAdmin vhostAdmin;

    @Autowired
    private IAmqpAdmin amqpAdmin;

    @SpyBean
    protected IPublisher publisher;

    @Autowired
    protected ISessionStepRepository sessionStepRepo;

    @Autowired
    protected IStepPropertyUpdateRequestRepository stepPropertyRepo;

    @Autowired
    protected ISnapshotProcessRepository snapshotProcessRepo;

    @Autowired
    protected IJobInfoRepository jobInfoRepo;

    @Autowired
    private ISubscriber subscriber;

    @Autowired
    private IJobInfoService jobInfoService;

    protected static final String SOURCE_1 = "SOURCE 1";

    protected static final String SOURCE_2 = "SOURCE 2";

    protected static final String SOURCE_3 = "SOURCE 3";

    protected static final String OWNER_1 = "OWNER 1";

    protected static final String OWNER_2 = "OWNER 2";

    @Before
    public void init() throws Exception {
        this.stepPropertyRepo.deleteAll();
        this.sessionStepRepo.deleteAll();
        this.snapshotProcessRepo.deleteAll();
        this.jobInfoRepo.deleteAll();
        // override this method to custom action performed before
        doInit();
    }

    @After
    public void after() throws Exception {
        subscriber.unsubscribeFrom(StepPropertyUpdateRequestEvent.class);
        subscriber.unsubscribeFrom(JobEvent.class);
        cleanAMQPQueues(SessionAgentEventHandler.class, Target.ONE_PER_MICROSERVICE_TYPE);
        cleanAMQPQueues(SnapshotJobEventHandler.class, Target.MICROSERVICE);
        Thread.sleep(10000L);
        doAfter();
    }

    /**
     * Custom test initialization to override
     *
     * @throws Exception
     */
    protected void doInit() throws Exception {
        // Override to init something
    }

    /**
     * Custom test cleaning to override
     *
     * @throws Exception
     */
    protected void doAfter() throws Exception {
        // Override to init something
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