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

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.batch.IBatchHandler;
import fr.cnes.regards.framework.amqp.event.IRequestDeniedService;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.modules.feature.dto.RequestInfo;
import fr.cnes.regards.modules.feature.dto.event.in.FeatureCreationRequestEvent;
import fr.cnes.regards.modules.feature.dto.event.out.FeatureRequestEvent;
import fr.cnes.regards.modules.feature.service.IFeatureCreationService;
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
 * This handler absorbs the incoming creation request flow
 *
 * @author Marc SORDI
 */
@Component
@Profile("!noFemHandler")
public class FeatureCreationRequestEventHandler extends AbstractFeatureRequestEventHandler<FeatureCreationRequestEvent>
    implements IBatchHandler<FeatureCreationRequestEvent>, ApplicationListener<ApplicationReadyEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureCreationRequestEventHandler.class);

    private FeatureConfigurationProperties confProperties;

    private ISubscriber subscriber;

    private IPublisher publisher;

    private IFeatureCreationService featureService;

    private ITenantResolver tenantResolver;

    public FeatureCreationRequestEventHandler(FeatureConfigurationProperties confProperties,
                                              ISubscriber subscriber,
                                              IPublisher publisher,
                                              IFeatureCreationService featureService,
                                              ITenantResolver tenantResolver,
                                              Validator validator) {
        super(FeatureCreationRequestEvent.class, validator);
        this.confProperties = confProperties;
        this.subscriber = subscriber;
        this.publisher = publisher;
        this.featureService = featureService;
        this.tenantResolver = tenantResolver;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        subscriber.subscribeTo(FeatureCreationRequestEvent.class, this);
        // Init feature response out exchange
        publisher.initExchange(tenantResolver.getAllActiveTenants(), FeatureRequestEvent.class);
    }

    @Override
    public void handleBatch(List<FeatureCreationRequestEvent> messages) {
        long start = System.currentTimeMillis();
        RequestInfo<String> requestInfo = featureService.registerRequests(messages);
        LOGGER.info("{} granted request(s) and {} denied request(s) registered in {} ms",
                    requestInfo.getGranted().size(),
                    requestInfo.getDenied().keySet().size(),
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
        return featureService;
    }

}
