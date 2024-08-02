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

import fr.cnes.regards.framework.jpa.multitenant.event.spring.TenantConnectionReady;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.modules.tenant.settings.domain.DynamicTenantSetting;
import fr.cnes.regards.framework.modules.tenant.settings.service.AbstractSettingService;
import fr.cnes.regards.framework.modules.tenant.settings.service.IDynamicTenantSettingService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.modules.ingest.domain.settings.IngestSettings;
import org.apache.commons.lang.BooleanUtils;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * see {@link IIngestSettingsService}
 *
 * @author Iliana Ghazali
 */

@Service
@MultitenantTransactional
@Scope(proxyMode = ScopedProxyMode.TARGET_CLASS)
public class IngestSettingsService extends AbstractSettingService implements IIngestSettingsService {

    private final ITenantResolver tenantsResolver;

    private final IRuntimeTenantResolver runtimeTenantResolver;

    private IngestSettingsService self;

    public IngestSettingsService(IDynamicTenantSettingService dynamicTenantSettingService,
                                 ITenantResolver tenantsResolver,
                                 IRuntimeTenantResolver runtimeTenantResolver,
                                 IngestSettingsService ingestSettingsService) {
        super(dynamicTenantSettingService);
        this.tenantsResolver = tenantsResolver;
        this.runtimeTenantResolver = runtimeTenantResolver;
        this.self = ingestSettingsService;
    }

    @EventListener
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void onApplicationStartedEvent(ApplicationStartedEvent applicationStartedEvent) throws EntityException {
        //for each tenant try to create notification settings, if it do not exists then create with default value
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
    public boolean isActiveNotification() {
        return BooleanUtils.isTrue(getValue(IngestSettings.ACTIVE_NOTIFICATION));
    }

    @Override
    public void setActiveNotification(Boolean isActiveNotification) throws EntityException {
        dynamicTenantSettingService.update(IngestSettings.ACTIVE_NOTIFICATION, isActiveNotification);
    }

    @Override
    public void setSipBodyTimeToLive(int sipBodyTimeToLive) throws EntityException {
        dynamicTenantSettingService.update(IngestSettings.SIP_BODY_TIME_TO_LIVE, sipBodyTimeToLive);
    }

    @Override
    public int getSipBodyTimeToLive() {
        return getValue(IngestSettings.SIP_BODY_TIME_TO_LIVE);
    }

    @Override
    protected List<DynamicTenantSetting> getSettingList() {
        return IngestSettings.SETTING_LIST;
    }

}
