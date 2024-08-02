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


package fr.cnes.regards.modules.ingest.service.settings;

import fr.cnes.regards.framework.jpa.multitenant.test.AbstractMultitenantServiceIT;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.modules.tenant.settings.domain.DynamicTenantSetting;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.modules.ingest.domain.settings.IngestSettings;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test for {@link IngestSettingsService}
 *
 * @author Iliana Ghazali
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=aip_notification_settings_service_it" },
                    locations = { "classpath:application-test.properties" })
@ActiveProfiles(value = { "noscheduler" })
public class IngestSettingsServiceIT extends AbstractMultitenantServiceIT {

    @Autowired
    IngestSettingsService notificationSettingsService;

    @Autowired
    IRuntimeTenantResolver runtimeTenantResolver;

    @Before
    public void reset() throws EntityException {
        simulateApplicationStartedEvent();
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        notificationSettingsService.resetSettings();
    }

    @Test
    @Purpose("Check notification settings are retrieved")
    public void testRetrieve() {
        Set<DynamicTenantSetting> notificationSettings = notificationSettingsService.retrieve();
        assertEquals(2, notificationSettings.size());
        DynamicTenantSetting setting = notificationSettings.stream().findFirst().get();
        assertEquals(IngestSettings.ACTIVE_NOTIFICATION, setting.getName());
        assertEquals(IngestSettings.DEFAULT_ACTIVE_NOTIFICATION, setting.getDefaultValue());
        assertEquals(IngestSettings.DEFAULT_ACTIVE_NOTIFICATION, setting.getValue());
    }

    @Test
    @Purpose("Check the update of existing notification settings")
    public void testUpdate() throws EntityException {
        notificationSettingsService.setActiveNotification(true);
        assertEquals(true, notificationSettingsService.isActiveNotification());
    }
}
