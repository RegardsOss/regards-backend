/*
 * Copyright 2017-2025 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import com.google.gson.reflect.TypeToken;
import fr.cnes.regards.framework.modules.jobs.domain.AbstractJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import fr.cnes.regards.modules.file.packager.service.FilePackagerService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

/**
 * Job deleting the files at the paths given as parameter {@link FileIdAndPath#path()}.
 * If the file is successfully deleted or didn't exist, delete the {@link fr.cnes.regards.modules.file.packager.domain.FileInBuildingPackage} with the
 * corresponding {@link FileIdAndPath#fileId()}. Otherwise, set the {@link fr.cnes.regards.modules.file.packager.domain.FileInBuildingPackage} to
 * {@link fr.cnes.regards.modules.file.packager.domain.FileInBuildingPackageStatus#DELETION_ERROR}
 *
 * @author Thibaud Michaudel
 **/
public class DeleteLocalFilesJob extends AbstractJob<Void> {

    public static final String FILES_ID_AND_PATH_PARAMETER = "filesIdAndPath";

    private List<FileIdAndPath> filesIdAndPath;

    @Autowired
    private FilePackagerService filePackagerService;

    @Override
    public void setParameters(Map<String, JobParameter> parameters)
        throws JobParameterMissingException, JobParameterInvalidException {
        filesIdAndPath = getValue(parameters, FILES_ID_AND_PATH_PARAMETER, new TypeToken<List<FileIdAndPath>>() {

        }.getType());
    }

    @Override
    public void run() {
        long start = System.currentTimeMillis();
        logger.debug("[DELETE LOCAL FILES JOB] Start DeleteLocalFilesJob for {} files", filesIdAndPath.size());
        filePackagerService.deleteLocalFiles(filesIdAndPath);
        logger.debug("[DELETE LOCAL FILES JOB] End DeleteLocalFilesJob for {} files after {} ms",
                     filesIdAndPath.size(),
                     System.currentTimeMillis() - start);
    }
}
