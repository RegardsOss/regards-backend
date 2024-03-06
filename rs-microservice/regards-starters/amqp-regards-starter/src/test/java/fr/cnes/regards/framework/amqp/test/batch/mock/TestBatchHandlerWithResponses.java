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
package fr.cnes.regards.framework.amqp.test.batch.mock;

import fr.cnes.regards.framework.amqp.batch.dto.BatchMessage;
import fr.cnes.regards.framework.amqp.batch.dto.ResponseMessage;
import fr.cnes.regards.framework.amqp.configuration.AmqpConstants;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.test.batch.domain.ResponseTestedMessage;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;

/**
 * Handler inherited from {@link TestBatchHandler} with error message responses configured.
 */
public class TestBatchHandlerWithResponses extends TestBatchHandler {

    protected static final Logger LOGGER = LoggerFactory.getLogger(TestBatchHandlerWithResponses.class);

    public TestBatchHandlerWithResponses(IRuntimeTenantResolver tenantResolver) {
        super(tenantResolver);
    }

    @Override
    public boolean isDedicatedDLQEnabled() {
        return false;
    }

    protected Logger getLogger() {
        return LOGGER;
    }

    @Override
    public ResponseMessage<? extends ISubscribable> buildDeniedResponseForInvalidMessage(BatchMessage batchMessage,
                                                                                         String errorMessage) {
        return ResponseMessage.buildResponse(ResponseTestedMessage.buildResponseMessage(errorMessage,
                                                                                        batchMessage.getOrigin()
                                                                                                    .getMessageProperties()
                                                                                                    .getHeader(
                                                                                                        AmqpConstants.REGARDS_REQUEST_ID_HEADER)));
    }

    @Override
    public ResponseMessage<? extends ISubscribable> buildDeniedResponseForNotConvertedMessage(Message message,
                                                                                              String errorMessage) {
        return ResponseMessage.buildResponse(ResponseTestedMessage.buildResponseMessage(errorMessage,
                                                                                        message.getMessageProperties()
                                                                                               .getHeader(AmqpConstants.REGARDS_REQUEST_ID_HEADER)));
    }
}
