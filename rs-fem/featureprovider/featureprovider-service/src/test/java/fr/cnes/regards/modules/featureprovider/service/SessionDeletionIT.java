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
import fr.cnes.regards.framework.modules.session.agent.domain.events.StepPropertyEventTypeEnum;
import fr.cnes.regards.framework.modules.session.agent.domain.update.StepPropertyUpdateRequest;
import fr.cnes.regards.framework.modules.session.commons.domain.events.SessionDeleteEvent;
import fr.cnes.regards.framework.modules.session.commons.domain.events.SourceDeleteEvent;
import fr.cnes.regards.framework.modules.session.commons.service.delete.SessionDeleteEventHandler;
import fr.cnes.regards.framework.modules.session.commons.service.delete.SourceDeleteEventHandler;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.utils.plugins.exception.NotAvailablePluginConfigurationException;
import fr.cnes.regards.modules.feature.dto.FeatureCreationSessionMetadata;
import fr.cnes.regards.modules.feature.dto.PriorityLevel;
import fr.cnes.regards.modules.feature.dto.StorageMetadata;
import fr.cnes.regards.modules.feature.dto.event.out.RequestState;
import fr.cnes.regards.modules.featureprovider.domain.FeatureExtractionRequestEvent;
import fr.cnes.regards.modules.featureprovider.service.session.ExtractionSessionDeleteService;
import fr.cnes.regards.modules.featureprovider.service.session.ExtractionSessionPropertyEnum;
import fr.cnes.regards.modules.featureprovider.service.session.ExtractionSourceDeleteService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * Test for {@link ExtractionSourceDeleteService} and {@link ExtractionSessionDeleteService}
 *
 * @author Iliana Ghazali
 **/
@TestPropertySource(
        properties = { "spring.jpa.properties.hibernate" + ".default_schema=featureprovider_session_deletion_it",
                "regards.amqp.enabled=true", "spring.task.scheduling.pool.size=2", "zuul.prefix=zuulPrefix",
                "regards.feature.provider.max.bulk.size=10" },
        locations = { "classpath:regards_perf.properties", "classpath:batch.properties" })
@ActiveProfiles(value = { "testAmqp", "noscheduler" })
public class SessionDeletionIT extends FeatureProviderMultitenantTest {

    private static final String SOURCE_1 = "SOURCE 1";

    private static final String SOURCE_2 = "SOURCE 2";

    private static final String SESSION_1 = "SESSION 1";

    private static final String SESSION_2 = "SESSION 2";

    private static final int NB_ERROR_REQUESTS = 5;

    private static final int NB_GRANTED_REQUESTS = 10;

    @Autowired
    private ExtractionSessionDeleteService extractionSessionDeleteService;

    @Autowired
    private ExtractionSourceDeleteService extractionSourceDeleteService;

    @Override
    public void doInit() throws InterruptedException, ModuleException, NotAvailablePluginConfigurationException {
        initRequests();
    }

    @Test
    @Purpose("Assert a session is correctly deleted. Only requests other than error requests should remain.")
    public void deleteSessionTest() throws InterruptedException {
        extractionSessionDeleteService.deleteSession(SOURCE_1, SESSION_1);
        waitForJobSuccesses(FeatureExtractionDeletionJob.class.getName(), 1, 30000);

        // assert session is correctly deleted
        Set<RequestState> statesDeleted = new HashSet<>(Arrays.asList(RequestState.ERROR));
        Assert.assertEquals(String.format("All requests in %s from source %s and session %s should have been deleted",
                                          statesDeleted, SOURCE_1, SESSION_1), 0L, this.referenceRequestRepo
                                    .findByMetadataSessionOwnerAndMetadataSessionAndStateIn(SOURCE_1, SESSION_1,
                                                                                            statesDeleted, PageRequest
                                                                                                    .of(0, properties
                                                                                                            .getMaxBulkSize()))
                                    .getTotalPages());
        // assert other sessions are still present
        Set<RequestState> allStates = new HashSet<>(
                Arrays.asList(RequestState.ERROR, RequestState.DENIED, RequestState.GRANTED, RequestState.SUCCESS));
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

        // assert decrement events on errors was correctly sent
        List<StepPropertyUpdateRequest> stepPropertyList = stepRepo.findAll();
        List<StepPropertyUpdateRequest> decEvents = stepPropertyList.stream()
                .filter(step -> step.getType().equals(StepPropertyEventTypeEnum.DEC)).collect(Collectors.toList());
        Assert.assertEquals("Unexpected number of events. Check the workflow", 1, decEvents.size());
        checkStepEvent(decEvents.get(0), ExtractionSessionPropertyEnum.REQUESTS_ERRORS.getName(),
                       String.valueOf(NB_ERROR_REQUESTS), StepPropertyEventTypeEnum.DEC, SOURCE_1, SESSION_1);
    }

    @Test
    @Purpose("Assert a source is correctly deleted. Only requests other than error requests should remain.")
    public void deleteSourceTest() throws InterruptedException {
        extractionSourceDeleteService.deleteSource(SOURCE_1);
        waitForJobSuccesses(FeatureExtractionDeletionJob.class.getName(), 1, 30000);

        // assert source is correctly deleted
        Set<RequestState> statesDeleted = new HashSet<>(Arrays.asList(RequestState.ERROR));
        Assert.assertEquals(
                String.format("All requests in %s states from source %s should have been deleted", statesDeleted,
                              SOURCE_1), 0L, this.referenceRequestRepo
                        .findByMetadataSessionOwnerAndStateIn(SOURCE_1, statesDeleted,
                                                              PageRequest.of(0, properties.getMaxBulkSize()))
                        .getTotalPages());

        // assert other source is still present
        Set<RequestState> allStates = new HashSet<>(
                Arrays.asList(RequestState.ERROR, RequestState.DENIED, RequestState.GRANTED, RequestState.SUCCESS));
        Assert.assertNotEquals(String.format("All requests from source %s should have be present", SOURCE_2), 0L,
                               this.referenceRequestRepo.findByMetadataSessionOwnerAndStateIn(SOURCE_2, allStates,
                                                                                              PageRequest.of(0,
                                                                                                             properties
                                                                                                                     .getMaxBulkSize()))
                                       .getTotalPages());

        // assert decrement events on errors were correctly sent
        List<StepPropertyUpdateRequest> stepPropertyList = stepRepo.findAll();
        List<StepPropertyUpdateRequest> decEvents = stepPropertyList.stream()
                .filter(step -> step.getType().equals(StepPropertyEventTypeEnum.DEC)).collect(Collectors.toList());
        Assert.assertEquals("Unexpected number of events. Check the workflow", 2, decEvents.size());
        for (StepPropertyUpdateRequest decEvent : decEvents) {
            if (decEvent.getSession().equals(SESSION_1)) {
                checkStepEvent(decEvent, ExtractionSessionPropertyEnum.REQUESTS_ERRORS.getName(),
                               String.valueOf(NB_ERROR_REQUESTS), StepPropertyEventTypeEnum.DEC, SOURCE_1, SESSION_1);
            } else if (decEvent.getSession().equals(SESSION_2)) {
                checkStepEvent(decEvent, ExtractionSessionPropertyEnum.REQUESTS_ERRORS.getName(),
                               String.valueOf(NB_ERROR_REQUESTS), StepPropertyEventTypeEnum.DEC, SOURCE_1, SESSION_2);
            } else {
                Assert.fail("Unexpected event");
            }
        }
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

        // ERROR REQ
        // source 1 / session 1
        List<FeatureExtractionRequestEvent> eventsToPublish = createExtractionRequests(SOURCE_1, SESSION_1, NB_ERROR_REQUESTS);
        this.publisher.publish(eventsToPublish);
        Mockito.doThrow(new ModuleException("")).when(pluginService).getPlugin(Mockito.anyString());
        // wait requests registration
        this.waitRequest(referenceRequestRepo, NB_ERROR_REQUESTS, 60_000);
        // process requests and wait for error state
        featureReferenceService.scheduleRequests();
        waitForState(this.referenceRequestRepo, RequestState.ERROR, NB_ERROR_REQUESTS);

        // source 1 / session 2
        eventsToPublish = createExtractionRequests(SOURCE_1, SESSION_2, NB_ERROR_REQUESTS);
        this.publisher.publish(eventsToPublish);
        // wait requests registration
        this.waitRequest(referenceRequestRepo, NB_ERROR_REQUESTS * 2, 60_000);
        // process requests and wait for error state
        featureReferenceService.scheduleRequests();
        waitForState(this.referenceRequestRepo, RequestState.ERROR, NB_ERROR_REQUESTS * 2);

        // GRANTED REQ
        // source 1 / session 1
        Mockito.reset(pluginService);
        eventsToPublish = createExtractionRequests(SOURCE_1, SESSION_1, NB_GRANTED_REQUESTS);
        this.publisher.publish(eventsToPublish);
        // wait requests registration
        this.waitRequest(referenceRequestRepo, NB_ERROR_REQUESTS * 2 + NB_GRANTED_REQUESTS, 60_000);

        // source 2 / session 1
        Mockito.reset(pluginService);
        eventsToPublish = createExtractionRequests(SOURCE_2, SESSION_1, NB_GRANTED_REQUESTS);
        this.publisher.publish(eventsToPublish);
        // wait requests registration
        this.waitRequest(referenceRequestRepo, NB_ERROR_REQUESTS * 2 + 2 * NB_GRANTED_REQUESTS, 60_000);
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