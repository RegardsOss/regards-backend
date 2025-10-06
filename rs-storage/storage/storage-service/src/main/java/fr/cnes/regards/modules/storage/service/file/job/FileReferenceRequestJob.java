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
package fr.cnes.regards.modules.storage.service.file.job;

import fr.cnes.regards.framework.modules.jobs.domain.AbstractJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.modules.fileaccess.plugin.domain.FileStorageWorkingSubset;
import fr.cnes.regards.modules.storage.domain.database.request.FileReferenceRequestAggregation;
import fr.cnes.regards.modules.storage.domain.database.request.FileStorageRequestAggregation;
import fr.cnes.regards.modules.storage.service.file.request.FileReferenceRequestService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Job for processing a bundle of file references request {@link FileReferenceRequestAggregation}s. <br/>
 * This job is scheduled to try and create FileReferences
 * from a bundle of request all of them are from the same storage.<br/>
 *
 * @author Olivier Navarro
 */
public class FileReferenceRequestJob extends AbstractJob<Void> {

    /**
     * JOB Parameter key for the Working subset of {@link FileStorageRequestAggregation} to handle for storage.
     */
    public static final String WORKING_SUBSET = "wss";

    @Autowired
    private FileReferenceRequestService fileReferenceReqService;

    @Autowired
    public MeterRegistry meterRegistry;

    private Timer myTimer;

    public Timer getTimer() {
        if (myTimer == null) {
            myTimer = Timer.builder("file_reference_request_job")
                           .description("FileReferenceRequestJob#run")
                           .publishPercentileHistogram()
                           .register(meterRegistry);
        }
        return myTimer;
    }

    private FileStorageWorkingSubset workingSubset;

    private int nbRequestToHandle = 0;

    @Override
    public void setParameters(Map<String, JobParameter> parameters) {
        workingSubset = parameters.get(WORKING_SUBSET).getValue();
        nbRequestToHandle = workingSubset.getFileReferenceRequests().size();
    }

    @Override
    public void run() {
        final long start = System.currentTimeMillis();

        logger.debug("[FILE REFERENCE REQUEST JOB] Running job for {} file reference requests", nbRequestToHandle);
        fileReferenceReqService.tryAndReference(workingSubset.getFileReferenceRequests());

        final long durationInMillis = System.currentTimeMillis() - start;
        getTimer().record(durationInMillis, TimeUnit.MILLISECONDS);
        logger.debug("[FILE REFERENCE REQUEST JOB]  Job handled for {} file reference requests in {}ms",
                     nbRequestToHandle,
                     durationInMillis);
    }

    @Override
    public int getCompletionCount() {
        return nbRequestToHandle > 0 ? nbRequestToHandle : super.getCompletionCount();
    }

}
