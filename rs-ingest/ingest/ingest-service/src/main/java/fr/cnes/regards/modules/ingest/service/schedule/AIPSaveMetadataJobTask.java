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


package fr.cnes.regards.modules.ingest.service.schedule;

import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.ingest.service.dump.AIPSaveMetadataService;
import fr.cnes.regards.modules.ingest.service.job.AIPSaveMetadataJob;

/**
 * Task to schedule {@link AIPSaveMetadataJob} through {@link AIPSaveMetadataService}
 * @author Iliana Ghazali
 */
public class AIPSaveMetadataJobTask implements Runnable {

    private AIPSaveMetadataService aipSaveMetadataService;

    private String tenant;

    private IRuntimeTenantResolver runtimeTenantResolver;

    public AIPSaveMetadataJobTask(AIPSaveMetadataService aipSaveMetadataService, String tenant,
            IRuntimeTenantResolver runtimeTenantResolver) {
        this.aipSaveMetadataService = aipSaveMetadataService;
        this.tenant = tenant;
        this.runtimeTenantResolver = runtimeTenantResolver;
    }

    @Override
    public void run() {
        runtimeTenantResolver.forceTenant(tenant);
        try {
            aipSaveMetadataService.scheduleJobs();
        } finally {
            runtimeTenantResolver.clearTenant();
        }
    }
}
