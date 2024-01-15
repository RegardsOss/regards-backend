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
import fr.cnes.regards.framework.utils.RsRuntimeException;
import fr.cnes.regards.framework.utils.plugins.exception.NotAvailablePluginConfigurationException;
import fr.cnes.regards.modules.fileaccess.plugin.domain.IStorageLocation;
import fr.cnes.regards.modules.storage.domain.database.FileReference;
import fr.cnes.regards.modules.storage.service.file.FileReferenceService;
import fr.cnes.regards.modules.storage.service.glacier.GlacierArchiveService;
import fr.cnes.regards.modules.storage.service.location.StorageLocationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Storage Job to trigger periodic actions on a given store location.
 * Some storage location (aka plugin) needs to do some asynchronous action(s) at periodic times.
 * This job allow each storage plugin to define a periodic action to do.
 *
 * @author Sébastien Binda
 */
public class PeriodicStorageLocationJob extends AbstractJob<Void> {

    public static final String DATA_STORAGE_CONF_BUSINESS_ID = "storage";

    private String storageLocation;

    @Autowired
    private IPluginService pluginService;

    @Autowired
    private StorageLocationService storageLocationService;

    @Autowired
    private FileReferenceService fileRefService;

    @Autowired
    private GlacierArchiveService glacierArchiveService;

    @Value("${regards.storage.check.periodic.action.enabled:true}")
    private boolean checkPeriodicActionEnabled;

    @Override
    public void run() {
        try {
            logger.debug("[PERIODIC STORAGE LOCATION JOB] Running job for location {}", storageLocation);
            long startTime = System.currentTimeMillis();
            IStorageLocation storagePlugin = pluginService.getPlugin(storageLocation);
            if (storagePlugin.hasPeriodicAction()) {
                PeriodicActionProgressManager progressManager = new PeriodicActionProgressManager(fileRefService,
                                                                                                  storageLocationService,
                                                                                                  glacierArchiveService);
                storagePlugin.runPeriodicAction(progressManager);
                // Save remaining pending action status for run step.
                progressManager.bulkSavePendings();
                if (checkPeriodicActionEnabled) {
                    storagePlugin.runCheckPendingAction(progressManager,
                                                        fileRefService.searchPendingActionsRemaining(storageLocation)
                                                                      .stream()
                                                                      .map(FileReference::toDtoWithoutOwners)
                                                                      .collect(Collectors.toSet()));
                    // Save remaining pending action status for check step.
                    progressManager.bulkSavePendings();
                }
                // Notify errors for both steps.
                progressManager.notifyPendingActionErrors();
                logger.info("[PERIODIC STORAGE LOCATION JOB] Periodic task on storage {} done in {}ms",
                            storageLocation,
                            System.currentTimeMillis() - startTime);
            } else {
                logger.debug("[PERIODIC STORAGE LOCATION JOB] No periodic location defined for storage {}",
                             storageLocation);
            }
        } catch (ModuleException | NotAvailablePluginConfigurationException e) {
            logger.error(e.getMessage(), e);
            throw new RsRuntimeException(e);
        }
    }

    @Override
    public void setParameters(Map<String, JobParameter> parameters)
        throws JobParameterMissingException, JobParameterInvalidException {
        storageLocation = parameters.get(DATA_STORAGE_CONF_BUSINESS_ID).getValue();
    }
}
