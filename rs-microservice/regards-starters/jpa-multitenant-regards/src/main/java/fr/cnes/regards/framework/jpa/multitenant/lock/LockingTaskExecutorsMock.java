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

import jakarta.validation.constraints.NotNull;
import net.javacrumbs.shedlock.core.DefaultLockingTaskExecutor;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SimpleLock;

import javax.sql.DataSource;
import java.util.Optional;

/**
 * Mock class for LockingTaskExecutors for tests purpose to avoid usage of scheduler locks.
 * In test context, tests can be stopped before lock is released.
 *
 * @author Sébastien Binda
 */
public class LockingTaskExecutorsMock extends LockingTaskExecutors {

    private static final class SimpleLockMock implements SimpleLock {

        @Override
        public void unlock() {
            // Do nothing
        }
    }

    private static final class MockLockProvider implements LockProvider {

        @Override
        public @NotNull Optional<SimpleLock> lock(@NotNull LockConfiguration lockConfiguration) {
            return Optional.of(new SimpleLockMock());
        }
    }

    @Override
    public void registerLockingTaskExecutor(String tenant, DataSource datasource) {
        taskExecutors.put(tenant, new DefaultLockingTaskExecutor(new MockLockProvider()));
    }

}
