/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.storagelight.service.file.flow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.storagelight.domain.flow.RetryFlowItem;
import fr.cnes.regards.modules.storagelight.service.file.request.FileCacheRequestService;
import fr.cnes.regards.modules.storagelight.service.file.request.FileStorageRequestService;

/**
 * @author Sébastien Binda
 */
@Component
public class RetryFlowItemHandler implements ApplicationListener<ApplicationReadyEvent>, IHandler<RetryFlowItem> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RetryFlowItemHandler.class);

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private ISubscriber subscriber;

    @Autowired
    private FileStorageRequestService storageService;

    @Autowired
    private FileCacheRequestService cacheService;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        subscriber.subscribeTo(RetryFlowItem.class, this);
    }

    @Override
    public void handle(TenantWrapper<RetryFlowItem> wrapper) {
        runtimeTenantResolver.forceTenant(wrapper.getTenant());
        RetryFlowItem request = wrapper.getContent();
        try {
            switch (request.getType()) {
                case STORAGE:
                    if (request.getGroupId() != null) {
                        storageService.retryRequest(request.getGroupId());
                    } else {
                        storageService.retry(request.getOwners());
                    }
                    break;
                case AVAILABILITY:
                    if (request.getGroupId() != null) {
                        cacheService.retryRequest(request.getGroupId());
                    } else {
                        LOGGER.warn("Retry action is not available for availability requests with no request id.");
                    }
                    break;
                case DELETION:
                    LOGGER.warn("Retry action is not available for file reference requests.");
                    break;
                case REFERENCE:
                    LOGGER.warn("Retry action is not available for file reference requests.");
                    break;
                default:
                    LOGGER.warn("Retry action is not available for undefined requests.");
                    break;
            }
        } finally {
            runtimeTenantResolver.clearTenant();
        }

    }

}
