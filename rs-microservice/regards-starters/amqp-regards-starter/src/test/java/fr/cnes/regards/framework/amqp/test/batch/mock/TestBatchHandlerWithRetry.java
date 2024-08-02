/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
import fr.cnes.regards.framework.amqp.batch.RetryBatchMessageHandler;
import fr.cnes.regards.framework.amqp.test.batch.domain.TestRuntimeException;
import fr.cnes.regards.framework.amqp.test.batch.domain.TestedMessage;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.Errors;
import org.springframework.validation.MapBindingResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handler with retry feature enabled. Actions can be triggered according to the status of the
 * input messages to simulate different behaviours.
 */
public class TestBatchHandlerWithRetry implements IBatchHandler<TestedMessage> {

    protected static final Logger LOGGER = LoggerFactory.getLogger(TestBatchHandler.class);

    private final IRuntimeTenantResolver tenantResolver;

    // Utils to track batch behaviours

    private final Map<String, Integer> validCountByTenants;

    private final Map<String, Integer> invalidByTenants;

    private Integer handleCalls;

    public TestBatchHandlerWithRetry(final IRuntimeTenantResolver tenantResolver) {
        this.tenantResolver = tenantResolver;
        this.handleCalls = 0;
        this.validCountByTenants = new HashMap<>();
        this.invalidByTenants = new HashMap<>();
    }

    @Override
    public Class<TestedMessage> getMType() {
        return TestedMessage.class;
    }

    @Override
    public Errors validate(TestedMessage message) {
        Errors errors = new MapBindingResult(new HashMap<>(), message.getClass().getName());
        if (message.getMessageType() == SimulatedMessageTypeEnum.INVALID) {
            errors.rejectValue("message.content", "message.content.error", "Default message");
            incrementInvalidCountByTenant(tenantResolver.getTenant());
        }
        return errors;
    }

    @Override
    public void handleBatch(List<TestedMessage> messages) {
        getLogger().info("Received {} AMQP messages", messages.size());
        incrementHandleCalls();
        for (TestedMessage message : messages) {
            switch (message.getMessageType()) {
                case VALID -> incrementValidCountByTenant(tenantResolver.getTenant());
                case INVALID -> incrementInvalidCountByTenant(tenantResolver.getTenant());
                case TEMPORARY_UNEXPECTED_EXCEPTION -> {
                    if (message.getMessageProperties().getHeader(RetryBatchMessageHandler.X_RETRY_HEADER) == null) {
                        throw new TestRuntimeException("Expected exception to verify retry.");
                    }
                    incrementValidCountByTenant(tenantResolver.getTenant());
                }
                case PERMANENT_UNEXPECTED_EXCEPTION ->
                    throw new TestRuntimeException("Expected exception to verify retry.");
            }
        }
    }

    public void incrementHandleCalls() {
        handleCalls++;
    }

    private void incrementValidCountByTenant(String tenant) {
        if (validCountByTenants.containsKey(tenant)) {
            validCountByTenants.put(tenant, validCountByTenants.get(tenant) + 1);
        } else {
            validCountByTenants.put(tenant, 1);
        }
    }

    public Integer getValidCountByTenant(String tenant) {
        if (validCountByTenants.containsKey(tenant)) {
            return validCountByTenants.get(tenant);
        }
        return 0;
    }

    private void incrementInvalidCountByTenant(String tenant) {
        if (invalidByTenants.containsKey(tenant)) {
            invalidByTenants.put(tenant, invalidByTenants.get(tenant) + 1);
        } else {
            invalidByTenants.put(tenant, 1);
        }
    }

    public Integer getHandleCalls() {
        return handleCalls;
    }

    protected Logger getLogger() {
        return LOGGER;
    }

    @Override
    public boolean isRetryEnabled() {
        return true;
    }
}
