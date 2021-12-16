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
package fr.cnes.regards.modules.workermanager.service.flow;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.batch.IBatchHandler;
import fr.cnes.regards.framework.amqp.configuration.AmqpChannel;
import fr.cnes.regards.framework.amqp.configuration.IAmqpAdmin;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.modules.workermanager.dto.events.EventHeadersHelper;
import fr.cnes.regards.modules.workermanager.dto.events.in.RequestEvent;
import fr.cnes.regards.modules.workermanager.dto.events.out.ResponseEvent;
import fr.cnes.regards.modules.workermanager.dto.requests.RequestStatus;
import fr.cnes.regards.modules.workermanager.dto.requests.SessionsRequestsInfo;
import fr.cnes.regards.modules.workermanager.service.requests.RequestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.validation.DataBinder;
import org.springframework.validation.Errors;

import java.util.List;

/**
 * AMQP batch handler to handle {@link RequestEvent}s<br/>
 * All requests are validated before being dispatched to an available matching worker.
 *
 * @author Sébastien Binda
 */
@Component
public class RequestHandler implements ApplicationListener<ApplicationReadyEvent>, IBatchHandler<RequestEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestHandler.class);

    /**
     * Bulk size limit to handle messages
     */
    @Value("${regards.workermanager.request.bulk.size:1000}")
    private int BULK_SIZE;

    @Autowired
    private ISubscriber subscriber;

    @Autowired
    private RequestService workerManagerService;

    @Autowired
    private IPublisher publisher;

    @Autowired
    private ITenantResolver tenantResolver;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        subscriber.subscribeTo(RequestEvent.class, this);
        // Init response out exchange
        publisher.initExchange(tenantResolver.getAllActiveTenants(), ResponseEvent.class);
    }

    @Override
    public Class<RequestEvent> getMType() {
        return RequestEvent.class;
    }

    @Override
    public Errors validate(RequestEvent message) {
        DataBinder db = new DataBinder(message);
        String tenant = message.getMessageProperties().getHeader(EventHeadersHelper.TENANT_HEADER);
        String requestId = message.getMessageProperties().getHeader(EventHeadersHelper.REQUEST_ID_HEADER);
        Errors errors = db.getBindingResult();
        if (StringUtils.isEmpty(tenant)) {
            errors.rejectValue(EventHeadersHelper.TENANT_HEADER, EventHeadersHelper.MISSING_HEADER_CODE);
        }
        if (StringUtils.isEmpty(requestId)) {
            errors.rejectValue(EventHeadersHelper.REQUEST_ID_HEADER, EventHeadersHelper.MISSING_HEADER_CODE);
        }
        return errors;
    }

    @Override
    public void handleBatchWithRaw(List<RequestEvent> messages, List<Message> rawMessages) {
        long start = System.currentTimeMillis();
        LOGGER.info("Handling {} messages", rawMessages.size());
        SessionsRequestsInfo requestInfo = workerManagerService.registerRequests(rawMessages);
        LOGGER.info("{} dispatched request(s) ,{} delayed request(s) and {} skipped event(s) registered in {} ms",
                    requestInfo.getRequests(RequestStatus.DISPATCHED).size(),
                    requestInfo.getRequests(RequestStatus.NO_WORKER_AVAILABLE).size(),
                    requestInfo.getSkippedEvents().size(), System.currentTimeMillis() - start);
    }

    @Override
    public void handleBatch(List<RequestEvent> messages) {
        // Nothing to do
    }

    @Override
    public int getBatchSize() {
        return BULK_SIZE;
    }

    @Override
    public boolean isDedicatedDLQEnabled() {
        // Warning : Do not set dedicated DLQ for queue creation consistency with Notifier plugins when sending message
        // from feature notifier to workerManager with WorkerMangerSender
        return false;
    }
}
