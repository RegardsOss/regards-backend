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

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.batch.IBatchHandler;
import fr.cnes.regards.modules.feature.dto.RequestInfo;
import fr.cnes.regards.modules.featureprovider.domain.FeatureExtractionRequestEvent;
import fr.cnes.regards.modules.featureprovider.service.conf.FeatureProviderConfigurationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;

import java.util.List;

/**
 * This handler absorbs the incoming reference creation request flow
 *
 * @author Kevin Marchois
 */
@Component
@Profile("!nohandler")
public class FeatureExtractionRequestEventHandler
        implements IBatchHandler<FeatureExtractionRequestEvent>, ApplicationListener<ApplicationReadyEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureExtractionRequestEventHandler.class);

    @Autowired
    private FeatureProviderConfigurationProperties confProperties;

    @Autowired
    private ISubscriber subscriber;

    @Autowired
    private IFeatureExtractionService featureExtractionService;

    @Override
    public Class<FeatureExtractionRequestEvent> getMType() {
        return FeatureExtractionRequestEvent.class;
    }

    @Override
    public boolean handleConversionError(Message message, String errorMessage) {
        return featureExtractionService.denyMessage(message, errorMessage);
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        subscriber.subscribeTo(FeatureExtractionRequestEvent.class, this);
    }

    @Override
    public Errors validate(FeatureExtractionRequestEvent message) {
        // FIXME
        return null;
    }

    @Override
    public void handleBatch(List<FeatureExtractionRequestEvent> messages) {
        long start = System.currentTimeMillis();
        RequestInfo<String> requestInfo = featureExtractionService.registerRequests(messages);
        LOGGER.info("{} granted request(s) and {} denied request(s) registered in {} ms",
                    requestInfo.getGranted().size(), requestInfo.getDenied().keySet().size(),
                    System.currentTimeMillis() - start);
    }

    @Override
    public int getBatchSize() {
        return confProperties.getBatchSize();
    }

    @Override
    public long getReceiveTimeout() {
        return confProperties.getBatchReceiveTimeout();
    }
}
