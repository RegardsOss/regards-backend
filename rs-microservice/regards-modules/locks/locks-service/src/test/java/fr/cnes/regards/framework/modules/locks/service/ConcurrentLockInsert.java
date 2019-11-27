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
package fr.cnes.regards.framework.modules.locks.service;

import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.test.context.TestPropertySource;

import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;

/**
 *
 * Test lock isolation
 * @author Marc SORDI
 *
 */
@Ignore
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=lock" },
        locations = { "classpath:regards_perf.properties", "classpath:batch.properties" })
public class ConcurrentLockInsert extends AbstractLockMultitenantTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConcurrentLockInsert.class);

    @Autowired
    private AutowireCapableBeanFactory beanFactory;

    @Test
    public void lockCollision() throws InterruptedException {

        Runnable firstR = new AsyncLock(getDefaultTenant(), "first", 3000);
        beanFactory.autowireBean(firstR);
        Thread first = new Thread(firstR);

        Runnable secondR = new AsyncLock(getDefaultTenant(), "second", 3000);
        beanFactory.autowireBean(secondR);
        Thread second = new Thread(secondR);

        first.start();
        second.start();

        Thread.sleep(30_000);
    }

    static class AsyncLock implements Runnable {

        private final String tenant;

        private final String lockName;

        private final long sleep;

        @Autowired
        private IRuntimeTenantResolver runtimeTenantResolver;

        @Autowired
        private ILockService lockService;

        public AsyncLock(String tenant, String lockName, long sleep) {
            this.tenant = tenant;
            this.lockName = lockName;
            this.sleep = sleep;
        }

        @Override
        public void run() {
            try {
                runtimeTenantResolver.forceTenant(tenant);
                lockService.obtainLockOrSkip(lockName, this, 30000);
                Thread.sleep(sleep);
            } catch (InterruptedException e) {
                LOGGER.error("Interrupted", e);
            } finally {
                lockService.releaseLock(lockName, this);
                runtimeTenantResolver.clearTenant();
            }
        }

    }

}
