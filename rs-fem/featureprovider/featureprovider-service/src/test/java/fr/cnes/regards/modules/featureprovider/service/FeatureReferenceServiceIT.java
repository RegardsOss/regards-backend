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
import fr.cnes.regards.modules.feature.dto.FeatureCreationSessionMetadata;
import fr.cnes.regards.modules.feature.dto.FeatureRequestStep;
import fr.cnes.regards.modules.feature.dto.PriorityLevel;
import fr.cnes.regards.modules.feature.dto.StorageMetadata;
import fr.cnes.regards.modules.feature.dto.event.out.FeatureRequestEvent;
import fr.cnes.regards.modules.feature.dto.event.out.FeatureRequestType;
import fr.cnes.regards.modules.feature.dto.event.out.RequestState;
import fr.cnes.regards.modules.featureprovider.domain.FeatureExtractionRequestEvent;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.mockito.Mockito;
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
public class FeatureReferenceServiceIT extends FeatureProviderMultitenantTest {

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
            eventsToPublish.add(FeatureExtractionRequestEvent
                    .build("bibi",
                           FeatureCreationSessionMetadata.build("bibi", "session", PriorityLevel.NORMAL, false,
                                                                new StorageMetadata[0]),
                           parameters, "testFeatureGeneration"));
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
}