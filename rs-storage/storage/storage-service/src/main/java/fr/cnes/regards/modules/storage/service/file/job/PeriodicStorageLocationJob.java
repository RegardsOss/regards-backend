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
package fr.cnes.regards.modules.storage.service.file.job;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.AbstractJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.utils.plugins.exception.NotAvailablePluginConfigurationException;
import fr.cnes.regards.modules.storage.domain.plugin.IStorageLocation;
import fr.cnes.regards.modules.storage.service.file.FileReferenceService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

public class PeriodicStorageLocationJob extends AbstractJob<Void> {

    public static final String DATA_STORAGE_CONF_BUSINESS_ID = "storage";

    private String storageLocation;

    @Autowired
    private IPluginService pluginService;

    @Autowired
    private FileReferenceService fileRefService;

    @Override
    public void run() {
        try {
            long startTime = System.currentTimeMillis();
            IStorageLocation storagePlugin = pluginService.getPlugin(storageLocation);
            PeriodicActionProgressManager progressManager = new PeriodicActionProgressManager(fileRefService);
            storagePlugin.runPeriodicAction(progressManager);
            progressManager.bulkSavePendings();
            progressManager.notifyPendingActionErrors();
            logger.info("Periodic task on storage {} done in {}ms",
                        storageLocation,
                        System.currentTimeMillis() - startTime);
        } catch (ModuleException e) {
            throw new RuntimeException(e);
        } catch (NotAvailablePluginConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setParameters(Map<String, JobParameter> parameters)
        throws JobParameterMissingException, JobParameterInvalidException {
        super.setParameters(parameters);
        storageLocation = parameters.get(DATA_STORAGE_CONF_BUSINESS_ID).getValue();
    }
}
