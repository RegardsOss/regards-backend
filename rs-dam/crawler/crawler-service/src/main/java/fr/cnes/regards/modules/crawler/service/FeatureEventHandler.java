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
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.dam.dto.FeatureEvent;
import fr.cnes.regards.modules.dam.dto.FeatureEventType;
import fr.cnes.regards.modules.model.gson.ModelJsonReadyEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Handle {@link FeatureEvent}s to delete features from index.
 *
 * @author Sébastien Binda
 * @author Marc Sordi
 */
@Component
public class FeatureEventHandler implements IBatchHandler<FeatureEvent> {

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private IEntityIndexerService entityIndexerService;

    @Autowired
    private ISubscriber subscriber;

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
    public void handleBatch(List<FeatureEvent> messages) {
        Set<String> ipIds = messages.stream()
                                    .filter(f -> FeatureEventType.DELETE.equals(f.getType()))
                                    .map(f -> f.getFeatureId())
                                    .collect(Collectors.toUnmodifiableSet());
        entityIndexerService.deleteDataObjectsAndUpdate(runtimeTenantResolver.getTenant(), ipIds);
    }

    @Override
    public boolean isDedicatedDLQEnabled() {
        return false;
    }
}
