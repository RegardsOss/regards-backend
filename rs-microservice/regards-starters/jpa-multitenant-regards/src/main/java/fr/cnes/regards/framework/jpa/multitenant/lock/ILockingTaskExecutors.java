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

import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;

import javax.sql.DataSource;

/**
 * @author Sébastien Binda
 **/
public interface ILockingTaskExecutors {

    void registerLockingTaskExecutor(String tenant, DataSource datasource);

    void removeLockingTaskExecutor(String tenant);

    void executeWithLock(Runnable task, LockConfiguration lockConfig);

    void executeWithLock(LockingTaskExecutor.Task task, LockConfiguration lockConfig) throws Throwable;

    void assertLocked();

    <T> LockingTaskExecutor.TaskResult<T> executeWithLock(LockingTaskExecutor.TaskWithResult<T> task,
                                                          LockConfiguration lockConfig) throws Throwable;

}
