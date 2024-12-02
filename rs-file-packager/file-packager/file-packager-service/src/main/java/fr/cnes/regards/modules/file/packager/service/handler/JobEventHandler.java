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
package fr.cnes.regards.modules.file.packager.service.handler;

import com.google.gson.reflect.TypeToken;
import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.batch.IBatchHandler;
import fr.cnes.regards.framework.modules.jobs.domain.IJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.event.JobEvent;
import fr.cnes.regards.framework.modules.jobs.domain.event.JobEventType;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.modules.file.packager.service.FilePackagerService;
import fr.cnes.regards.modules.file.packager.service.job.DeleteLocalFilesJob;
import fr.cnes.regards.modules.file.packager.service.job.FileIdAndPath;
import fr.cnes.regards.modules.file.packager.service.job.StoreCompletePackageJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * JobEventHandler for file-packager.
 * Handle failure or abort of {@link DeleteLocalFilesJob} & {@link StoreCompletePackageJob}
 *
 * @author Thibaud Michaudel
 **/
@Component
public class JobEventHandler implements ApplicationListener<ApplicationReadyEvent>, IBatchHandler<JobEvent> {

    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(JobEventHandler.class);

    private final ISubscriber subscriber;

    private final IJobInfoService jobInfoService;

    private final FilePackagerService filePackagerService;

    public JobEventHandler(ISubscriber subscriber,
                           IJobInfoService jobInfoService,
                           FilePackagerService filePackagerService) {
        this.subscriber = subscriber;
        this.jobInfoService = jobInfoService;
        this.filePackagerService = filePackagerService;
    }

    @Override
    public Errors validate(JobEvent message) {
        return null;
    }

    @Override
    public void handleBatch(List<JobEvent> messages) {
        long start = System.currentTimeMillis();
        LOGGER.debug("[FILE-PACKAGER JOB EVENT HANDLER] Handling {} JobEvents...", messages.size());
        long nbJobError = 0;
        for (JobEvent jobEvent : messages) {
            if (jobEvent.getJobEventType() == JobEventType.FAILED
                || jobEvent.getJobEventType() == JobEventType.ABORTED) {
                JobInfo jobInfo = jobInfoService.retrieveJob(jobEvent.getJobId());
                // Keep in mind a single request that fail does not mean the job will have the FAILED state
                // we receive here events when the job raises an exception on boot / end (issue with params, plugin init ...)
                // so all requests are dead
                if (jobInfo.getClassName().equals(DeleteLocalFilesJob.class.getName())) {
                    // On DeleteLocalFilesJob crash or abort, set the files back to TO_LOCAL_DELETE_STATUS, so they
                    // can be deleted by another job
                    try {
                        List<FileIdAndPath> fileIdAndPathList = IJob.getValue(jobInfo.getParametersAsMap(),
                                                                              DeleteLocalFilesJob.FILES_ID_AND_PATH_PARAMETER,
                                                                              new TypeToken<List<FileIdAndPath>>() {

                                                                              }.getType());

                        filePackagerService.retryFileDeletion(fileIdAndPathList.stream()
                                                                               .map(FileIdAndPath::fileId)
                                                                               .toList());
                    } catch (JobParameterMissingException | JobParameterInvalidException e) {
                        LOGGER.error("Error while retrieving aborted deleting file ids", e);
                    }
                    nbJobError++;
                } else if (jobInfo.getClassName().equals(StoreCompletePackageJob.class.getName())) {
                    // On StoreCompletePackageJob crash or abort, try to delete the archive (the job might have been
                    // aborted after of before its creation) and set the package to STORE_ERROR
                    Map<String, JobParameter> parameters = jobInfo.getParametersAsMap();
                    Long packageId = parameters.get(StoreCompletePackageJob.PACKAGE_ID_PARAMETER).getValue();
                    String date = parameters.get(StoreCompletePackageJob.CREATION_DATE_PARAMETER).getValue();
                    String storageSubdirectory = parameters.get(StoreCompletePackageJob.STORAGE_SUBDIRECTORY_PARAMETER)
                                                           .getValue();

                    Path archivePath = filePackagerService.getArchivePath(storageSubdirectory, date);

                    filePackagerService.setPackageError(packageId, "The job has been aborted or crashed");
                    try {
                        Files.deleteIfExists(archivePath);
                    } catch (Exception e) { //NOSONAR no exception allowed here
                        LOGGER.error("Error while deleting malformed archive : {}", archivePath.toString(), e);
                    }
                    nbJobError++;
                }
            }
        }
        LOGGER.debug("[FILE-PACKAGER JOB EVENT HANDLER] {} JobEvents in error handled in {} ms",
                     nbJobError,
                     System.currentTimeMillis() - start);
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        subscriber.subscribeTo(JobEvent.class, this);
    }

    @Override
    public boolean isDedicatedDLQEnabled() {
        return false;
    }
}
