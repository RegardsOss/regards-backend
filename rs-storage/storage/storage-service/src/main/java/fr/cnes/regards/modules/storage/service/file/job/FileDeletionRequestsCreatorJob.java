/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * JOB to handle deletion requests on many {@link FileReference}s.<br>
 * This jobs requests database to retrieve {@link FileReference}s with search criterion and for each, send a {@link DeletionFlowItem} event.<br>
 * Events can be handled by the first available storage microservice to create associated {@link FileDeletionRequest}.<br>
 * NOTE : Be careful that the {@link #run()} stays not transactional.
 *
 * @author Sébastien Binda
 */
public class FileDeletionRequestsCreatorJob extends AbstractJob<Void> {

    public static final String STORAGE_LOCATION_ID = "storage";

    public static final String FORCE_DELETE = "force";

    public static final String SESSION_OWNER = "sessionOwner";

    public static final String SESSION = "session";

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
        // init values
        String storage = parameters.get(STORAGE_LOCATION_ID).getValue();
        Boolean forceDelete = parameters.get(FORCE_DELETE).getValue();
        String sessionOwner = parameters.get(SESSION_OWNER).getValue();
        String session = parameters.get(SESSION).getValue();
        Pageable pageRequest = PageRequest.of(0, DeletionFlowItem.MAX_REQUEST_PER_GROUP);
        Page<FileReference> pageResults;
        long start = System.currentTimeMillis();
        logger.info("[DELETION JOB] Calculate all files to delete for storage location {} (forceDelete={})",
                    storage,
                    forceDelete);
        String requestGroupId = String.format("DELETION-%s", UUID.randomUUID().toString());
        Set<FileDeletionRequestDTO> deletionRequests = Sets.newHashSet();

        // Search for all file references of the given storage location
        do {
            // Search for all file references of the given storage location
            pageResults = fileRefService.searchWithOwners(storage, pageRequest);
            for (FileReference fileRef : pageResults.getContent()) {
                for (String owner : fileRef.getLazzyOwners()) {
                    deletionRequests.add(FileDeletionRequestDTO.build(fileRef.getMetaInfo().getChecksum(),
                                                                      storage,
                                                                      owner,
                                                                      sessionOwner,
                                                                      session,
                                                                      forceDelete));
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
                    pageResults.getTotalElements(),
                    storage,
                    System.currentTimeMillis() - start);
    };

    @Override
    public void run() {
        try {
            lockingTaskExecutors.executeWithLock(publishdeletionFlowItemsTask,
                                                 new LockConfiguration(DeletionFlowItem.DELETION_LOCK,
                                                                       Instant.now().plusSeconds(300)));
        } catch (Throwable e) {
            logger.error("[COPY JOB] Unable to get a lock for copy process. Copy job canceled");
            logger.error(e.getMessage(), e);
        }
    }

}
