/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.framework.amqp.batch.IBatchHandler;

/**
 * @author Marc SORDI
 *
 */
public class BatchHandler implements IBatchHandler<BatchMessage> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BatchHandler.class);

    private final Map<String, Integer> countByTenants = new HashMap<>();

    private final Map<String, Integer> failByTenants = new HashMap<>();

    private Integer calls = 0;

    public static final String FAKE_TENANT = "FAKE";

    public static final String FAIL_TENANT = "FAIL";

    @Override
    public boolean validate(String tenant, BatchMessage message) {
        if (FAKE_TENANT.equals(tenant)) {
            return false;
        }
        return true;
    }

    @Override
    public void handleBatch(String tenant, List<BatchMessage> messages) {

        if (FAKE_TENANT.equals(tenant)) {
            throw new IllegalArgumentException("Unknown tenant");
        }

        if (FAIL_TENANT.equals(tenant)) {
            incrementFails(tenant);
            throw new IllegalArgumentException("Fail tenant");
        }

        calls++;
        for (BatchMessage message : messages) {
            Assert.assertTrue("Bad tenant", message.getMessage().startsWith(tenant));
            LOGGER.info(message.getMessage());
            incrementCount(tenant);
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

    public Integer getCalls() {
        return calls;
    }
}
