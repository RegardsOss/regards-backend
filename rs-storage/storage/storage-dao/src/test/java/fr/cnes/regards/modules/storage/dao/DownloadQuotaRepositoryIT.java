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
package fr.cnes.regards.modules.storage.dao;

import fr.cnes.regards.framework.jpa.multitenant.test.AbstractDaoTransactionalTest;
import fr.cnes.regards.framework.jpa.multitenant.test.AbstractMultitenantServiceTest;
import fr.cnes.regards.framework.jpa.multitenant.test.DefaultDaoTestConfiguration;
import fr.cnes.regards.modules.storage.dao.config.StorageDaoConfiguration;
import fr.cnes.regards.modules.storage.domain.database.UserQuotaAggregate;
import fr.cnes.regards.modules.storage.domain.database.UserRateAggregate;
import fr.cnes.regards.modules.storage.domain.database.repository.IDownloadQuotaRepository;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.concurrent.*;

import static org.junit.Assert.assertEquals;

@TestPropertySource(
    properties = {
        "spring.jpa.properties.hibernate.default_schema=storage_download_gauge_dao",
//        "regards.jpa.multitenant.tenants[0].url=jdbc:tc:postgresql:///GaugeRepositoryIT",
//        "regards.jpa.multitenant.tenants[0].tenant=PROJECT"
    }
)
@ContextConfiguration(classes = { DefaultDaoTestConfiguration.class, StorageDaoConfiguration.class })
public class DownloadQuotaRepositoryIT extends AbstractMultitenantServiceTest {

    @Autowired
    private IDownloadQuotaRepository repo;

    // never too sure
    @Before
    @After
    public void clean() {
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        repo.deleteAll();
        runtimeTenantResolver.clearTenant();
    }

    @Test
    public void upsertOrCombineDownloadRate_should_insert_or_accumulate() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(3);

        String email = "plop";
        int instanceCount = 5;
        Random rand = new Random();

        long iter = 10_000;
        for (int i=0; i<iter; i++) {
            executor.submit(() -> {
                runtimeTenantResolver.forceTenant(getDefaultTenant());
                repo.upsertOrCombineDownloadRate(String.valueOf(rand.nextInt(instanceCount)), email, 1L, LocalDateTime.now());
                runtimeTenantResolver.clearTenant();
            });
        }

        executor.shutdown();
        executor.awaitTermination(600, TimeUnit.SECONDS);

        UserRateAggregate userRate = repo.fetchDownloadRatesSum(email);

        // then
        assertEquals(iter, userRate.getGauge().longValue());
    }


    @Test
    public void upsertOrCombineDownloadQuota_should_insert_or_accumulate() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(3);

        String email = "plop";
        int instanceCount = 5;
        Random rand = new Random();

        long iter = 10_000;
        for (int i=0; i<iter; i++) {
            executor.submit(() -> {
                runtimeTenantResolver.forceTenant(getDefaultTenant());
                repo.upsertOrCombineDownloadQuota(String.valueOf(rand.nextInt(instanceCount)), email, 1L);
                runtimeTenantResolver.clearTenant();
            });
        }

        executor.shutdown();
        executor.awaitTermination(600, TimeUnit.SECONDS);

        UserQuotaAggregate userQuotaAggregate = repo.fetchDownloadQuotaSum(email);

        // then
        assertEquals(iter, userQuotaAggregate.getCounter().longValue());
    }
}
