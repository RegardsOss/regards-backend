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
package fr.cnes.regards.framework.jpa.multitenant.lock;

import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import net.javacrumbs.shedlock.core.DefaultLockingTaskExecutor;
import net.javacrumbs.shedlock.core.LockAssert;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import net.javacrumbs.shedlock.core.LockingTaskExecutor.Task;
import net.javacrumbs.shedlock.core.LockingTaskExecutor.TaskResult;
import net.javacrumbs.shedlock.core.LockingTaskExecutor.TaskWithResult;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.support.KeepAliveLockProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

/**
 * Service to get a shared lock in database before running a given {@link Task}.
 * With method #executeWithLock(Task, LockConfiguration) you can run a task when the lock is available or fail after
 * timeout.
 *
 * @author Marc SORDI
 */
public class LockingTaskExecutors implements ILockingTaskExecutors {

    @Value("${regards.shedlock.scheduler.thread.pool.size:1}")
    private int schedulerThreadPoolSize;

    /**
     * Map of tenant with {@link LockingTaskExecutor} for execution action with lock.
     */
    protected final Map<String, LockingTaskExecutor> taskExecutors = new ConcurrentHashMap<>();

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
        throw new IllegalArgumentException(String.format(
            "Unexpected tenant %s or lock provider not initialized for this tenant",
            tenant));
    }

    @Override
    public void registerLockingTaskExecutor(String tenant, DataSource datasource) {
        JdbcTemplateLockProvider.Configuration configurationJdbcTemplateLockProvider = JdbcTemplateLockProvider.Configuration.builder()
                                                                                                                             .withJdbcTemplate(
                                                                                                                                 new JdbcTemplate(
                                                                                                                                     datasource))
                                                                                                                             // Use db time to avoid time de-synchronization between instances
                                                                                                                             .usingDbTime()
                                                                                                                             .build();
        JdbcTemplateLockProvider jdbcTemplateLockProvider = new JdbcTemplateLockProvider(
            configurationJdbcTemplateLockProvider);

        taskExecutors.put(tenant,
                          new DefaultLockingTaskExecutor(new KeepAliveLockProvider(jdbcTemplateLockProvider,
                                                                                   Executors.newScheduledThreadPool(
                                                                                       schedulerThreadPoolSize))));
    }

    @Override
    public void removeLockingTaskExecutor(String tenant) {
        taskExecutors.remove(tenant);
    }

    @Override
    public void executeWithLock(Runnable task, LockConfiguration lockConfig) {
        // Delegate to schedlock
        getTaskExecutor().executeWithLock(task, lockConfig);
    }

    @Override
    public void executeWithLock(Task task, LockConfiguration lockConfig) throws Throwable {
        // Delegate to schedlock
        getTaskExecutor().executeWithLock(task, lockConfig);
    }

    @Override
    public <T> TaskResult<T> executeWithLock(TaskWithResult<T> task, LockConfiguration lockConfig) throws Throwable {
        // Delegate to schedlock
        return getTaskExecutor().executeWithLock(task, lockConfig);
    }

    @Override
    public void assertLocked() {
        LockAssert.assertLocked();
    }
}
