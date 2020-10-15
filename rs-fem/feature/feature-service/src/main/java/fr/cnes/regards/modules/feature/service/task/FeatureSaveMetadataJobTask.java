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


package fr.cnes.regards.modules.feature.service.task;

import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.feature.service.dump.FeatureSaveMetadataService;
import fr.cnes.regards.modules.feature.service.job.FeatureSaveMetadataJob;

/**
 * Task to schedule {@link FeatureSaveMetadataJob} through {@link FeatureSaveMetadataService}
 * @author Iliana Ghazali
 */

public class FeatureSaveMetadataJobTask implements Runnable {

    private FeatureSaveMetadataService featureSaveMetadataService;

    private String tenant;

    private IRuntimeTenantResolver runtimeTenantResolver;

    public FeatureSaveMetadataJobTask(FeatureSaveMetadataService featureSaveMetadataService, String tenant,
            IRuntimeTenantResolver runtimeTenantResolver) {
        this.featureSaveMetadataService = featureSaveMetadataService;
        this.tenant = tenant;
        this.runtimeTenantResolver = runtimeTenantResolver;
    }

    @Override
    public void run() {
        runtimeTenantResolver.forceTenant(tenant);
        try {
            featureSaveMetadataService.scheduleJobs();
        } finally {
            runtimeTenantResolver.clearTenant();
        }
    }
}
