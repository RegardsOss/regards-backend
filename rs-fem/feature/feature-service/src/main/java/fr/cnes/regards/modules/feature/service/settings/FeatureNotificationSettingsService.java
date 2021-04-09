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


package fr.cnes.regards.modules.feature.service.settings;

import fr.cnes.regards.framework.jpa.multitenant.event.spring.TenantConnectionReady;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.modules.tenant.settings.domain.DynamicTenantSetting;
import fr.cnes.regards.framework.modules.tenant.settings.service.AbstractSettingService;
import fr.cnes.regards.framework.modules.tenant.settings.service.IDynamicTenantSettingService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.modules.feature.domain.settings.FeatureNotificationSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * see {@link IFeatureNotificationSettingsService}
 *
 * @author Iliana Ghazali
 */

@Service
@MultitenantTransactional
public class FeatureNotificationSettingsService extends AbstractSettingService implements IFeatureNotificationSettingsService {

    private static final Logger LOG = LoggerFactory.getLogger(FeatureNotificationSettingsService.class);

    private ITenantResolver tenantsResolver;
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private FeatureNotificationSettingsService self;

    public FeatureNotificationSettingsService(IDynamicTenantSettingService dynamicTenantSettingService,
                                              ITenantResolver tenantsResolver,
                                              IRuntimeTenantResolver runtimeTenantResolver
    ) {
        super(dynamicTenantSettingService);
        this.tenantsResolver = tenantsResolver;
        this.runtimeTenantResolver = runtimeTenantResolver;
    }

    @EventListener
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void onApplicationStartedEvent(ApplicationStartedEvent applicationStartedEvent) throws EntityException {
        for (String tenant : tenantsResolver.getAllActiveTenants()) {
            runtimeTenantResolver.forceTenant(tenant);
            try {
                self.init();
            } finally {
                runtimeTenantResolver.clearTenant();
            }
        }
    }

    @EventListener
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void onTenantConnectionReady(TenantConnectionReady event) throws EntityException {
        runtimeTenantResolver.forceTenant(event.getTenant());
        try {
            self.init();
        } finally {
            runtimeTenantResolver.clearTenant();
        }
    }

    @Override
    public List<DynamicTenantSetting> retrieve() {
        return dynamicTenantSettingService.readAll();
    }

    @Override
    public void update(DynamicTenantSetting dynamicTenantSetting) {
        try {
            dynamicTenantSettingService.update(dynamicTenantSetting.getName(), dynamicTenantSetting.getValue());
        } catch (EntityException e) {
            LOG.error("Unable to update setting {}", dynamicTenantSetting.getName());
        }
    }

    @Override
    public void resetSettings() throws EntityException {
        deleteAll();
        init();
    }

    @Override
    public boolean isActiveNotification() {
        Boolean isActiveNotification = getValue(FeatureNotificationSettings.ACTIVE_NOTIFICATION);
        return isActiveNotification != null && isActiveNotification;
    }

    @Override
    protected List<DynamicTenantSetting> getSettingList() {
        return FeatureNotificationSettings.SETTING_LIST;
    }

}
