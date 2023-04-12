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
package fr.cnes.regards.modules.acquisition.service;

import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

/**
 * Scheduled actions to process new CREATED SIPS by sending bulk request to Ingest
 *
 * @author Christophe Mertz
 * @author Marc Sordi
 */
@Component
@Profile("!disableDataProviderTask")
@EnableScheduling
public class ScheduledDataProviderTasks {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScheduledDataProviderTasks.class);

    @Autowired
    private ITenantResolver tenantResolver;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private IAcquisitionProcessingService chainService;

    private OffsetDateTime lastCheckDate = OffsetDateTime.now();

    // Run every minutes
    @Scheduled(cron = "0 * * * * *")
    public void processAcquisitionChains() {
        LOGGER.info("Checking for active chains to run.");
        OffsetDateTime now = OffsetDateTime.now();
        for (String tenant : tenantResolver.getAllActiveTenants()) {
            try {
                runtimeTenantResolver.forceTenant(tenant);
                chainService.startAutomaticChains(lastCheckDate, now);
            } finally {
                runtimeTenantResolver.clearTenant();
            }
        }
        lastCheckDate = now;
    }
}
