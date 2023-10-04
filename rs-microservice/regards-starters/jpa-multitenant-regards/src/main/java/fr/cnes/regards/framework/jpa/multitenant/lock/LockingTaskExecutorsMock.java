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
package fr.cnes.regards.framework.jpa.multitenant.lock;

import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockingTaskExecutor.Task;
import net.javacrumbs.shedlock.core.LockingTaskExecutor.TaskResult;
import net.javacrumbs.shedlock.core.LockingTaskExecutor.TaskWithResult;

import javax.sql.DataSource;

/**
 * Mock class for LockingTaskExecutors for tests purpose to avoid usage of scheduler locks.
 * In test context, tests can be stopped before lock is released.
 *
 * @author Sébastien Binda
 */
public class LockingTaskExecutorsMock implements ILockingTaskExecutors {

    @Override
    public void registerLockingTaskExecutor(String tenant, DataSource datasource) {
        // Nothing to do
    }

    @Override
    public void removeLockingTaskExecutor(String tenant) {
        // Nothing to do
    }

    @Override
    public void executeWithLock(Runnable task, LockConfiguration lockConfig) {
        task.run();
    }

    @Override
    public void executeWithLock(Task task, LockConfiguration lockConfig) throws Throwable {
        task.call();
    }

    @Override
    public void assertLocked() {
        // Nothing to do
    }

    @Override
    public <T> TaskResult<T> executeWithLock(TaskWithResult<T> task, LockConfiguration lockConfig) throws Throwable {
        return (TaskResult<T>) task.call();
    }
}
