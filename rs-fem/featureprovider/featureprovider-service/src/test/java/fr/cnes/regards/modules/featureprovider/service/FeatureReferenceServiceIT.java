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

import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.assertj.core.util.Lists;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.mockito.Mockito;
import org.springframework.amqp.AmqpIOException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.annotation.DirtiesContext.HierarchyMode;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.ui.Model;
import org.springframework.util.MimeType;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.configuration.AmqpConstants;
import fr.cnes.regards.framework.amqp.configuration.IAmqpAdmin;
import fr.cnes.regards.framework.amqp.configuration.IRabbitVirtualHostAdmin;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.event.Target;
import fr.cnes.regards.framework.geojson.geometry.IGeometry;
import fr.cnes.regards.framework.jpa.multitenant.test.AbstractMultitenantServiceTest;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.dao.IPluginConfigurationRepository;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.framework.utils.plugins.exception.NotAvailablePluginConfigurationException;
import fr.cnes.regards.modules.feature.domain.request.AbstractFeatureRequest;
import fr.cnes.regards.modules.feature.domain.request.AbstractRequest;
import fr.cnes.regards.modules.feature.domain.request.FeatureRequestStep;
import fr.cnes.regards.modules.feature.dto.Feature;
import fr.cnes.regards.modules.feature.dto.FeatureCreationSessionMetadata;
import fr.cnes.regards.modules.feature.dto.FeatureFile;
import fr.cnes.regards.modules.feature.dto.FeatureFileAttributes;
import fr.cnes.regards.modules.feature.dto.FeatureFileLocation;
import fr.cnes.regards.modules.feature.dto.PriorityLevel;
import fr.cnes.regards.modules.feature.dto.StorageMetadata;
import fr.cnes.regards.modules.feature.dto.event.in.FeatureCreationRequestEvent;
import fr.cnes.regards.modules.feature.dto.event.in.FeatureDeletionRequestEvent;
import fr.cnes.regards.modules.feature.dto.event.in.FeatureReferenceRequestEvent;
import fr.cnes.regards.modules.feature.dto.event.out.RequestState;
import fr.cnes.regards.modules.feature.dto.urn.FeatureUniformResourceName;
import fr.cnes.regards.modules.featureprovider.dao.IFeatureReferenceRequestRepository;
import fr.cnes.regards.modules.featureprovider.service.conf.FeatureConfigurationProperties;
import fr.cnes.regards.modules.model.dto.properties.IProperty;
import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author kevin
 *
 */
@TestPropertySource(
        properties = { "spring.jpa.properties.hibernate.default_schema=feature_reference", "regards.amqp.enabled=true",
                "spring.task.scheduling.pool.size=2", "regards.feature.metrics.enabled=true" },
        locations = { "classpath:regards_perf.properties", "classpath:batch.properties",
                "classpath:metrics.properties" })
@ActiveProfiles(value = { "testAmqp" })
//Clean all context (schedulers)
@DirtiesContext(classMode = ClassMode.AFTER_CLASS, hierarchyMode = HierarchyMode.EXHAUSTIVE)
public class FeatureReferenceServiceIT extends AbstractMultitenantServiceTest {

    @Autowired
    protected IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    protected FeatureConfigurationProperties properties;

    @Autowired
    protected ISubscriber subscriber;

    @Autowired
    protected IPublisher publisher;

    @Autowired
    private IFeatureReferenceRequestRepository referenceRequestRepo;

    @Autowired
    private IPluginConfigurationRepository pluginConfRepo;

    @SpyBean
    private IPluginService pluginService;

    @Autowired(required = false)
    private IAmqpAdmin amqpAdmin;

    @Autowired(required = false)
    private IRabbitVirtualHostAdmin vhostAdmin;

    @Before
    public void setup() throws InterruptedException {
        this.referenceRequestRepo.deleteAll();
        this.pluginConfRepo.deleteAll();
        simulateApplicationReadyEvent();
    }

    @Test
    public void testProcessReference() {
        PluginConfiguration recipientPlugin = new PluginConfiguration();
        recipientPlugin.setBusinessId("testFeatureGeneration");
        recipientPlugin.setVersion("1.0.0");
        recipientPlugin.setLabel("test recipient");
        recipientPlugin.setPluginId("DefaultFeatureGenerator");
        recipientPlugin = this.pluginConfRepo.save(recipientPlugin);

        List<FeatureReferenceRequestEvent> eventsToPublish = new ArrayList<>();
        for (int i = 0; i < this.properties.getMaxBulkSize(); i++) {
            JsonObject parameters = new JsonObject();
            parameters.add("location", new JsonPrimitive("test" + i));
            eventsToPublish.add(FeatureReferenceRequestEvent.build("bibi",
                                                                   FeatureCreationSessionMetadata.build("bibi",
                                                                                                        "session",
                                                                                                        PriorityLevel.NORMAL,
                                                                                                        false,
                                                                                                        new StorageMetadata[0]),
                                                                   parameters,
                                                                   "testFeatureGeneration"));
        }
        this.publisher.publish(eventsToPublish);

        // this.waitRequest(this.featureCreationRequestRepo, this.properties.getMaxBulkSize(), 60000);
//        this.waitRequest(this.featureRepo, this.properties.getMaxBulkSize(), 120_000);

        assertEquals(0, this.referenceRequestRepo.count());
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

        List<FeatureReferenceRequestEvent> eventsToPublish = new ArrayList<>();
        for (int i = 0; i < this.properties.getMaxBulkSize(); i++) {
            JsonObject parameters = new JsonObject();
            parameters.add("location", new JsonPrimitive("test" + i));
            eventsToPublish.add(FeatureReferenceRequestEvent.build("bibi",
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
        this.waitRequest(this.referenceRequestRepo, this.properties.getMaxBulkSize(), 60000);
        waitForErrorState(this.referenceRequestRepo);
        // no feature creation should be in database
//        Assertions.assertEquals(0, this.featureCreationRequestRepo.count());

    }

    /**
     * Internal method to clean AMQP queues, if actives
     */
    public void cleanAMQPQueues(Class<? extends IHandler<?>> handler, Target target) {
        if (vhostAdmin != null) {
            // Re-set tenant because above simulation clear it!

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

    protected void waitForErrorState(JpaRepository<? extends AbstractRequest, ?> repo)

            throws InterruptedException {
        int cpt = 0;

        // we will expect that all feature reference remain in database with the error state
        do {
            Thread.sleep(1000);
            if (cpt == 60) {
                fail("Timeout");
            }
            cpt++;
        } while (!repo.findAll().stream().allMatch(request -> RequestState.ERROR.equals(request.getState())));
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
        subscriber.unsubscribeFrom(FeatureReferenceRequestEvent.class);
        cleanAMQPQueues(FeatureReferenceRequestEventHandler.class, Target.ONE_PER_MICROSERVICE_TYPE);
    }

}
