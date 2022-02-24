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
package fr.cnes.regards.modules.storage.service.file;

import fr.cnes.regards.modules.storage.service.DownloadTokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;

/**
 * Scheduler to periodicly delete expired download tokens.
 *
 * @author Sébastien Binda
 *
 */
@Component
@Profile("!noscheduler")
@EnableScheduling
public class PurgeDownloadTokenScheduler {

    private static final String DEFAULT_INITIAL_DELAY = "60000";

    private static final String DEFAULT_DELAY = "7200000";

    @Autowired
    private DownloadTokenService downloadTokenService;

    @Autowired
    private ITenantResolver tenantResolver;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Scheduled(fixedDelayString = "${regards.storage.purge.schedule.delay:" + DEFAULT_DELAY + "}",
            initialDelayString = "${regards.storage.purge.schedule.delay:" + DEFAULT_INITIAL_DELAY + "}")
    public void handleFileStorageRequests() throws ModuleException {
        for (String tenant : tenantResolver.getAllActiveTenants()) {
            runtimeTenantResolver.forceTenant(tenant);
            try {
                downloadTokenService.purgeTokens();
            } finally {
                runtimeTenantResolver.clearTenant();
            }
        }
    }

}
