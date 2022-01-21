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
package fr.cnes.regards.framework.amqp.test.batch;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.Errors;
import org.springframework.validation.MapBindingResult;

import fr.cnes.regards.framework.amqp.batch.IBatchHandler;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;

/**
 * @author Marc SORDI
 *
 */
public class BatchHandler implements IBatchHandler<BatchedMessage> {

    protected static final Logger LOGGER = LoggerFactory.getLogger(BatchHandler.class);

    private final Map<String, Integer> countByTenants = new HashMap<>();

    private final Map<String, Integer> failByTenants = new HashMap<>();

    private final Map<String, Integer> invalidByTenants = new HashMap<>();

    private Integer calls = 0;

    public static final String FIELD = "message.content";

    // Message content managing batch behavior

    public static final String VALID = "ThisIsValid";

    public static final String INVALID = "ThisIsInvalid";

    public static final String THROW_EXCEPTION = "ThrowIt";

    private final IRuntimeTenantResolver tenantResolver;

    public BatchHandler(final IRuntimeTenantResolver tenantResolver) {
        this.tenantResolver = tenantResolver;
    }

    @Override
    public Class<BatchedMessage> getMType() {
        return BatchedMessage.class;
    }

    @Override
    public Errors validate(BatchedMessage message) {

        Map<String, String> toValidate = new HashMap<>();
        toValidate.put(FIELD, INVALID);

        Errors errors = new MapBindingResult(toValidate, message.getClass().getName());

        if (INVALID.equals(message.getMessage())) {
            errors.rejectValue(FIELD, "message.content.error", "Default message");
            incrementInvalid(tenantResolver.getTenant());
        }
        return errors;
    }

    @Override
    public void handleBatch(List<BatchedMessage> messages) {

        calls++;
        for (BatchedMessage message : messages) {
            getLogger().info(message.getMessage());
            if (VALID.equals(message.getMessage())) {
                incrementCount(tenantResolver.getTenant());
            } else {
                incrementFails(tenantResolver.getTenant());
                throw new IllegalArgumentException(
                        String.format("One message processing throws exception and breaks the batch processing : %s",
                                      message.getMessage()));
            }
        }
    }

    private void incrementCount(String tenant) {
        if (countByTenants.containsKey(tenant)) {
            countByTenants.put(tenant, countByTenants.get(tenant) + 1);
        } else {
            countByTenants.put(tenant, 1);
        }
    }

    public Integer getCountByTenant(String tenant) {
        if (countByTenants.containsKey(tenant)) {
            return countByTenants.get(tenant);
        }
        return 0;
    }

    private void incrementFails(String tenant) {
        if (failByTenants.containsKey(tenant)) {
            failByTenants.put(tenant, failByTenants.get(tenant) + 1);
        } else {
            failByTenants.put(tenant, 1);
        }
    }

    public Integer getFailsByTenant(String tenant) {
        if (failByTenants.containsKey(tenant)) {
            return failByTenants.get(tenant);
        }
        return 0;
    }

    private void incrementInvalid(String tenant) {
        if (invalidByTenants.containsKey(tenant)) {
            invalidByTenants.put(tenant, invalidByTenants.get(tenant) + 1);
        } else {
            invalidByTenants.put(tenant, 1);
        }
    }

    public Integer getInvalidByTenant(String tenant) {
        if (invalidByTenants.containsKey(tenant)) {
            return invalidByTenants.get(tenant);
        }
        return 0;
    }

    public Integer getCalls() {
        return calls;
    }

    protected Logger getLogger() {
        return LOGGER;
    }

    @Override
    public boolean isDedicatedDLQEnabled() {
        return true;
    }
}
