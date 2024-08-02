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


package fr.cnes.regards.framework.modules.dump.service.settings;

import fr.cnes.regards.framework.jpa.multitenant.event.spring.TenantConnectionReady;
import fr.cnes.regards.framework.jpa.utils.RegardsTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.modules.dump.domain.DumpParameters;
import fr.cnes.regards.framework.modules.dump.domain.DumpSettings;
import fr.cnes.regards.framework.modules.tenant.settings.domain.DynamicTenantSetting;
import fr.cnes.regards.framework.modules.tenant.settings.service.AbstractSettingService;
import fr.cnes.regards.framework.modules.tenant.settings.service.IDynamicTenantSettingService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RegardsTransactional
@Scope(proxyMode = ScopedProxyMode.TARGET_CLASS)
public class DumpSettingsService extends AbstractSettingService implements IDumpSettingsService {

    private final ITenantResolver tenantsResolver;

    private final IRuntimeTenantResolver runtimeTenantResolver;

    private DumpSettingsService self;

    protected DumpSettingsService(IDynamicTenantSettingService dynamicTenantSettingService,
                                  ITenantResolver tenantsResolver,
                                  IRuntimeTenantResolver runtimeTenantResolver,
                                  DumpSettingsService dumpSettingsService) {
        super(dynamicTenantSettingService);
        this.tenantsResolver = tenantsResolver;
        this.runtimeTenantResolver = runtimeTenantResolver;
        this.self = dumpSettingsService;
    }

    @EventListener
    @Order(0)
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
    @Order(0)
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
    public void resetLastDumpDate() throws EntityException {
        setLastDumpReqDate(null);
    }

    @Override
    public DumpParameters getDumpParameters() {
        return getValue(DumpSettings.DUMP_PARAMETERS);
    }

    @Override
    public void setDumpParameters(DumpParameters dumpParameters) throws EntityException {
        dynamicTenantSettingService.update(DumpSettings.DUMP_PARAMETERS, dumpParameters);
    }

    @Override
    public OffsetDateTime lastDumpReqDate() {
        return getValue(DumpSettings.LAST_DUMP_REQ_DATE);
    }

    @Override
    public void setLastDumpReqDate(OffsetDateTime lastDumpReqDate) throws EntityException {
        dynamicTenantSettingService.update(DumpSettings.LAST_DUMP_REQ_DATE, lastDumpReqDate);
    }

    @Override
    protected List<DynamicTenantSetting> getSettingList() {
        return DumpSettings.SETTING_LIST;
    }

}
