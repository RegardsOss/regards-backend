/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import fr.cnes.regards.framework.jpa.multitenant.test.AbstractMultitenantServiceTest;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.framework.modules.tenant.settings.domain.DynamicTenantSetting;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.modules.ingest.domain.settings.AIPNotificationSettings;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * Test for {@link AIPNotificationSettingsService}
 * @author Iliana Ghazali
 */
@TestPropertySource(
        properties = { "spring.jpa.properties.hibernate.default_schema=aip_notification_settings_service_it" },
        locations = { "classpath:application-test.properties" })
@ActiveProfiles(value = {"noschedule"})
public class AIPNotificationSettingsServiceIT extends AbstractMultitenantServiceTest {

    @Autowired
    AIPNotificationSettingsService notificationSettingsService;

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
        List<DynamicTenantSetting> notificationSettings = notificationSettingsService.retrieve();
        assertEquals(1, notificationSettings.size());
        assertEquals(AIPNotificationSettings.ACTIVE_NOTIFICATION, notificationSettings.get(0).getName());
        assertEquals(AIPNotificationSettings.DEFAULT_ACTIVE_NOTIFICATION, notificationSettings.get(0).getDefaultValue());
        assertEquals(AIPNotificationSettings.DEFAULT_ACTIVE_NOTIFICATION, notificationSettings.get(0).getValue());
    }

    @Test
    @Purpose("Check the update of existing notification settings")
    public void testUpdate() throws EntityNotFoundException, EntityOperationForbiddenException, EntityInvalidException {
        notificationSettingsService.update(new DynamicTenantSetting(null, AIPNotificationSettings.ACTIVE_NOTIFICATION, null, true, true));
        assertEquals(true, notificationSettingsService.retrieve().get(0).getValue());
    }
}
