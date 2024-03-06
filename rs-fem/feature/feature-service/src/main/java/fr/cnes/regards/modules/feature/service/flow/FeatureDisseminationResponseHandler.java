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
import fr.cnes.regards.modules.feature.service.request.FeatureUpdateDisseminationService;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import java.util.List;

/**
 * Handler amqp request message for {@link DisseminationAckEvent} events (feature dissemination acknowledge)
 *
 * @author Léo Mieulet
 */
@Component
public class FeatureDisseminationResponseHandler
    implements ApplicationListener<ApplicationReadyEvent>, IBatchHandler<DisseminationAckEvent> {

    private ISubscriber subscriber;

    private FeatureUpdateDisseminationService featureUpdateDisseminationService;

    private Validator validator;

    public FeatureDisseminationResponseHandler(ISubscriber subscriber,
                                               FeatureUpdateDisseminationService featureUpdateDisseminationService,
                                               Validator validator) {
        this.subscriber = subscriber;
        this.featureUpdateDisseminationService = featureUpdateDisseminationService;
        this.validator = validator;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        subscriber.subscribeTo(DisseminationAckEvent.class, this);
    }

    @Override
    public Errors validate(DisseminationAckEvent message) {
        Errors errors = new BeanPropertyBindingResult(message, message.getClass().getName());
        validator.validate(message, errors);
        return errors;
    }

    @Override
    public void handleBatch(List<DisseminationAckEvent> disseminationAckEvts) {
        LOGGER.debug("[FEATURE DISSEMINATION ACK HANDLER] Bulk handling {} DisseminationAckEvent...",
                     disseminationAckEvts.size());
        long start = System.currentTimeMillis();

        featureUpdateDisseminationService.saveAckRequests(disseminationAckEvts);

        LOGGER.debug("[FEATURE DISSEMINATION ACK HANDLER] {} DisseminationAckEvent events handled in {} ms",
                     disseminationAckEvts.size(),
                     System.currentTimeMillis() - start);
    }

    @Override
    public boolean isRetryEnabled() {
        return true;
    }

}
