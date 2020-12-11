/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import fr.cnes.regards.framework.amqp.event.tenant.TenantCreatedEvent;
import fr.cnes.regards.framework.jpa.multitenant.event.spring.TenantConnectionReady;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.framework.utils.RsRuntimeException;
import fr.cnes.regards.modules.feature.dao.IFeatureNotificationSettingsRepository;
import fr.cnes.regards.modules.feature.domain.settings.FeatureNotificationSettings;

/**
 * see {@link IFeatureNotificationSettingsService}
 * @author Iliana Ghazali
 */

@Service
@MultitenantTransactional
public class FeatureNotificationSettingsService implements IFeatureNotificationSettingsService {

    @Autowired
    private IFeatureNotificationSettingsRepository notificationSettingsRepository;

    @Autowired
    private ITenantResolver tenantsResolver;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private IFeatureNotificationSettingsService self;

    @EventListener
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void onApplicationStartedEvent(ApplicationStartedEvent applicationStartedEvent) {
        //for each tenant try to create notification settings, if it do not exists then create with default value
        for(String tenant: tenantsResolver.getAllActiveTenants()) {
            runtimeTenantResolver.forceTenant(tenant);
            try {
                self.initNotificationSettings();
            } finally {
                runtimeTenantResolver.clearTenant();
            }
        }
    }

    @Override
    public void initNotificationSettings() {
        Optional<FeatureNotificationSettings> notificationSettingsOpt = notificationSettingsRepository.findFirstBy();
        if (!notificationSettingsOpt.isPresent()) {
            // init new settings
            notificationSettingsRepository.save(new FeatureNotificationSettings());
        }
    }

    @EventListener
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void onTenantConnectionReady(TenantConnectionReady event) {
        runtimeTenantResolver.forceTenant(event.getTenant());
        try {
            self.initNotificationSettings();
        } finally {
            runtimeTenantResolver.clearTenant();
        }
    }

    @Override
    public FeatureNotificationSettings retrieve() {
        return notificationSettingsRepository.findFirstBy().orElseThrow(() -> new RsRuntimeException(
                "Tenant has not been correctly initialized by system!! Go and shout on the devs!"));
    }

    @Override
    public void update(FeatureNotificationSettings featureNotificationSettings) {
        // SET ID (only one id is allowed for featureNotificationSettings)
        featureNotificationSettings.setId();

        // UPDATE SETTINGS if they already exist
        Optional<FeatureNotificationSettings> featSettingsOpt = notificationSettingsRepository.findById(featureNotificationSettings.getId());
        if (!featSettingsOpt.isPresent() || !featSettingsOpt.get().equals(featureNotificationSettings)) {
            notificationSettingsRepository.save(featureNotificationSettings);
        }
    }

    @Override
    public void resetSettings() {
        notificationSettingsRepository.deleteAll();
        initNotificationSettings();
    }
}
