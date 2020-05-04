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

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;

import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import net.javacrumbs.shedlock.core.DefaultLockingTaskExecutor;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import net.javacrumbs.shedlock.core.LockingTaskExecutor.Task;
import net.javacrumbs.shedlock.core.LockingTaskExecutor.TaskResult;
import net.javacrumbs.shedlock.core.LockingTaskExecutor.TaskWithResult;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;

/**
 * @author Marc SORDI
 *
 */
public class LockingTaskExecutors {

    /**
     * List of available {@link LockProvider}
     */
    private final Map<String, LockProvider> lockProviders = new HashMap<>();

    /**
     * List of {@link LockingTaskExecutor} for execution action with lock.
     */
    private final Map<String, LockingTaskExecutor> taskExecutors = new HashMap<>();

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    private LockingTaskExecutor getTaskExecutor() {
        String tenant = runtimeTenantResolver.getTenant();
        if (tenant == null) {
            throw new UnsupportedOperationException("Tenant must be set before calling executors!");
        }
        if (taskExecutors.containsKey(tenant)) {
            return taskExecutors.get(tenant);
        }
        throw new IllegalArgumentException(
                String.format("Unexpected tenant %s or lock provider not initialized for this tenant", tenant));
    }

    public void registerLockingTaskExecutor(String tenant, DataSource datasource) {
        LockProvider lockProvider = new JdbcTemplateLockProvider(datasource);
        lockProviders.put(tenant, lockProvider);
        taskExecutors.put(tenant, new DefaultLockingTaskExecutor(lockProvider));
    }

    public void removeLockingTaskExecutor(String tenant) {
        lockProviders.remove(tenant);
        taskExecutors.remove(tenant);
    }

    public void executeWithLock(Runnable task, LockConfiguration lockConfig) {
        // Delegate to schedlock
        getTaskExecutor().executeWithLock(task, lockConfig);
    }

    public void executeWithLock(Task task, LockConfiguration lockConfig) throws Throwable {
        // Delegate to schedlock
        getTaskExecutor().executeWithLock(task, lockConfig);
    }

    public <T> TaskResult<T> executeWithLock(TaskWithResult<T> task, LockConfiguration lockConfig) throws Throwable {
        // Delegate to schedlock
        return getTaskExecutor().executeWithLock(task, lockConfig);
    }
}
