/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.springframework.amqp.AmqpIOException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.annotation.DirtiesContext.HierarchyMode;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.configuration.AmqpConstants;
import fr.cnes.regards.framework.amqp.configuration.IAmqpAdmin;
import fr.cnes.regards.framework.amqp.configuration.IRabbitVirtualHostAdmin;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.event.Target;
import fr.cnes.regards.framework.jpa.multitenant.test.AbstractMultitenantServiceTest;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.dao.IPluginConfigurationRepository;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.utils.plugins.exception.NotAvailablePluginConfigurationException;
import fr.cnes.regards.modules.accessrights.client.IProjectUsersClient;
import fr.cnes.regards.modules.feature.client.FeatureClient;
import fr.cnes.regards.modules.feature.client.FeatureRequestEventHandler;
import fr.cnes.regards.modules.feature.domain.request.AbstractRequest;
import fr.cnes.regards.modules.feature.domain.request.FeatureRequestStep;
import fr.cnes.regards.modules.feature.dto.FeatureCreationSessionMetadata;
import fr.cnes.regards.modules.feature.dto.PriorityLevel;
import fr.cnes.regards.modules.feature.dto.StorageMetadata;
import fr.cnes.regards.modules.featureprovider.domain.FeatureExtractionRequestEvent;
import fr.cnes.regards.modules.feature.dto.event.out.FeatureRequestEvent;
import fr.cnes.regards.modules.feature.dto.event.out.FeatureRequestType;
import fr.cnes.regards.modules.feature.dto.event.out.RequestState;
import fr.cnes.regards.modules.featureprovider.dao.IFeatureExtractionRequestRepository;
import fr.cnes.regards.modules.featureprovider.service.conf.FeatureConfigurationProperties;
import fr.cnes.regards.modules.model.client.IModelAttrAssocClient;
import fr.cnes.regards.modules.model.client.IModelClient;
import fr.cnes.regards.modules.project.client.rest.IProjectsClient;
import static org.junit.Assert.fail;

/**
 * @author kevin
 *
 */
@TestPropertySource(
        properties = { "spring.jpa.properties.hibernate.default_schema=feature_reference", "regards.amqp.enabled=true",
                "spring.task.scheduling.pool.size=2", "zuul.prefix=zuulPrefix" },
        locations = { "classpath:regards_perf.properties", "classpath:batch.properties" })
@ActiveProfiles(value = { "testAmqp", "noscheduler" })
//Clean all context (schedulers)
@DirtiesContext(classMode = ClassMode.AFTER_CLASS, hierarchyMode = HierarchyMode.EXHAUSTIVE)
public class FeatureReferenceServiceIT extends AbstractMultitenantServiceTest {

    @Configuration
    static class Config {

        @Bean
        public IProjectsClient projectsClient() {
            return Mockito.mock(IProjectsClient.class);
        }

        @Bean
        public IProjectUsersClient projectUsersClient() {
            return Mockito.mock(IProjectUsersClient.class);
        }

        @Bean
        public IModelAttrAssocClient modelAttrAssocClient() {
            return Mockito.mock(IModelAttrAssocClient.class);
        }

        @Bean
        public IModelClient modelClient() {
            return Mockito.mock(IModelClient.class);
        }

    }

    @Autowired
    protected IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    protected FeatureConfigurationProperties properties;

    @Autowired
    protected ISubscriber subscriber;

    @Autowired
    protected IPublisher publisher;

    @Autowired
    private IFeatureExtractionRequestRepository referenceRequestRepo;

    @Autowired
    private IFeatureExtractionService featureReferenceService;

    @Autowired
    private IPluginConfigurationRepository pluginConfRepo;

    @SpyBean
    private IPluginService pluginService;

    @Autowired(required = false)
    private IAmqpAdmin amqpAdmin;

    @Autowired(required = false)
    private IRabbitVirtualHostAdmin vhostAdmin;

    @Spy
    private FeatureClient featureClient;

    @Before
    public void setup() throws InterruptedException {
        cleanAMQP();
        this.referenceRequestRepo.deleteAll();
        this.pluginConfRepo.deleteAll();
        simulateApplicationReadyEvent();
    }

    @Test
    public void testProcessReference() throws InterruptedException {
        PluginConfiguration recipientPlugin = new PluginConfiguration();
        recipientPlugin.setBusinessId("testFeatureGeneration");
        recipientPlugin.setVersion("1.0.0");
        recipientPlugin.setLabel("test recipient");
        recipientPlugin.setPluginId("DefaultFeatureGenerator");
        recipientPlugin = this.pluginConfRepo.save(recipientPlugin);

        List<FeatureExtractionRequestEvent> eventsToPublish = new ArrayList<>();
        List<FeatureRequestEvent> creationGrantedToPublish = new ArrayList<>();
        for (int i = 0; i < this.properties.getMaxBulkSize(); i++) {
            JsonObject parameters = new JsonObject();
            parameters.add("location", new JsonPrimitive("test" + i));
            FeatureExtractionRequestEvent referenceEvent = FeatureExtractionRequestEvent.build("bibi",
                                                                                               FeatureCreationSessionMetadata
                                                                                                     .build("bibi",
                                                                                                            "session",
                                                                                                            PriorityLevel.NORMAL,
                                                                                                            false,
                                                                                                            new StorageMetadata[0]),
                                                                                               parameters,
                                                                                               "testFeatureGeneration");
            eventsToPublish.add(referenceEvent);
            creationGrantedToPublish.add(FeatureRequestEvent.build(FeatureRequestType.CREATION,
                                                                   referenceEvent.getRequestId(),
                                                                   referenceEvent.getRequestOwner(),
                                                                   null,
                                                                   null,
                                                                   RequestState.GRANTED));
        }
        this.publisher.publish(eventsToPublish);
        // lets wait until all requests are registered
        this.waitRequest(referenceRequestRepo, properties.getMaxBulkSize(), 60_000);
        // once this is done, lets schedule all requests
        featureReferenceService.scheduleRequests();
        // wait for all jobs to be finished it means all requests are in step REMOTE_CREATION_REQUESTED
        waitForStep(referenceRequestRepo, FeatureRequestStep.REMOTE_CREATION_REQUESTED, 120_000);
        // now simulate that every request has been successfully granted by feature module
        publisher.publish(creationGrantedToPublish);
        // then lets wait for the DB to be empty
        this.waitRequest(referenceRequestRepo, 0, 10_000);
    }

    @Test
    public void testProcessReferenceWithErrors()
            throws ModuleException, NotAvailablePluginConfigurationException, InterruptedException {
        PluginConfiguration recipientPlugin = new PluginConfiguration();
        recipientPlugin.setBusinessId("testFeatureGeneration");
        recipientPlugin.setVersion("1.0.0");
        recipientPlugin.setLabel("test recipient");
        recipientPlugin.setPluginId("DefaultFeatureGenerator");
        recipientPlugin = this.pluginConfRepo.save(recipientPlugin);

        List<FeatureExtractionRequestEvent> eventsToPublish = new ArrayList<>();
        for (int i = 0; i < this.properties.getMaxBulkSize(); i++) {
            JsonObject parameters = new JsonObject();
            parameters.add("location", new JsonPrimitive("test" + i));
            eventsToPublish.add(FeatureExtractionRequestEvent.build("bibi",
                                                                    FeatureCreationSessionMetadata.build("bibi",
                                                                                                        "session",
                                                                                                        PriorityLevel.NORMAL,
                                                                                                        false,
                                                                                                        new StorageMetadata[0]),
                                                                    parameters,
                                                                    "testFeatureGeneration"));
        }
        this.publisher.publish(eventsToPublish);

        Mockito.doThrow(new ModuleException("")).when(pluginService).getPlugin(Mockito.anyString());
        // lets wait until all requests are registered
        this.waitRequest(referenceRequestRepo, properties.getMaxBulkSize(), 60_000);
        // once this is done, lets schedule all requests
        featureReferenceService.scheduleRequests();
        // now lets wait for request to be in error
        waitForState(this.referenceRequestRepo, RequestState.ERROR);
        Mockito.verify(featureClient, Mockito.times(0)).createFeatures(Mockito.anyList());
    }

    /**
     * Internal method to clean AMQP queues, if actives
     */
    public void cleanAMQPQueues(Class<? extends IHandler<?>> handler, Target target) {
        if (vhostAdmin != null) {
            // Re-set tenant because above simulation clear it!
            runtimeTenantResolver.forceTenant(getDefaultTenant());
            // Purge event queue
            try {
                vhostAdmin.bind(AmqpConstants.AMQP_MULTITENANT_MANAGER);
                amqpAdmin.purgeQueue(amqpAdmin.getSubscriptionQueueName(handler, target), false);
            } catch (AmqpIOException e) {
                //todo
            } finally {
                vhostAdmin.unbind();
            }
        }
    }

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

    protected void waitForStep(JpaRepository<? extends AbstractRequest, ?> repo, FeatureRequestStep step, int timeout)
            throws InterruptedException {
        int cpt = 0;

        // we will expect that all feature reference remain in database with the error state
        do {
            Thread.sleep(1000);
            if (cpt == timeout / 1000) {
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
                    logger.error(String.format("Thread interrupted %s expected in database, %s really ",
                                               expected,
                                               entityCount));
                    Assert.fail(String.format("Thread interrupted {} expected in database, {} really ",
                                              expected,
                                              entityCount));

                }
            } else {
                logger.error(String.format("Thread interrupted %s expected in database, %s really ",
                                           expected,
                                           entityCount));
                Assert.fail("Timeout");
            }
        } while (true);
    }

    @After
    public void after() {
        subscriber.unsubscribeFrom(FeatureExtractionRequestEvent.class);
        cleanAMQP();
    }

    private void cleanAMQP() {
        cleanAMQPQueues(FeatureExtractionRequestEventHandler.class, Target.ONE_PER_MICROSERVICE_TYPE);
        cleanAMQPQueues(FeatureRequestEventHandler.class, Target.ONE_PER_MICROSERVICE_TYPE);
    }

}
