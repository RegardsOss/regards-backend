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
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.session.agent.domain.events.StepPropertyEventTypeEnum;
import fr.cnes.regards.framework.modules.session.agent.domain.update.StepPropertyUpdateRequest;
import fr.cnes.regards.framework.utils.plugins.exception.NotAvailablePluginConfigurationException;
import fr.cnes.regards.modules.feature.dto.FeatureCreationSessionMetadata;
import fr.cnes.regards.modules.feature.dto.FeatureRequestStep;
import fr.cnes.regards.modules.feature.dto.PriorityLevel;
import fr.cnes.regards.modules.feature.dto.StorageMetadata;
import fr.cnes.regards.modules.feature.dto.event.out.FeatureRequestEvent;
import fr.cnes.regards.modules.feature.dto.event.out.FeatureRequestType;
import fr.cnes.regards.modules.feature.dto.event.out.RequestState;
import fr.cnes.regards.modules.featureprovider.domain.FeatureExtractionRequestEvent;
import fr.cnes.regards.modules.featureprovider.service.session.ExtractionSessionNotifier;
import fr.cnes.regards.modules.featureprovider.service.session.ExtractionSessionPropertyEnum;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * Test to test if
 * {@link fr.cnes.regards.framework.modules.session.agent.domain.events.StepPropertyUpdateRequestEvent} are correctly
 * sent following the calling of methods in {@link ExtractionSessionNotifier}
 *
 * @author Iliana Ghazali
 **/
@TestPropertySource(
        properties = { "spring.jpa.properties.hibernate" + ".default_schema=featureprovider_session_it",
                "regards.amqp.enabled=true", "spring.task.scheduling.pool.size=2", "zuul.prefix=zuulPrefix",
                "regards.feature.provider.max.bulk.size=10" },
        locations = { "classpath:regards_perf.properties", "classpath:batch.properties" })
@ActiveProfiles(value = { "testAmqp", "noscheduler" })
public class SessionIT extends FeatureProviderMultitenantTest {

    private static final String SOURCE_1 = "SOURCE 1";

    private static final String SESSION_1 = "SESSION 1";

    private static final String SESSION_2 = "SESSION 2";


    @Test
    public void eventsSuccessTest() throws InterruptedException {
        int nbRequests = 3;
        // init requests and simulate that every request has been successfully granted by feature module
        List<FeatureExtractionRequestEvent> eventsToPublish = initGrantedRequests(nbRequests);
        simulateGrantedRequests(eventsToPublish);

        // get list of properties
        List<StepPropertyUpdateRequest> stepPropertyList = this.stepRepo.findBySession(SESSION_1);
        stepPropertyList.sort(Comparator.comparing(StepPropertyUpdateRequest::getCreationDate));

        // check events were correctly sent
        Assert.assertEquals("Wrong number of events created. Check the workflow of events sent", 6,
                            stepPropertyList.size());
        checkStepEvent(stepPropertyList.get(0), ExtractionSessionPropertyEnum.TOTAL_REQUESTS.getName(), "1",
                       StepPropertyEventTypeEnum.INC, SOURCE_1, SESSION_1);
        checkStepEvent(stepPropertyList.get(1), ExtractionSessionPropertyEnum.TOTAL_REQUESTS.getName(), "1",
                       StepPropertyEventTypeEnum.INC, SOURCE_1, SESSION_1);
        checkStepEvent(stepPropertyList.get(2), ExtractionSessionPropertyEnum.TOTAL_REQUESTS.getName(), "1",
                       StepPropertyEventTypeEnum.INC, SOURCE_1, SESSION_1);
        checkStepEvent(stepPropertyList.get(3), ExtractionSessionPropertyEnum.GENERATED_PRODUCTS.getName(), "1",
                       StepPropertyEventTypeEnum.INC, SOURCE_1, SESSION_1);
        checkStepEvent(stepPropertyList.get(4), ExtractionSessionPropertyEnum.GENERATED_PRODUCTS.getName(), "1",
                       StepPropertyEventTypeEnum.INC, SOURCE_1, SESSION_1);
        checkStepEvent(stepPropertyList.get(5), ExtractionSessionPropertyEnum.GENERATED_PRODUCTS.getName(), "1",
                       StepPropertyEventTypeEnum.INC, SOURCE_1, SESSION_1);
    }

    @Test
    public void eventsErrorTest() throws InterruptedException {
        int nbRequests = 2;
        // init requests and simulate that every request has been denied by feature module
        List<FeatureExtractionRequestEvent> eventsToPublish = initGrantedRequests(nbRequests);
        simulateErrorRequests(eventsToPublish);

        // get list of properties
        List<StepPropertyUpdateRequest> stepPropertyList = this.stepRepo.findBySession(SESSION_1);
        stepPropertyList.sort(Comparator.comparing(StepPropertyUpdateRequest::getCreationDate));

        // check events were correctly sent
        Assert.assertEquals("Wrong number of events created. Check the workflow of events sent", 4,
                            stepPropertyList.size());
        checkStepEvent(stepPropertyList.get(0), ExtractionSessionPropertyEnum.TOTAL_REQUESTS.getName(), "1",
                       StepPropertyEventTypeEnum.INC, SOURCE_1, SESSION_1);
        checkStepEvent(stepPropertyList.get(1), ExtractionSessionPropertyEnum.TOTAL_REQUESTS.getName(), "1",
                       StepPropertyEventTypeEnum.INC, SOURCE_1, SESSION_1);
        checkStepEvent(stepPropertyList.get(2), ExtractionSessionPropertyEnum.REQUESTS_ERRORS.getName(), "1",
                       StepPropertyEventTypeEnum.INC, SOURCE_1, SESSION_1);
        checkStepEvent(stepPropertyList.get(3), ExtractionSessionPropertyEnum.REQUESTS_ERRORS.getName(), "1",
                       StepPropertyEventTypeEnum.INC, SOURCE_1, SESSION_1);
    }


    // ---- UTILS METHODS ----

    /**
     * Init requests to test the deletion of a source or a session
     *
     * @throws InterruptedException
     * @throws NotAvailablePluginConfigurationException
     * @throws ModuleException
     */
    private List<FeatureExtractionRequestEvent> initGrantedRequests(int nbGrantedRequests) throws InterruptedException {
        // init plugin for feature request creation
        createPlugin();

        // GRANTED REQ
        List<FeatureExtractionRequestEvent> eventsToPublish = createExtractionRequests(SOURCE_1, SESSION_1,
                                                                                       nbGrantedRequests);
        this.publisher.publish(eventsToPublish);
        // wait requests registration
        this.waitRequest(referenceRequestRepo,  nbGrantedRequests, 60_000);
        // process requests and wait for remote state
        featureReferenceService.scheduleRequests();
        waitForStep(this.referenceRequestRepo, FeatureRequestStep.REMOTE_CREATION_REQUESTED, 120_000);
        return eventsToPublish;
    }

    private void simulateGrantedRequests(List<FeatureExtractionRequestEvent> eventsToPublish)
            throws InterruptedException {
        List<FeatureRequestEvent> creationGrantedToPublish = new ArrayList<>();
        eventsToPublish.forEach(event -> creationGrantedToPublish.add(FeatureRequestEvent
                                                                              .build(FeatureRequestType.CREATION,
                                                                                     event.getRequestId(),
                                                                                     event.getRequestOwner(), null,
                                                                                     null, RequestState.GRANTED)));
        publisher.publish(creationGrantedToPublish);
        Thread.sleep(5000L);

    }

    private void simulateErrorRequests(List<FeatureExtractionRequestEvent> eventsToPublish)
            throws InterruptedException {
        List<FeatureRequestEvent> creationDeniedToPublish = new ArrayList<>();
        eventsToPublish.forEach(event -> creationDeniedToPublish.add(FeatureRequestEvent
                                                                              .build(FeatureRequestType.CREATION,
                                                                                     event.getRequestId(),
                                                                                     event.getRequestOwner(), null,
                                                                                     null, RequestState.DENIED)));
        publisher.publish(creationDeniedToPublish);
        Thread.sleep(5000L);

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
                                                                            .build(source, session, PriorityLevel.NORMAL,
                                                                                   false, false, new StorageMetadata[0]), parameters,
                                                                    "testFeatureGeneration"));
        }
        return eventsToPublish;
    }
}