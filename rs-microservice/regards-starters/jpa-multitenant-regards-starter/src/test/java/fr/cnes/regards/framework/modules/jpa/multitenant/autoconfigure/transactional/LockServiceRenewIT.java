/*
 * Copyright 2017-2023 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.modules.jpa.multitenant.autoconfigure.transactional;

import fr.cnes.regards.framework.jpa.multitenant.lock.LockService;
import fr.cnes.regards.framework.jpa.multitenant.lock.LockServiceTask;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import org.awaitility.Awaitility;
import org.awaitility.Durations;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Test class for {@link  LockService#renewLock(String)}
 *
 * @author Thibaud Michaudel
 **/
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { LockServiceTestConfiguration.class })
@ActiveProfiles("test")
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=lock_service_renew_test",
                                   "regards.lock.time.to.live=1000" })
public class LockServiceRenewIT {

    private static final Logger LOGGER = getLogger(LockServiceRenewIT.class);

    @Autowired
    LockService lockService;

    @Autowired
    private IRuntimeTenantResolver tenantResolver;

    ExecutorService threadPool = Executors.newFixedThreadPool(100);

    @Test
    public void lock_service_renew_test() throws InterruptedException {
        List<String> resultList = new ArrayList<>();
        threadPool.submit(() -> runRenewingProcess("lock1", resultList, "run1"));
        Thread.sleep(750);
        threadPool.submit(() -> runWithLock("lock1", resultList, "run2"));
        Thread.sleep(250);
        threadPool.submit(() -> runWithLock("lock2", resultList, "run3"));

        Awaitility.await().atMost(Durations.TEN_SECONDS).until(() -> resultList.size() == 3);
        Assertions.assertEquals("run3", resultList.get(0));
        Assertions.assertEquals("run1", resultList.get(1));
        Assertions.assertEquals("run2", resultList.get(2));
    }

    private boolean runWithLock(String lock, List<String> resultList, String textToAdd) throws InterruptedException {
        tenantResolver.forceTenant("test1");
        return lockService.runWithLock(lock, new TestProcess(resultList, textToAdd));
    }

    private boolean runRenewingProcess(String lock, List<String> resultList, String textToAdd)
        throws InterruptedException {
        tenantResolver.forceTenant("test1");
        return lockService.runWithLock(lock, new TestRenewingProcess(lock, resultList, textToAdd));
    }

    private static class TestProcess implements LockServiceTask {

        private final List<String> resultList;

        private final String textToAdd;

        private TestProcess(List<String> resultList, String textToAdd) {
            this.resultList = resultList;
            this.textToAdd = textToAdd;
        }

        @Override
        public void run() {
            LOGGER.info("process {} started", textToAdd);
            resultList.add(textToAdd);
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private class TestRenewingProcess implements LockServiceTask {

        private String lock;

        private List<String> resultList;

        private String textToAdd;

        private TestRenewingProcess(String lock, List<String> resultList, String textToAdd) {
            this.resultList = resultList;
            this.textToAdd = textToAdd;
            this.lock = lock;
        }

        @Override
        public void run() {
            try {
                LOGGER.info("process {} started", textToAdd);
                Thread.sleep(750);
                lockService.renewLock(lock);
                Thread.sleep(500);
                resultList.add(textToAdd);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
