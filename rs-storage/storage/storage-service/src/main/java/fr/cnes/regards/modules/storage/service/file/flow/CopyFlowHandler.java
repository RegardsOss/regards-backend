/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.storage.service.file.flow;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.batch.IBatchHandler;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.storage.domain.flow.CopyFlowItem;
import fr.cnes.regards.modules.storage.service.file.request.FileCopyRequestService;

/**
 * Handler to handle {@link CopyFlowItem} AMQP messages.<br>
 * Those messages are sent to copy a file reference to a given storage location<br>
 * Each message is saved in a concurrent list to handle availability request by bulk.
 *
 * @author Sébastien Binda
 */
@Component
public class CopyFlowHandler implements ApplicationListener<ApplicationReadyEvent>, IBatchHandler<CopyFlowItem> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CopyFlowHandler.class);

    @Value("${regards.storage.copy.items.bulk.size:10}")
    private int BULK_SIZE;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private ISubscriber subscriber;

    @Autowired
    private FileCopyRequestService fileCopyReqService;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        subscriber.subscribeTo(CopyFlowItem.class, this);
    }

    @Override
    public void handleBatch(String tenant, List<CopyFlowItem> messages) {
        try {
            runtimeTenantResolver.forceTenant(tenant);
            LOGGER.debug("[COPY FLOW HANDLER] Bulk saving {} CopyFlowItem...", messages.size());
            long start = System.currentTimeMillis();
            fileCopyReqService.copy(messages);
            LOGGER.debug("[COPY FLOW HANDLER] {} CopyFlowItem handled in {} ms", messages.size(),
                         System.currentTimeMillis() - start);
        } finally {
            runtimeTenantResolver.clearTenant();
        }
    }

    @Override
    public boolean validate(String tenant, CopyFlowItem message) {
        return true;
    }

    @Override
    public int getBatchSize() {
        return BULK_SIZE;
    }

}
