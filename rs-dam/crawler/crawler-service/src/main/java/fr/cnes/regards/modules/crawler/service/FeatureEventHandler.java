/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.crawler.service;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.batch.IBatchHandler;
import fr.cnes.regards.modules.dam.dto.FeatureEvent;
import fr.cnes.regards.modules.model.gson.ModelJsonReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;

import java.util.List;

/**
 * Handle {@link FeatureEvent}s to delete features from index.
 *
 * @author Sébastien Binda
 * @author Marc Sordi
 */
@Component
public class FeatureEventHandler implements IBatchHandler<FeatureEvent> {

    private final ISubscriber subscriber;

    private final EntityDeletionService entityDeletionService;

    public FeatureEventHandler(ISubscriber subscriber, EntityDeletionService entityDeletionService) {
        this.subscriber = subscriber;
        this.entityDeletionService = entityDeletionService;
    }

    @EventListener
    public void handleApplicationReady(ModelJsonReadyEvent event) {
        subscriber.subscribeTo(FeatureEvent.class, this);
    }

    @Override
    public Errors validate(FeatureEvent message) {
        // Nothing to do
        return null;
    }

    @Override
    public void handleBatch(List<FeatureEvent> featureEvents) {
        LOGGER.debug("Received {} featureEvents", featureEvents.size());
        entityDeletionService.createRequests(featureEvents.stream().map(FeatureEvent::getFeatureId).toList());
        LOGGER.debug("{} featureEvents handled", featureEvents.size());
    }
}
