/*
 * Copyright 2017-2025 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
 * along with REGARDS. If not, see `<http://www.gnu.org/licenses/>`.
 */
package fr.cnes.regards.modules.notification.service;

import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.jpa.multitenant.test.AbstractMultitenantServiceIT;
import fr.cnes.regards.modules.dam.client.dataaccess.IAccessGroupClient;
import fr.cnes.regards.modules.emails.client.IEmailClient;
import fr.cnes.regards.modules.notification.dao.INotificationSettingsRepository;
import fr.cnes.regards.modules.notification.domain.NotificationSettings;
import fr.cnes.regards.modules.storage.client.IStorageDownloaderRestClient;
import fr.cnes.regards.modules.storage.client.IStorageRestClient;
import fr.cnes.regards.modules.storage.client.IStorageSettingClient;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 * @author Stephane Cortine
 **/
@ActiveProfiles({ "test", "nomail" })
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=notif_tests_1",
                                   "regards.accounts.root.user.login=test@test.fr",
                                   "purge.cron.expression=0 0 5 * * ?" })
public class NotificationSettingsServiceIT extends AbstractMultitenantServiceIT {

    private static final String EMAIL_TEST = "concurrent-user-test@example.com";

    @Autowired
    private INotificationSettingsRepository repository;

    @Autowired
    private INotificationSettingsService service;

    @MockBean
    private IAccessGroupClient accessGroupClient;

    @MockBean
    private IEmailClient emailClient;

    @MockBean
    private IStorageSettingClient storageSettingClient;

    @MockBean
    private IStorageRestClient storageDownloadQuotaRestClient;

    @MockBean
    private IStorageDownloaderRestClient storageDownloaderRestclient;

    @MockBean
    private IResourceService resourceService;

    @Before
    public void clearData() {
        runtimeTenantResolver.forceTenant(getDefaultTenant());

        repository.deleteAll();
    }

    @Test
    public void shouldInsertOnlyOnceUnderConcurrentAccess() throws InterruptedException {
        // Given
        int threads = 10;
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch finished = new CountDownLatch(threads);

        ConcurrentLinkedQueue<NotificationSettings> results = new ConcurrentLinkedQueue<>();

        ConcurrentLinkedQueue<Throwable> errors = new ConcurrentLinkedQueue<>();

        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(10);
        taskExecutor.setMaxPoolSize(20);
        taskExecutor.setQueueCapacity(100);
        taskExecutor.initialize();

        for (int i = 0; i < threads; i++) {
            taskExecutor.execute(() -> {
                runtimeTenantResolver.forceTenant(getDefaultTenant());

                try {
                    latch.await(); // Wait the starting
                    NotificationSettings ns = service.retrieveNotificationSettings(EMAIL_TEST); // Test method
                    results.add(ns);
                } catch (Exception e) {
                    e.printStackTrace();
                    errors.add(e);
                } finally {
                    finished.countDown();
                }
            });
        }

        // When
        latch.countDown(); // Run thread
        finished.await(5, TimeUnit.SECONDS); // Wait the end

        // Then
        if (!errors.isEmpty()) {
            fail("Raise exceptions in threads: " + errors);
        }

        List<NotificationSettings> all = repository.findAll();
        assertEquals("Only one entry", 1, all.size());

        for (NotificationSettings notificationSettings : results) {
            assertNotNull(notificationSettings);
            assertEquals(EMAIL_TEST, notificationSettings.getProjectUserEmail());
        }
    }

}


