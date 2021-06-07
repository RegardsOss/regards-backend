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
import fr.cnes.regards.framework.utils.plugins.exception.NotAvailablePluginConfigurationException;
import fr.cnes.regards.modules.feature.client.FeatureClient;
import fr.cnes.regards.modules.feature.domain.request.FeatureCreationMetadataEntity;
import fr.cnes.regards.modules.feature.domain.request.FeatureRequestStep;
import fr.cnes.regards.modules.feature.dto.FeatureCreationSessionMetadata;
import fr.cnes.regards.modules.feature.dto.FeatureRequestDTO;
import fr.cnes.regards.modules.feature.dto.FeatureRequestsSelectionDTO;
import fr.cnes.regards.modules.feature.dto.PriorityLevel;
import fr.cnes.regards.modules.feature.dto.StorageMetadata;
import fr.cnes.regards.modules.feature.dto.event.out.FeatureRequestEvent;
import fr.cnes.regards.modules.feature.dto.event.out.FeatureRequestType;
import fr.cnes.regards.modules.feature.dto.event.out.RequestState;
import fr.cnes.regards.modules.feature.dto.hateoas.RequestHandledResponse;
import fr.cnes.regards.modules.feature.dto.hateoas.RequestsPage;
import fr.cnes.regards.modules.featureprovider.domain.FeatureExtractionRequest;
import fr.cnes.regards.modules.featureprovider.domain.FeatureExtractionRequestEvent;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.assertj.core.util.Lists;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * @author kevin
 *
 */
@TestPropertySource(
        properties = { "spring.jpa.properties.hibernate.default_schema=feature_reference", "regards.amqp.enabled=true",
                "spring.task.scheduling.pool.size=2", "zuul.prefix=zuulPrefix" },
        locations = { "classpath:regards_perf.properties", "classpath:batch.properties" })
@ActiveProfiles(value = { "testAmqp", "noscheduler" })
public class FeatureExtractionServiceIT extends FeatureProviderMultitenantTest {

    @Autowired
    private IFeatureExtractionService featureExtractionService;

    @Spy
    private FeatureClient featureClient;

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
            FeatureExtractionRequestEvent referenceEvent = FeatureExtractionRequestEvent
                    .build("bibi",
                           FeatureCreationSessionMetadata.build("bibi", "session", PriorityLevel.NORMAL, false,
                                                                new StorageMetadata[0]),
                           parameters, "testFeatureGeneration");
            eventsToPublish.add(referenceEvent);
            creationGrantedToPublish
                    .add(FeatureRequestEvent.build(FeatureRequestType.CREATION, referenceEvent.getRequestId(),
                                                   referenceEvent.getRequestOwner(), null, null, RequestState.GRANTED));
        }
        this.publisher.publish(eventsToPublish);
        // lets wait until all requests are registered
        this.waitRequest(extractionRequestRepo, properties.getMaxBulkSize(), 60_000);
        // once this is done, lets schedule all requests
        featureExtractionService.scheduleRequests();
        // wait for all jobs to be finished it means all requests are in step REMOTE_CREATION_REQUESTED
        waitForStep(extractionRequestRepo, FeatureRequestStep.REMOTE_CREATION_REQUESTED, 120_000);
        // now simulate that every request has been successfully granted by feature module
        publisher.publish(creationGrantedToPublish);
        // then lets wait for the DB to be empty
        this.waitRequest(extractionRequestRepo, 0, 10_000);
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
            eventsToPublish.add(FeatureExtractionRequestEvent
                    .build("bibi",
                           FeatureCreationSessionMetadata.build("bibi", "session", PriorityLevel.NORMAL, false,
                                                                new StorageMetadata[0]),
                           parameters, "testFeatureGeneration"));
        }
        this.publisher.publish(eventsToPublish);

        Mockito.doThrow(new ModuleException("")).when(pluginService).getPlugin(Mockito.anyString());
        // lets wait until all requests are registered
        this.waitRequest(extractionRequestRepo, properties.getMaxBulkSize(), 60_000);
        // once this is done, lets schedule all requests
        featureExtractionService.scheduleRequests();
        // now lets wait for request to be in error
        waitForState(this.extractionRequestRepo, RequestState.ERROR);
        Mockito.verify(featureClient, Mockito.times(0)).createFeatures(Mockito.anyList());
    }

    @Test
    public void findRequests() {
        createRequests("source1", "session1", 10, RequestState.GRANTED);
        createRequests("source1", "session2", 20, RequestState.GRANTED);
        createRequests("source2", "session1", 30, RequestState.GRANTED);
        createRequests("source2", "session2", 40, RequestState.GRANTED);
        createRequests("source1", "session1", 50, RequestState.ERROR);

        FeatureRequestsSelectionDTO selection = FeatureRequestsSelectionDTO.build();
        RequestsPage<FeatureRequestDTO> results = featureExtractionService.findRequests(selection,
                                                                                        PageRequest.of(0, 10));
        Assert.assertEquals(150, results.getTotalElements());
        Assert.assertEquals(new Long(50), results.getInfo().getNbErrors());
        Assert.assertEquals(10, results.getNumberOfElements());

        selection = FeatureRequestsSelectionDTO.build().withState(RequestState.GRANTED);
        results = featureExtractionService.findRequests(selection, PageRequest.of(0, 10));
        Assert.assertEquals(100, results.getTotalElements());
        Assert.assertEquals(new Long(0), results.getInfo().getNbErrors());
        Assert.assertEquals(10, results.getNumberOfElements());

        selection = FeatureRequestsSelectionDTO.build().withState(RequestState.ERROR);
        results = featureExtractionService.findRequests(selection, PageRequest.of(0, 10));
        Assert.assertEquals(50, results.getTotalElements());
        Assert.assertEquals(new Long(50), results.getInfo().getNbErrors());
        Assert.assertEquals(10, results.getNumberOfElements());

        selection = FeatureRequestsSelectionDTO.build().withEnd(OffsetDateTime.now().plusDays(1))
                .withStart(OffsetDateTime.now().minusDays(1));
        results = featureExtractionService.findRequests(selection, PageRequest.of(0, 10));
        Assert.assertEquals(150, results.getTotalElements());
        Assert.assertEquals(new Long(50), results.getInfo().getNbErrors());
        Assert.assertEquals(10, results.getNumberOfElements());

        selection = FeatureRequestsSelectionDTO.build().withSource("source1").withSession("session1");
        results = featureExtractionService.findRequests(selection, PageRequest.of(0, 10));
        Assert.assertEquals(60, results.getTotalElements());
        Assert.assertEquals(new Long(50), results.getInfo().getNbErrors());
        Assert.assertEquals(10, results.getNumberOfElements());
    }

    @Test
    public void testDeleteRequests() {
        createRequests("source1", "session1", 10, RequestState.GRANTED);
        createRequests("source1", "session1", 50, RequestState.ERROR);

        RequestsPage<FeatureRequestDTO> searchResp = featureExtractionService
                .findRequests(FeatureRequestsSelectionDTO.build(), PageRequest.of(0, 1000));
        Assert.assertEquals(60, searchResp.getTotalElements());

        RequestHandledResponse response = featureExtractionService.deleteRequests(FeatureRequestsSelectionDTO.build());
        Assert.assertEquals("There should 50 requests in error state deleted", 50, response.getTotalHandled());
        Assert.assertEquals("There should 50 requests in error state deleted", 50, response.getTotalRequested());

        searchResp = featureExtractionService.findRequests(FeatureRequestsSelectionDTO.build(),
                                                           PageRequest.of(0, 1000));
        Assert.assertEquals("The 50 request in error state should be deleted", 10, searchResp.getTotalElements());
    }

    @Test
    public void testRetryRequests() {

        createRequests("source1", "session1", 10, RequestState.GRANTED);
        createRequests("source1", "session1", 50, RequestState.ERROR);

        RequestsPage<FeatureRequestDTO> searchResp = featureExtractionService
                .findRequests(FeatureRequestsSelectionDTO.build().withState(RequestState.GRANTED),
                              PageRequest.of(0, 1000));
        Assert.assertEquals(10, searchResp.getTotalElements());

        RequestHandledResponse response = featureExtractionService.retryRequests(FeatureRequestsSelectionDTO.build());
        Assert.assertEquals("There should 50 requests in error state deleted", 50, response.getTotalHandled());
        Assert.assertEquals("There should 50 requests in error state deleted", 50, response.getTotalRequested());

        searchResp = featureExtractionService
                .findRequests(FeatureRequestsSelectionDTO.build().withState(RequestState.GRANTED),
                              PageRequest.of(0, 1000));
        Assert.assertEquals("All error request  should be granted now", 60, searchResp.getTotalElements());

    }

    private void createRequests(String source, String session, int nbRequests, RequestState state) {
        List<FeatureExtractionRequest> requests = Lists.newArrayList();
        for (int i = 0; i < nbRequests; i++) {
            FeatureCreationMetadataEntity metadata = FeatureCreationMetadataEntity.build(source, session,
                                                                                         Lists.newArrayList(), true);
            requests.add(FeatureExtractionRequest.build(UUID.randomUUID().toString(), "owner", OffsetDateTime.now(),
                                                        state, metadata, FeatureRequestStep.LOCAL_DELAYED,
                                                        PriorityLevel.NORMAL, new JsonObject(), "factory"));
        }
        requests = extractionRequestRepo.saveAll(requests);
        Assert.assertEquals(nbRequests, requests.size());
    }

}