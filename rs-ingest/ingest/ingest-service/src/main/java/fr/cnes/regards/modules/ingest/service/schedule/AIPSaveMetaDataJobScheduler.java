/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.ingest.service.schedule;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.modules.ingest.service.aip.AIPMetadataService;

/**
 * This component scans the AIPSaveMetaDataRepo and schedule jobs
 *
 * @author Leo Mieulet
 */
@Profile("!noschedule")
@Component
public class AIPSaveMetaDataJobScheduler {

    @Autowired
    private ITenantResolver tenantResolver;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private AIPMetadataService aipMetadataService;

    /**
     * Bulk save queued items every second.
     */
    @Scheduled(fixedDelayString = "${regards.aips.save-metadata.bulk.delay:10000}", initialDelay = 1_000)
    protected void schduleAIPSaveMetaDataJobs() {
        for (String tenant : tenantResolver.getAllActiveTenants()) {
            try {
                runtimeTenantResolver.forceTenant(tenant);
                boolean stop = false;
                do {
                    // Call transactional proxy
                    stop = !aipMetadataService.scheduleJobs();
                } while (!stop);
            } finally {
                runtimeTenantResolver.clearTenant();
            }
        }
    }
}
