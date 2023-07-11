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
package fr.cnes.regards.modules.delivery.service.settings;

import fr.cnes.regards.framework.jpa.multitenant.event.spring.TenantConnectionReady;
import fr.cnes.regards.framework.jpa.utils.RegardsTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.framework.modules.tenant.settings.domain.DynamicTenantSetting;
import fr.cnes.regards.framework.modules.tenant.settings.service.AbstractSettingService;
import fr.cnes.regards.framework.modules.tenant.settings.service.IDynamicTenantSettingService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.modules.delivery.domain.settings.DeliverySettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * Setting service for {@link fr.cnes.regards.modules.delivery.domain.settings.DeliverySettings}
 *
 * @author Iliana Ghazali
 **/
@Service
@RegardsTransactional
public class DeliverySettingService extends AbstractSettingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeliverySettingService.class);

    private final ITenantResolver tenantsResolver;

    private final IRuntimeTenantResolver runtimeTenantResolver;

    public DeliverySettingService(IDynamicTenantSettingService dynamicTenantSettingService,
                                  ITenantResolver tenantsResolver,
                                  IRuntimeTenantResolver runtimeTenantResolver) {
        super(dynamicTenantSettingService);
        this.tenantsResolver = tenantsResolver;
        this.runtimeTenantResolver = runtimeTenantResolver;
    }

    @EventListener
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void onApplicationStartedEvent(ApplicationStartedEvent applicationStartedEvent) throws EntityException {
        for (String tenant : tenantsResolver.getAllActiveTenants()) {
            initSettings(tenant);
        }
    }

    @EventListener
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void onTenantConnectionReady(TenantConnectionReady event) throws EntityException {
        initSettings(event.getTenant());
    }

    private void initSettings(String tenant)
        throws EntityNotFoundException, EntityOperationForbiddenException, EntityInvalidException {
        runtimeTenantResolver.forceTenant(tenant);
        LOGGER.info("Initializing dynamic tenant settings for tenant {}", tenant);
        try {
            super.init();
        } finally {
            runtimeTenantResolver.clearTenant();
        }
        LOGGER.info("Dynamic tenant settings initialization done for tenant {}", tenant);
    }

    @Override
    protected List<DynamicTenantSetting> getSettingList() {
        return DeliverySettings.SETTING_LIST;
    }

    private Object getSettingByName(Set<DynamicTenantSetting> settings, String name) {
        return settings.stream()
                       .filter(setting -> setting.getName().equals(name))
                       .findFirst()
                       .map(DynamicTenantSetting::getValue)
                       .orElseThrow(() -> new IllegalArgumentException(String.format(
                           "Invalid delivery setting \"%s\". Check the validity of the following settings %s",
                           name,
                           settings)));
    }
}


