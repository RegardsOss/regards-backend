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
package fr.cnes.regards.modules.accessrights.service.projectuser;

import fr.cnes.regards.framework.jpa.multitenant.event.spring.TenantConnectionReady;
import fr.cnes.regards.framework.jpa.utils.RegardsTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.modules.tenant.settings.domain.DynamicTenantSetting;
import fr.cnes.regards.framework.modules.tenant.settings.service.AbstractSettingService;
import fr.cnes.regards.framework.modules.tenant.settings.service.IDynamicTenantSettingService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.modules.accessrights.domain.projects.AccessSettings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RegardsTransactional
public class AccessSettingsService extends AbstractSettingService {

    @Autowired
    private AccessSettingsService self;

    private final ITenantResolver tenantsResolver;
    private final IRuntimeTenantResolver runtimeTenantResolver;

    public AccessSettingsService(IDynamicTenantSettingService dynamicTenantSettingService, ITenantResolver tenantsResolver,
                                 IRuntimeTenantResolver runtimeTenantResolver
    ) {
        super(dynamicTenantSettingService);
        this.tenantsResolver = tenantsResolver;
        this.runtimeTenantResolver = runtimeTenantResolver;
    }

    @Override
    protected List<DynamicTenantSetting> getSettingList() {
        return AccessSettings.SETTING_LIST;
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

    /**
     * {@link Order} : Initialization needs to be done after RoleService initialization from {@link fr.cnes.regards.modules.accessrights.service.role.RoleEventListener}
     * @param event
     * @throws EntityException
     */
    @EventListener
    @Order(10)
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void onTenantConnectionReady(TenantConnectionReady event) throws EntityException {
        runtimeTenantResolver.forceTenant(event.getTenant());
        try {
            self.init();
        } finally {
            runtimeTenantResolver.clearTenant();
        }
    }

    public String defaultRole() {
        return getValue(AccessSettings.DEFAULT_ROLE);
    }

    public List<String> defaultGroups() {
        return getValue(AccessSettings.DEFAULT_GROUPS);
    }

    public boolean isAutoAccept() {
        return AccessSettings.AcceptanceMode.AUTO_ACCEPT.equals(AccessSettings.AcceptanceMode.fromName(getValue(AccessSettings.MODE)));
    }

    public Set<String> userCreationMailRecipients() {
        return getValue(AccessSettings.USER_CREATION_MAIL_RECIPIENTS);
    }

}
