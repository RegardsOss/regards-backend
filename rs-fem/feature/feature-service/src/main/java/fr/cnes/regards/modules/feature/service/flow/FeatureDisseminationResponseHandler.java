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
package fr.cnes.regards.modules.feature.service.flow;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.batch.IBatchHandler;
import fr.cnes.regards.modules.feature.dto.event.in.DisseminationAckEvent;
import fr.cnes.regards.modules.feature.service.IFeatureUpdateService;
import fr.cnes.regards.modules.feature.service.request.FeatureUpdateDisseminationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;

import java.util.List;

/**
 * Handler to handle Feature dissemination acknowledge
 *
 * @author Léo Mieulet
 */
@Component
public class FeatureDisseminationResponseHandler
        implements ApplicationListener<ApplicationReadyEvent>, IBatchHandler<DisseminationAckEvent> {

    @Autowired
    private ISubscriber subscriber;

    @Autowired
    private IFeatureUpdateService featureUpdateService;

    @Autowired
    private FeatureUpdateDisseminationService featureUpdateDisseminationService;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        subscriber.subscribeTo(DisseminationAckEvent.class, this);
    }

    @Override
    public Errors validate(DisseminationAckEvent message) {
        return null;
    }

    @Override
    public void handleBatch(List<DisseminationAckEvent> messages) {
        LOGGER.debug("[DISSEMINATION ACK HANDLER] Bulk handling {} DisseminationAckEvent...", messages.size());
        long start = System.currentTimeMillis();
        featureUpdateDisseminationService.saveAckRequests(messages);
        LOGGER.debug("[DISSEMINATION ACK HANDLER] {} DisseminationAckEvent events handled in {} ms", messages.size(),
                     System.currentTimeMillis() - start);
    }
}
