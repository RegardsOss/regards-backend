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
import fr.cnes.regards.framework.amqp.event.IRequestDeniedService;
import fr.cnes.regards.modules.feature.dto.event.in.FeatureNotificationRequestEvent;
import fr.cnes.regards.modules.feature.service.IFeatureNotificationService;
import fr.cnes.regards.modules.feature.service.conf.FeatureConfigurationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.validation.Validator;

import java.util.List;

/**
 * This handler handle {@link FeatureNotificationRequestEvent}
 *
 * @author Kevin Marchois
 */
@Component
@Profile("!noFemHandler")
public class NotificationRequestEventHandler extends AbstractFeatureRequestEventHandler<FeatureNotificationRequestEvent>
    implements IBatchHandler<FeatureNotificationRequestEvent>, ApplicationListener<ApplicationReadyEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationRequestEventHandler.class);

    private FeatureConfigurationProperties confProperties;

    private ISubscriber subscriber;

    private IFeatureNotificationService notificationService;

    public NotificationRequestEventHandler(FeatureConfigurationProperties confProperties,
                                           ISubscriber subscriber,
                                           IFeatureNotificationService featureService,
                                           Validator validator) {
        super(FeatureNotificationRequestEvent.class, validator);
        this.confProperties = confProperties;
        this.subscriber = subscriber;
        this.notificationService = featureService;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        subscriber.subscribeTo(FeatureNotificationRequestEvent.class, this);

    }

    @Override
    public void handleBatch(List<FeatureNotificationRequestEvent> messages) {
        long start = System.currentTimeMillis();
        LOGGER.info("{} notifications registred in {} ms",
                    notificationService.registerRequests(messages),
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

    @Override
    public IRequestDeniedService getFeatureService() {
        return notificationService;
    }

    @Override
    public boolean isDedicatedDLQEnabled() {
        return true;
    }
}
