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
package fr.cnes.regards.modules.workermanager.service.settings;

import fr.cnes.regards.framework.jpa.multitenant.test.AbstractMultitenantServiceTest;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.framework.modules.tenant.settings.dao.IDynamicTenantSettingRepository;
import fr.cnes.regards.framework.modules.tenant.settings.service.DynamicTenantSettingService;
import fr.cnes.regards.framework.modules.tenant.settings.service.IDynamicTenantSettingService;
import fr.cnes.regards.modules.workermanager.domain.WorkerManagerSetting;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.Arrays;
import java.util.List;

@TestPropertySource(
        properties = { "spring.jpa.properties.hibernate.default_schema=worker_manager_settings" },
        locations = { "classpath:application-test.properties" })
public class SettingsTest extends AbstractMultitenantServiceTest {

    @Autowired
    private WorkerManagerSettingService settingService;

    @Autowired
    private DynamicTenantSettingService tenantSettingService;

    @Autowired
    private IDynamicTenantSettingRepository tenantSettingRepo;

    @Before
    public void init() {
        runtimeTenantResolver.forceTenant(getDefaultTenant());

        tenantSettingRepo.deleteAll();

        simulateApplicationReadyEvent();
        simulateApplicationStartedEvent();
    }

    @Test
    public void testSetting() throws EntityOperationForbiddenException, EntityInvalidException, EntityNotFoundException {
        List<String> contentTypesToSkip = settingService.getValue(WorkerManagerSetting.SKIP_CONTENT_TYPES.getName());
        Assert.assertTrue(contentTypesToSkip.isEmpty());
        tenantSettingService.update(WorkerManagerSetting.SKIP_CONTENT_TYPES.getName(), Arrays.asList("content1","content2"));
        contentTypesToSkip = settingService.getValue(WorkerManagerSetting.SKIP_CONTENT_TYPES.getName());
        Assert.assertTrue(contentTypesToSkip.contains("content1"));
        Assert.assertTrue(contentTypesToSkip.contains("content2"));
    }
}
