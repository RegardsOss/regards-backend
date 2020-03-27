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
package fr.cnes.regards.modules.ingest.service.flow;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.modules.ingest.dto.sip.flow.IngestRequestFlowItem;
import fr.cnes.regards.modules.ingest.service.IIngestService;
import fr.cnes.regards.modules.ingest.service.conf.IngestConfigurationProperties;

/**
 * This handler absorbs the incoming SIP flow
 *
 * @author Marc SORDI
 *
 */
@Component
public class IngestRequestFlowHandler extends AbstractRequestFlowHandler<IngestRequestFlowItem>
        implements ApplicationListener<ApplicationReadyEvent> {

    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(IngestRequestFlowHandler.class);

    @Autowired
    private IngestConfigurationProperties confProperties;

    @Autowired
    private ISubscriber subscriber;

    @Autowired
    private IIngestService ingestService;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        subscriber.subscribeTo(IngestRequestFlowItem.class, this);
    }

    /**
     * Bulk save queued items every second.
     */
    @Override
    @Scheduled(fixedDelayString = "${regards.ingest.request.flow.bulk.delay:1000}")
    protected void handleQueue() {
        super.handleQueue();
    }

    @Override
    protected Integer getBulkSize() {
        return confProperties.getMaxBulkSize();
    }

    @Override
    protected void processBulk(List<IngestRequestFlowItem> items) {
        ingestService.handleIngestRequests(items);
    }
}
