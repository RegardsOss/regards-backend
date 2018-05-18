/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.storage.service.job;

import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.Optional;

import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobWorkspaceException;
import fr.cnes.regards.modules.storage.domain.database.StorageDataFile;
import fr.cnes.regards.modules.storage.domain.plugin.IWorkingSubset;

/**
 * This job is executed by JobService while its scheduling is handled by an IAIPService. This means that the job context is prepared by an IAIPService.
 *
 * This job aims to storeAndCreate the metadata of an AIP.
 *
 * @author Sylvain Vissiere-Guerinet
 *
 */
public class StoreMetadataFilesJob extends AbstractStoreFilesJob {

    @Override
    protected void handleWorkspaceException(IOException e) throws JobWorkspaceException {
        Long storageConfId = parameters.get(PLUGIN_TO_USE_PARAMETER_NAME).getValue();
        StorageJobProgressManager progressManager = new StorageJobProgressManager(publisher, this, storageConfId,
                runtimeTenantResolver);
        IWorkingSubset workingSubset = parameters.get(WORKING_SUB_SET_PARAMETER_NAME).getValue();
        workingSubset.getDataFiles()
                .forEach(file -> progressManager.storageFailed(file, Optional.empty(), e.toString()));
        super.handleWorkspaceException(e);
    }

    @Override
    protected void doRun(Map<String, JobParameter> parameterMap) {
        storeFile(parameterMap, true);
    }

    @Override
    protected void handleNotHandledDataFile(StorageDataFile notHandled, Optional<URL> notHandledUrl) {
        progressManager.storageFailed(notHandled, notHandledUrl, NOT_HANDLED_MSG);
    }

}
