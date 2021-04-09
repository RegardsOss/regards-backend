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

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.jpa.multitenant.lock.LockingTaskExecutors;
import fr.cnes.regards.framework.modules.jobs.domain.AbstractJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import fr.cnes.regards.modules.storage.domain.database.FileReference;
import fr.cnes.regards.modules.storage.domain.database.request.FileDeletionRequest;
import fr.cnes.regards.modules.storage.domain.dto.request.FileDeletionRequestDTO;
import fr.cnes.regards.modules.storage.domain.flow.DeletionFlowItem;
import fr.cnes.regards.modules.storage.service.file.FileReferenceService;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockingTaskExecutor.Task;

/**
 * JOB to handle deletion requests on many {@link FileReference}s.<br>
 * This jobs requests database to retrieve {@link FileReference}s with search criterion and for each, send a {@link DeletionFlowItem} event.<br>
 * Events can be handled by the first available storage microservice to create associated {@link FileDeletionRequest}.<br>
 * NOTE : Be careful that the {@link #run()} stays not transactional.
 *
 * @author Sébastien Binda
 *
 */
public class FileDeletionRequestsCreatorJob extends AbstractJob<Void> {

    public static final Logger LOGGER = LoggerFactory.getLogger(FileDeletionRequestsCreatorJob.class);

    public static final String STORAGE_LOCATION_ID = "storage";

    public static final String FORCE_DELETE = "force";

    @Autowired
    private IPublisher publisher;

    @Autowired
    private FileReferenceService fileRefService;

    @Autowired
    private LockingTaskExecutors lockingTaskExecutors;

    /**
     * The job parameters as a map
     */
    protected Map<String, JobParameter> parameters;

    @Override
    public void setParameters(Map<String, JobParameter> parameters)
            throws JobParameterMissingException, JobParameterInvalidException {
        this.parameters = parameters;
    }

    /**
     * Task to execute if lock acquired to publish {@link DeletionFlowItem} to initialize new deletion requests
     */
    private final Task publishdeletionFlowItemsTask = () -> {
        String storage = parameters.get(STORAGE_LOCATION_ID).getValue();
        Boolean forceDelete = parameters.get(FORCE_DELETE).getValue();
        Pageable pageRequest = PageRequest.of(0, DeletionFlowItem.MAX_REQUEST_PER_GROUP);
        Page<FileReference> pageResults;
        long start = System.currentTimeMillis();
        logger.info("[DELETION JOB] Calculate all files to delete for storage location {} (forceDelete={})", storage,
                    forceDelete);
        String requestGroupId = String.format("DELETION-%s", UUID.randomUUID().toString());
        Set<FileDeletionRequestDTO> deletionRequests = Sets.newHashSet();
        do {
            // Search for all file references of the given storage location
            pageResults = fileRefService.search(storage, pageRequest);
            for (FileReference fileRef : pageResults.getContent()) {
                for (String owner : fileRef.getOwners()) {
                    deletionRequests.add(FileDeletionRequestDTO.build(fileRef.getMetaInfo().getChecksum(), storage,
                                                                      owner, forceDelete));
                    if (deletionRequests.size() == DeletionFlowItem.MAX_REQUEST_PER_GROUP) {
                        publisher.publish(DeletionFlowItem.build(deletionRequests, requestGroupId));
                        deletionRequests.clear();
                        requestGroupId = String.format("DELETION-%s", UUID.randomUUID().toString());
                    }
                }
            }
            pageRequest = pageRequest.next();
        } while (pageResults.hasNext());
        if (!deletionRequests.isEmpty()) {
            publisher.publish(DeletionFlowItem.build(deletionRequests, requestGroupId));
        }
        logger.info("[DELETION JOB] {} files to delete for storage location {} calculated in {}ms",
                    pageResults.getTotalElements(), storage, System.currentTimeMillis() - start);
    };

    @Override
    public void run() {
        try {
            lockingTaskExecutors.executeWithLock(publishdeletionFlowItemsTask, new LockConfiguration(
                    DeletionFlowItem.DELETION_LOCK, Instant.now().plusSeconds(300)));
        } catch (Throwable e) {
            LOGGER.error("[COPY JOB] Unable to get a lock for copy process. Copy job canceled");
            LOGGER.error(e.getMessage(), e);
        }
    }

}
