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
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.integration.jdbc.lock.DefaultLockRepository;
import org.springframework.integration.jdbc.lock.JdbcLockRegistry;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Service used to run a synchronous locked task. It ensures that no two tasks sharing the same lock will be run at the
 * same time.
 * If the service attempt to run a task with a lock already in use, it will retry running the task until the lock is
 * free.
 * The lock has a maximum duration of {@link #lockTimeToLiveInSeconds} but the lock duration can be renewed using {@link #renewLock(String)}.
 *
 * @author Thibaud Michaudel
 **/
@Service
public class LockService {

    private static final Logger LOGGER = getLogger(LockService.class);

    public static final String LOCK_PREFIX = "SHARED_";

    @Value("${regards.lock.time.to.live.in.seconds:60000}")
    private int lockTimeToLiveInSeconds;

    @Value("${regards.lock.try.timeout.in.seconds:3600}")
    private int lockTryTimeoutInSeconds;

    @Value("${regards.lock.cache.capacity:100000}")
    private int cacheCapacity;

    /**
     * Map of tenant with {@link JdbcLockRegistry} for execution task with lock.
     */
    private final Map<String, JdbcLockRegistry> lockRegistryMap;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    public LockService() {
        lockRegistryMap = new HashMap<>();
    }

    /**
     * Register a JdbcLockRegistry for the given tenant and dataSource
     */
    public void registerLockRegistry(String tenant, DataSource dataSource) {
        DefaultLockRepository lockRepository = new DefaultLockRepository(dataSource);
        lockRepository.setPrefix(LOCK_PREFIX);
        // Keep the lock very long time
        lockRepository.setTimeToLive(lockTimeToLiveInSeconds * 1000);
        lockRepository.afterPropertiesSet();

        JdbcLockRegistry lockRegistry = new JdbcLockRegistry(lockRepository);
        lockRegistry.setCacheCapacity(cacheCapacity);

        lockRegistryMap.put(tenant, lockRegistry);
    }

    /**
     * Remove an existing JdbcLockRegistry
     */
    public void removeLockRegistry(String tenant) {
        lockRegistryMap.remove(tenant);
    }

    /**
     * Run synchronously the given {@link LockServiceTask} with the given lock.
     * The process will wait for the lock to be free to run the task.
     * If the lock is still not free after the given time {@link lockTryTimeoutInSeconds} (very long time for the
     * waiting), stop trying and return false.
     */
    public <T> LockServiceResponse<T> runWithLock(String lockName, LockServiceTask process)
        throws InterruptedException {
        return tryRunWithLock(lockName, process, lockTryTimeoutInSeconds, TimeUnit.SECONDS);
    }

    /**
     * Try to run synchronously the given {@link LockServiceTask} with the given lock.
     * The process will wait for the lock to be free to run the task.
     * If the lock is still not free after the given time, stop trying and return false.
     *
     * @param timeToWait how long the service will try to get the lock
     * @return A response with executed field set to true if the process was run, false otherwise
     */
    public <T> LockServiceResponse<T> tryRunWithLock(String lockName,
                                                     LockServiceTask<T> process,
                                                     int timeToWait,
                                                     TimeUnit timeUnit) throws InterruptedException {
        JdbcLockRegistry lockRegistry = lockRegistryMap.get(runtimeTenantResolver.getTenant());
        LOGGER.debug("Getting lock {} for task {}", lockName, process.getClass().getSimpleName());

        Lock lock = lockRegistry.obtain(lockName);
        boolean lockAcquired = lock.tryLock(timeToWait, timeUnit);
        if (!lockAcquired) {
            LOGGER.warn("Unable to acquire lock {} for task {}", lockName, process.getClass().getSimpleName());
            return new LockServiceResponse<>(false);
        }

        return runProcess(lockName, process, lock);
    }

    /**
     * Try to run synchronously the given {@link LockServiceTask} with the given lock.
     * The process will be ran only if the lock is available when this method is called.
     * If the lock is still not free, immediately return a not executed response.
     *
     * @return true if the process was run, false otherwise
     */
    public <T> LockServiceResponse<T> tryRunWithLock(String lockName, LockServiceTask<T> process) {
        JdbcLockRegistry lockRegistry = lockRegistryMap.get(runtimeTenantResolver.getTenant());
        LOGGER.debug("Getting lock {} for task {}", lockName, process.getClass().getSimpleName());

        Lock lock = lockRegistry.obtain(lockName);
        boolean lockAcquired = lock.tryLock();
        if (!lockAcquired) {
            return new LockServiceResponse<>(false);
        }

        return runProcess(lockName, process, lock);
    }

    private static <T> LockServiceResponse<T> runProcess(String lockName, LockServiceTask<T> process, Lock lock) {
        LOGGER.debug("Acquired lock {} for task {}", lockName, process.getClass().getSimpleName());
        try {
            T response = process.run();
            return new LockServiceResponse<>(true, response);
        } catch (Throwable e) {
            LOGGER.error("Unexpected error during execution of locked task with lock {}, releasing the lock", lock);
            throw e;
        } finally {
            LOGGER.debug("Released lock {} for task {}", lockName, process.getClass().getSimpleName());
            lock.unlock();
        }
    }

    /**
     * Renew the given lock
     */
    public void renewLock(String lockName) {
        JdbcLockRegistry lockRegistry = lockRegistryMap.get(runtimeTenantResolver.getTenant());
        lockRegistry.renewLock(lockName);
    }

    /**
     * Getter method for the lock TimeToLive parameter
     */
    public int getTimeToLiveInSeconds() {
        return lockTimeToLiveInSeconds;
    }
}
