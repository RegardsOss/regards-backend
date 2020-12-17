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
package fr.cnes.regards.framework.jpa.multitenant.lock;

import java.util.UUID;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;

/**
 * Base class utilities for task schedulers based on {@link LockingTaskExecutors} locking.
 * @author Marc SORDI
 *
 */
public abstract class AbstractTaskScheduler {

    protected static final String INSTANCE_RANDOM_ID = "------------------------------> "
            + UUID.randomUUID().toString();

    @Value("${spring.application.name}")
    private String applicationName;

    protected void traceScheduling(String tenant, String type) {
        getLogger().trace("[{}][{}] Scheduling {}", INSTANCE_RANDOM_ID, tenant, type);
    }

    protected void handleSchedulingError(String type, String title, Throwable e) {
        String errorMessage = String.format("Error scheduling %s", type);
        getLogger().warn(errorMessage, e);
    }

    abstract protected Logger getLogger();
}
