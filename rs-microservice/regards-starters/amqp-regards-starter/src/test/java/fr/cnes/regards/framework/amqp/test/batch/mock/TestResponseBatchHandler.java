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

import fr.cnes.regards.framework.amqp.batch.IBatchHandler;
import fr.cnes.regards.framework.amqp.test.batch.domain.ResponseTestedMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.Errors;

import java.util.ArrayList;
import java.util.List;

/**
 * Receives batch of {@link ResponseTestedMessage}s, which were published in test scenarios because of AMQP errors
 * (denied message or conversion exception).
 *
 * @author Iliana Ghazali
 */
public class TestResponseBatchHandler implements IBatchHandler<ResponseTestedMessage> {

    protected static final Logger LOGGER = LoggerFactory.getLogger(TestResponseBatchHandler.class);

    private int handleCalls;

    private List<ResponseTestedMessage> responseTestedMessages;

    public TestResponseBatchHandler() {
        this.handleCalls = 0;
        this.responseTestedMessages = new ArrayList<>();
    }

    @Override
    public Class<ResponseTestedMessage> getMType() {
        return ResponseTestedMessage.class;
    }

    @Override
    public Errors validate(ResponseTestedMessage message) {
        return null;
    }

    @Override
    public void handleBatch(List<ResponseTestedMessage> messages) {
        getLogger().info("Received {} valid ResponseTestedMessage.", messages.size());
        incrementHandleCalls();
        setResponseTestedMessages(messages);
    }

    public void incrementHandleCalls() {
        handleCalls++;
    }

    public int getHandleCalls() {
        return handleCalls;
    }

    protected Logger getLogger() {
        return LOGGER;
    }

    public List<ResponseTestedMessage> getResponseTestedMessages() {
        return responseTestedMessages;
    }

    public void setResponseTestedMessages(List<ResponseTestedMessage> responseTestedMessages) {
        this.responseTestedMessages = responseTestedMessages;
    }

    @Override
    public boolean isDedicatedDLQEnabled() {
        return true;
    }
}
