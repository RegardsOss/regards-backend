/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.file.packager.service.job;

import fr.cnes.regards.framework.modules.jobs.domain.AbstractJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import fr.cnes.regards.modules.file.packager.service.FilePackagerService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

/**
 * Job creating the physical archive containing the
 * {@link fr.cnes.regards.modules.file.packager.domain.FileInBuildingPackage FileInBuildingPackage} of a
 * {@link fr.cnes.regards.modules.file.packager.domain.PackageReference PackageRererence}. The job will update the
 * package status and checksum if it succeeds and send an event to file-access to request the storage of the archive.
 *
 * @author Thibaud Michaudel
 **/
public class StoreCompletePackageJob extends AbstractJob<Void> {

    public static final String PACKAGE_ID_PARAMETER = "packageId";

    public static final String CREATION_DATE_PARAMETER = "creationDate"; // format yyyyMMddHHmmssSSS

    public static final String STORAGE_SUBDIRECTORY_PARAMETER = "storageSubdirectory";

    public static final String STORAGE_PARAMETER = "storage";

    private Long packageId;

    private String creationDate;

    private String storageSubdirectory;

    private String storage;

    @Autowired
    private FilePackagerService filePackagerService;

    @Override
    public void setParameters(Map<String, JobParameter> parameters)
        throws JobParameterMissingException, JobParameterInvalidException {
        packageId = getValue(parameters, PACKAGE_ID_PARAMETER);
        creationDate = getValue(parameters, CREATION_DATE_PARAMETER);
        storageSubdirectory = getValue(parameters, STORAGE_SUBDIRECTORY_PARAMETER);
        storage = getValue(parameters, STORAGE_PARAMETER);
    }

    @Override
    public void run() {
        long start = System.currentTimeMillis();
        logger.debug("[STORE COMPLETE PACKAGE JOB] Start StoreCompletePackageJob for package {}", packageId);
        filePackagerService.storeCompletePackage(packageId, storageSubdirectory, creationDate, storage);
        logger.debug("[STORE COMPLETE PACKAGE JOB] End StoreCompletePackageJob for package {} after {} ms",
                     packageId,
                     System.currentTimeMillis() - start);
    }
}
