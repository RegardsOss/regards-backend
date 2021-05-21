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

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import fr.cnes.regards.framework.amqp.event.Target;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.session.commons.domain.events.SessionDeleteEvent;
import fr.cnes.regards.framework.modules.session.commons.domain.events.SourceDeleteEvent;
import fr.cnes.regards.framework.modules.session.commons.service.delete.SessionDeleteEventHandler;
import fr.cnes.regards.framework.modules.session.commons.service.delete.SourceDeleteEventHandler;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.utils.plugins.exception.NotAvailablePluginConfigurationException;
import fr.cnes.regards.modules.feature.dto.FeatureCreationSessionMetadata;
import fr.cnes.regards.modules.feature.dto.PriorityLevel;
import fr.cnes.regards.modules.feature.dto.StorageMetadata;
import fr.cnes.regards.modules.feature.dto.event.out.FeatureRequestEvent;
import fr.cnes.regards.modules.feature.dto.event.out.FeatureRequestType;
import fr.cnes.regards.modules.feature.dto.event.out.RequestState;
import fr.cnes.regards.modules.featureprovider.domain.FeatureExtractionRequestEvent;
import fr.cnes.regards.modules.featureprovider.service.session.SessionDeleteService;
import fr.cnes.regards.modules.featureprovider.service.session.SourceDeleteService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * Test for {@link SourceDeleteService} and {@link SessionDeleteService}
 *
 * @author Iliana Ghazali
 **/
@TestPropertySource(
        properties = { "spring.jpa.properties.hibernate" + ".default_schema=featureprovider_session_deletion_it",
                "regards.amqp.enabled=true", "spring.task.scheduling.pool.size=2", "zuul.prefix=zuulPrefix",
                "regards.feature.provider.max.bulk.size=10" },
        locations = { "classpath:regards_perf.properties", "classpath:batch.properties" })
@ActiveProfiles(value = { "testAmqp", "noscheduler" })
//Clean all context (schedulers)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS,
        hierarchyMode = DirtiesContext.HierarchyMode.EXHAUSTIVE)
public class SessionDeletionIT extends FeatureProviderMultitenantTest {

    private static final String SOURCE_1 = "SOURCE 1";

    private static final String SOURCE_2 = "SOURCE 2";

    private static final String SESSION_1 = "SESSION 1";

    private static final String SESSION_2 = "SESSION 2";

    @Autowired
    private SessionDeleteService sessionDeleteService;

    @Autowired
    private SourceDeleteService sourceDeleteService;

    @Override
    public void doInit() throws InterruptedException, ModuleException, NotAvailablePluginConfigurationException {
        initRequests();
    }

    @Test
    @Purpose("Assert a session is correctly deleted. Only requests other than in error and denied states should "
            + "remain.")
    public void deleteSessionTest() throws InterruptedException {
        sessionDeleteService.deleteSession(SOURCE_1, SESSION_1);
        waitForJobSuccesses(FeatureExtractionDeletionJob.class.getName(), 1, 30000);

        // assert session is correctly deleted
        Set<RequestState> statesDeleted = new HashSet<>(Arrays.asList(RequestState.DENIED, RequestState.ERROR));
        Assert.assertEquals(String.format("All requests in %s from source %s and session %s should have been deleted",
                                          statesDeleted, SOURCE_1, SESSION_1), 0L, this.referenceRequestRepo
                                    .findByMetadataSessionOwnerAndMetadataSessionAndStateIn(SOURCE_1, SESSION_1,
                                                                                            statesDeleted, PageRequest
                                                                                                    .of(0, properties
                                                                                                            .getMaxBulkSize()))
                                    .getTotalPages());
        // assert other sessions are still present
        Set<RequestState> allStates = new HashSet<>(
                Arrays.asList(RequestState.DENIED, RequestState.ERROR, RequestState.GRANTED, RequestState.SUCCESS));
        Assert.assertNotEquals(
                String.format("All requests from source %s and session %s should be present", SOURCE_1, SESSION_1), 0L,
                this.referenceRequestRepo
                        .findByMetadataSessionOwnerAndMetadataSessionAndStateIn(SOURCE_1, SESSION_2, allStates,
                                                                                PageRequest.of(0, properties
                                                                                        .getMaxBulkSize()))
                        .getTotalPages());

        Assert.assertNotEquals(
                String.format("All requests from source %s and session %s should be present", SOURCE_1, SESSION_1), 0L,
                this.referenceRequestRepo
                        .findByMetadataSessionOwnerAndMetadataSessionAndStateIn(SOURCE_2, SESSION_1, allStates,
                                                                                PageRequest.of(0, properties
                                                                                        .getMaxBulkSize()))
                        .getTotalPages());
    }

    @Test
    @Purpose("Assert a source is correctly deleted. Only requests other than in error or denied states should remain.")
    public void deleteSourceTest() throws InterruptedException {
        sourceDeleteService.deleteSource(SOURCE_1);
        waitForJobSuccesses(FeatureExtractionDeletionJob.class.getName(), 1, 30000);

        // assert source is correctly deleted
        Set<RequestState> statesDeleted = new HashSet<>(Arrays.asList(RequestState.DENIED, RequestState.ERROR));
        Assert.assertEquals(
                String.format("All requests in %s states from source %s should have been deleted", statesDeleted,
                              SOURCE_1), 0L, this.referenceRequestRepo
                        .findByMetadataSessionOwnerAndStateIn(SOURCE_1, statesDeleted,
                                                              PageRequest.of(0, properties.getMaxBulkSize()))
                        .getTotalPages());

        // assert other source is still present
        Set<RequestState> allStates = new HashSet<>(
                Arrays.asList(RequestState.DENIED, RequestState.ERROR, RequestState.GRANTED, RequestState.SUCCESS));
        Assert.assertNotEquals(String.format("All requests from source %s should have be present", SOURCE_2), 0L,
                               this.referenceRequestRepo.findByMetadataSessionOwnerAndStateIn(SOURCE_2, allStates,
                                                                                              PageRequest.of(0,
                                                                                                             properties
                                                                                                                     .getMaxBulkSize()))
                                       .getTotalPages());
    }

    /**
     * Init requests to test the deletion of a source or a session
     *
     * @throws InterruptedException
     * @throws NotAvailablePluginConfigurationException
     * @throws ModuleException
     */
    private void initRequests() throws InterruptedException, NotAvailablePluginConfigurationException, ModuleException {
        // init plugin for feature request creation
        createPlugin();

        // --- CREATE REQUESTS ---
        int nbErrorRequests = 10;
        int nbDeniedRequests = 10;
        int nbGrantedRequests = 12;

        // DENIED REQ
        List<FeatureExtractionRequestEvent> eventsToPublish = createExtractionRequests(SOURCE_1, SESSION_1,
                                                                                       nbDeniedRequests);
        this.publisher.publish(eventsToPublish);
        // wait requests registration
        this.waitRequest(referenceRequestRepo, nbDeniedRequests, 60_000);
        // simulate requests denied
        List<FeatureRequestEvent> deniedToPublish = new ArrayList<>();
        eventsToPublish.forEach(event -> deniedToPublish.add(FeatureRequestEvent.build(FeatureRequestType.CREATION,
                                                                                       event.getRequestId(),
                                                                                       event.getRequestOwner(), null,
                                                                                       null, RequestState.DENIED)));
        publisher.publish(deniedToPublish);
        waitForState(this.referenceRequestRepo, RequestState.DENIED, nbDeniedRequests);

        // ERROR REQ
        eventsToPublish = createExtractionRequests(SOURCE_1, SESSION_2, nbErrorRequests);
        this.publisher.publish(eventsToPublish);
        Mockito.doThrow(new ModuleException("")).when(pluginService).getPlugin(Mockito.anyString());
        // wait requests registration
        this.waitRequest(referenceRequestRepo, nbDeniedRequests + nbErrorRequests, 60_000);
        // process requests and wait for error state
        featureReferenceService.scheduleRequests();
        waitForState(this.referenceRequestRepo, RequestState.ERROR, nbErrorRequests);

        // GRANTED REQ
        // source 1
        Mockito.reset(pluginService);
        eventsToPublish = createExtractionRequests(SOURCE_1, SESSION_1, nbGrantedRequests);
        this.publisher.publish(eventsToPublish);
        // wait requests registration
        this.waitRequest(referenceRequestRepo, nbDeniedRequests + nbErrorRequests + nbGrantedRequests, 60_000);

        // source 2
        Mockito.reset(pluginService);
        eventsToPublish = createExtractionRequests(SOURCE_2, SESSION_1, nbGrantedRequests);
        this.publisher.publish(eventsToPublish);
        // wait requests registration
        this.waitRequest(referenceRequestRepo, nbDeniedRequests + nbErrorRequests + 2 * nbGrantedRequests, 60_000);
    }

    private void createPlugin() {
        PluginConfiguration recipientPlugin = new PluginConfiguration();
        recipientPlugin.setBusinessId("testFeatureGeneration");
        recipientPlugin.setVersion("1.0.0");
        recipientPlugin.setLabel("test recipient");
        recipientPlugin.setPluginId("DefaultFeatureGenerator");
        this.pluginConfRepo.save(recipientPlugin);
    }

    private List<FeatureExtractionRequestEvent> createExtractionRequests(String source, String session, int size) {
        List<FeatureExtractionRequestEvent> eventsToPublish = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            JsonObject parameters = new JsonObject();
            parameters.add("location", new JsonPrimitive("test" + i));
            eventsToPublish.add(FeatureExtractionRequestEvent.build("test", FeatureCreationSessionMetadata
                                                                            .build(source, session, PriorityLevel.NORMAL, false, new StorageMetadata[0]), parameters,
                                                                    "testFeatureGeneration"));
        }
        return eventsToPublish;
    }

    @Override
    public void doAfter() {
        subscriber.unsubscribeFrom(SourceDeleteEvent.class);
        subscriber.unsubscribeFrom(SessionDeleteEvent.class);
        cleanAMQPQueues(SourceDeleteEventHandler.class, Target.ONE_PER_MICROSERVICE_TYPE);
        cleanAMQPQueues(SessionDeleteEventHandler.class, Target.ONE_PER_MICROSERVICE_TYPE);
    }
}