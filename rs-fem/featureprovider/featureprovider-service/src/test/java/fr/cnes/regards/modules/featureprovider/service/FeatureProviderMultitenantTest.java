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
package fr.cnes.regards.modules.featureprovider.service;

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
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.framework.modules.plugins.dao.IPluginConfigurationRepository;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.modules.session.agent.dao.IStepPropertyUpdateRequestRepository;
import fr.cnes.regards.framework.modules.session.agent.domain.events.StepPropertyEventTypeEnum;
import fr.cnes.regards.framework.modules.session.agent.domain.events.StepPropertyUpdateRequestEvent;
import fr.cnes.regards.framework.modules.session.agent.domain.update.StepPropertyUpdateRequest;
import fr.cnes.regards.framework.modules.session.agent.domain.update.StepPropertyUpdateRequestInfo;
import fr.cnes.regards.framework.modules.session.commons.dao.ISessionStepRepository;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.feature.client.FeatureClient;
import fr.cnes.regards.modules.feature.client.FeatureRequestEventHandler;
import fr.cnes.regards.modules.feature.domain.request.AbstractRequest;
import fr.cnes.regards.modules.feature.dto.FeatureRequestStep;
import fr.cnes.regards.modules.feature.dto.event.out.FeatureRequestEvent;
import fr.cnes.regards.modules.feature.dto.event.out.RequestState;
import fr.cnes.regards.modules.featureprovider.dao.IFeatureExtractionRequestRepository;
import fr.cnes.regards.modules.featureprovider.domain.FeatureExtractionRequestEvent;
import fr.cnes.regards.modules.featureprovider.service.conf.FeatureProviderConfigurationProperties;
import org.junit.After;
import org.junit.Assert;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.mockito.Spy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpIOException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Utils for tests
 *
 * @author Iliana Ghazali
 **/
public abstract class FeatureProviderMultitenantTest extends AbstractMultitenantServiceTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureProviderMultitenantTest.class);

    /**
     * Services
     */
    @Autowired
    protected IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    protected FeatureProviderConfigurationProperties properties;

    // AMQP
    @Autowired
    protected ISubscriber subscriber;

    @Autowired
    protected IPublisher publisher;

    @Autowired(required = false)
    protected IAmqpAdmin amqpAdmin;

    @Autowired(required = false)
    protected IRabbitVirtualHostAdmin vhostAdmin;

    // FEATURE
    @Autowired
    protected IFeatureExtractionService featureReferenceService;

    @Spy
    protected FeatureClient featureClient;


    // PLUGINS
    @SpyBean
    protected IPluginService pluginService;

    // JOBS
    @Autowired
    private IJobInfoService jobInfoService;

    /**
     * Repositories
     */

    // REQUESTS
    @Autowired
    protected IFeatureExtractionRequestRepository extractionRequestRepo;

    @Autowired
    protected IFeatureExtractionRequestRepository referenceRequestRepo;

    // PLUGINS
    @Autowired
    protected IPluginConfigurationRepository pluginConfRepo;

    // SESSION AGENT
    @Autowired
    protected IStepPropertyUpdateRequestRepository stepRepo;

    @Autowired
    protected ISessionStepRepository sessionStepRepo;

    // JOBS
    @Autowired
    private IJobInfoRepository jobInfoRepo;


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
        // clean repositories
        cleanRepositories();
        // override this method to custom action performed before
        doInit();
    }

    /**
     * Custom test initialization to override
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
        cleanAMQP();
        // override this method to custom action performed after
        doAfter();
    }

    /**
     * Custom test cleaning to override
     * @throws Exception
     */
    protected void doAfter() throws Exception {
        // Override to init something
    }

    // -------------
    //     AMQP
    // -------------

    private void cleanAMQP() throws InterruptedException {
        subscriber.unsubscribeFrom(FeatureExtractionRequestEvent.class);
        subscriber.unsubscribeFrom(FeatureRequestEvent.class);
        subscriber.unsubscribeFrom(StepPropertyUpdateRequestEvent.class);

        cleanAMQPQueues(FeatureExtractionRequestEventHandler.class, Target.ONE_PER_MICROSERVICE_TYPE);
        cleanAMQPQueues(FeatureRequestEventHandler.class, Target.ONE_PER_MICROSERVICE_TYPE);
        cleanAMQPQueues(StepPropertyUpdateRequestEvent.class, Target.MICROSERVICE, WorkerMode.UNICAST);

        Thread.sleep(2000L);
    }

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
        // session agent
        this.stepRepo.deleteAll();
        this.sessionStepRepo.deleteAll();

        // features
        this.extractionRequestRepo.deleteAll();
        this.referenceRequestRepo.deleteAll();
        this.pluginConfRepo.deleteAll();

        // jobs
        jobInfoRepo.deleteAll();
    }

    // -------------
    //     UTILS
    // -------------

    protected void waitForState(JpaRepository<? extends AbstractRequest, ?> repo, RequestState state)
            throws InterruptedException {
        int cpt = 0;

        // we will expect that all feature reference remain in database with the error state
        do {
            Thread.sleep(1000);
            if (cpt == 60) {
                fail("Timeout");
            }
            cpt++;
        } while (!repo.findAll().stream().allMatch(request -> state.equals(request.getState())));
    }

    protected void waitForState(JpaRepository<? extends AbstractRequest, ?> repo, RequestState state, int nbReq)
            throws InterruptedException {
        int cpt = 0;

        // we will expect that all feature reference remain in database with the error state
        do {
            Thread.sleep(1000);
            if (cpt == 60) {
                fail("Timeout");
            }
            cpt++;
        } while (repo.findAll().stream().filter(request -> state.equals(request.getState())).count() != nbReq);
    }

    protected void waitForStep(JpaRepository<? extends AbstractRequest, ?> repo, FeatureRequestStep step, int timeout)
            throws InterruptedException {
        int cpt = 0;

        // we will expect that all feature reference remain in database with the error state
        do {
            Thread.sleep(1000);
            if (cpt == (timeout / 1000)) {
                fail("Timeout");
            }
            cpt++;
        } while (!repo.findAll().stream().allMatch(request -> step.equals(request.getStep())));
    }

    protected void waitRequest(JpaRepository<?, ?> repo, long expected, long timeout) {
        long end = System.currentTimeMillis() + timeout;
        // Wait
        long entityCount;
        do {
            entityCount = repo.count();
            logger.trace("{} request(s) remain(s) in database", entityCount);
            if (entityCount == expected) {
                break;
            }
            long now = System.currentTimeMillis();
            if (end > now) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    logger.error(String.format("Thread interrupted %s expected in database, %s really ", expected,
                                               entityCount));
                    Assert.fail(String.format("Thread interrupted {} expected in database, {} really ", expected,
                                              entityCount));

                }
            } else {
                logger.error(String.format("Thread interrupted %s expected in database, %s really ", expected,
                                           entityCount));
                Assert.fail("Timeout");
            }
        } while (true);
    }

    protected boolean waitForJobSuccesses(String jobName, int nbJobs, long timeout) throws InterruptedException {
        long count, now = System.currentTimeMillis(), end = now + timeout;
        logger.info("Waiting for jobs to be in success state ...");
        do {
            count = jobInfoService.retrieveJobsCount(jobName, JobStatus.SUCCEEDED);
            now = System.currentTimeMillis();
            if (count != nbJobs) {
                Thread.sleep(5000L);
            }
        } while (count != nbJobs && now <= end);
        return count == nbJobs;
    }

    /**
     * Method to check properties of StepPropertyUpdateRequestEvents
     */
    protected void checkStepEvent(StepPropertyUpdateRequest step, String expectedProperty, String expectedValue,
            StepPropertyEventTypeEnum expectedType, String expectedSessionOwner, String expectedSession) {
        StepPropertyUpdateRequestInfo stepInfo = step.getStepPropertyInfo();
        Assert.assertEquals("This property was not expected. Check the StepPropertyUpdateRequestEvent workflow.",
                            expectedProperty, stepInfo.getProperty());
        Assert.assertEquals("This value was not expected. Check the StepPropertyUpdateRequestEvent workflow.",
                            expectedValue, stepInfo.getValue());
        Assert.assertEquals("This type was not expected. Check the StepPropertyUpdateRequestEvent workflow.",
                            expectedType, step.getType());
        Assert.assertEquals("This sessionOwner was not expected. Check the StepPropertyUpdateRequestEvent workflow.",
                            expectedSessionOwner, step.getSource());
        Assert.assertEquals("This session was not expected. Check the StepPropertyUpdateRequestEvent workflow.",
                            expectedSession, step.getSession());
    }
}