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
package fr.cnes.regards.modules.storage.service.file.flow;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.batch.IBatchHandler;
import fr.cnes.regards.modules.storage.domain.flow.ReferenceFlowItem;
import fr.cnes.regards.modules.storage.service.file.request.FileReferenceRequestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;

import java.util.List;

/**
 * Handler to handle {@link ReferenceFlowItem} AMQP messages.<br>
 * Those messages are sent to create new file reference.<br>
 * Each message is saved in a concurrent list to handle availability request by bulk.
 *
 * @author Sébastien Binda
 */
@Component
public class ReferenceFlowItemHandler
        implements ApplicationListener<ApplicationReadyEvent>, IBatchHandler<ReferenceFlowItem> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReferenceFlowItemHandler.class);

    /**
     * Bulk size limit to handle messages
     */
    @Value("${regards.storage.reference.items.bulk.size:10}")
    private int BULK_SIZE;

    @Autowired
    private ISubscriber subscriber;

    @Autowired
    private FileReferenceRequestService fileRefReqService;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        subscriber.subscribeTo(ReferenceFlowItem.class, this);
    }

    @Override
    public void handleBatch(List<ReferenceFlowItem> messages) {
        LOGGER.info("[REFERENCE FLOW HANDLER] Bulk saving {} AddFileRefFlowItem...", messages.size());
        long start = System.currentTimeMillis();
        fileRefReqService.reference(messages);
        LOGGER.info("[REFERENCE FLOW HANDLER] {} AddFileRefFlowItem handled in {} ms", messages.size(),
                    System.currentTimeMillis() - start);
    }

    @Override
    public Errors validate(ReferenceFlowItem message) {
        return null;
    }

    @Override
    public int getBatchSize() {
        return BULK_SIZE;
    }

}
