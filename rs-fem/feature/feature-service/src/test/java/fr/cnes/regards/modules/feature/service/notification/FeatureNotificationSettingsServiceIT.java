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


package fr.cnes.regards.modules.feature.service.notification;

import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.modules.tenant.settings.domain.DynamicTenantSetting;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.modules.feature.domain.settings.FeatureNotificationSettings;
import fr.cnes.regards.modules.feature.service.AbstractFeatureMultitenantServiceTest;
import fr.cnes.regards.modules.feature.service.settings.FeatureNotificationSettingsService;
import org.junit.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test for {@link FeatureNotificationSettings}
 * @author Iliana Ghazali
 */

@TestPropertySource(
        properties = { "spring.jpa.properties.hibernate.default_schema=feature_notification_settings_service_it",
                "regards.amqp.enabled=true"})
@ActiveProfiles(value = {"testAmqp", "nohandler", "noscheduler"})
public class FeatureNotificationSettingsServiceIT extends AbstractFeatureMultitenantServiceTest {

    @Autowired
    FeatureNotificationSettingsService notificationSettingsService;

    @Autowired
    IRuntimeTenantResolver runtimeTenantResolver;

    @BeforeEach
    public void reset() throws EntityException {
        notificationSettingsService.resetSettings();
    }

    @Test
    @Purpose("Check notification settings are retrieved")
    public void testRetrieve() {
        List<DynamicTenantSetting> notificationSettings = notificationSettingsService.retrieve();
        assertEquals(1, notificationSettings.size());
        assertEquals(FeatureNotificationSettings.ACTIVE_NOTIFICATION, notificationSettings.get(0).getName());
        assertEquals(FeatureNotificationSettings.DEFAULT_ACTIVE_NOTIFICATION, notificationSettings.get(0).getDefaultValue());
        assertEquals(FeatureNotificationSettings.DEFAULT_ACTIVE_NOTIFICATION, notificationSettings.get(0).getValue());
    }

    @Test
    @Purpose("Check the update of existing notification settings")
    public void testUpdate() throws EntityException {
        notificationSettingsService.setActiveNotification(false);
        assertEquals(false, notificationSettingsService.retrieve().get(0).getValue());
    }

}
