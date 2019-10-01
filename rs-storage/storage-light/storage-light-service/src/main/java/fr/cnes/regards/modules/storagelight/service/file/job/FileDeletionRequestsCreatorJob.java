/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.storagelight.service.file.job;

import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.modules.jobs.domain.AbstractJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import fr.cnes.regards.modules.storagelight.domain.database.FileReference;
import fr.cnes.regards.modules.storagelight.domain.database.request.FileDeletionRequest;
import fr.cnes.regards.modules.storagelight.domain.dto.request.FileDeletionRequestDTO;
import fr.cnes.regards.modules.storagelight.domain.flow.DeletionFlowItem;
import fr.cnes.regards.modules.storagelight.service.file.FileReferenceService;
import fr.cnes.regards.modules.storagelight.service.file.request.FileDeletionRequestService;

/**
 * JOB to handle deletion requests on many {@link FileReference}s.<br>
 * This jobs requests database to retrieve {@link FileReference}s with search criterion and for each, send a {@link DeletionFlowItem} event.<br>
 * Events can be handled by the first available storage microservice to create associated {@link FileDeletionRequest}.<br>
 * NOTE : Be careful that the {@link this#run()} stays not transactional.
 *
 * @author Sébastien Binda
 *
 */
public class FileDeletionRequestsCreatorJob extends AbstractJob<Void> {

    public static final String STORAGE_LOCATION_ID = "storage";

    public static final String FORCE_DELETE = "force";

    private static final int PAGE_BULK_SIZE = 500;

    @Autowired
    private IPublisher publisher;

    @Autowired
    private FileReferenceService fileRefService;

    @Autowired
    private FileDeletionRequestService fileDelReqService;

    /**
     * The job parameters as a map
     */
    protected Map<String, JobParameter> parameters;

    @Override
    public void setParameters(Map<String, JobParameter> parameters)
            throws JobParameterMissingException, JobParameterInvalidException {
        this.parameters = parameters;
    }

    @Override
    public void run() {
        String storage = parameters.get(STORAGE_LOCATION_ID).getValue();
        Boolean forceDelete = parameters.get(FORCE_DELETE).getValue();
        Pageable pageRequest = PageRequest.of(0, PAGE_BULK_SIZE);
        Page<FileReference> pageResults;
        String requestGroupId = String.format("DELETION-%s", UUID.randomUUID().toString());
        do {
            // Search for all file references of the given storage location
            pageResults = fileRefService.search(storage, pageRequest);
            for (FileReference fileRef : pageResults.getContent()) {
                // For each :
                // If file is owned send a deletion event for each owner.
                // Else create deletion request
                if (fileRef.getOwners().isEmpty()) {
                    fileDelReqService.create(fileRef, forceDelete, requestGroupId);
                } else {
                    for (String owner : fileRef.getOwners()) {
                        publisher.publish(DeletionFlowItem.build(FileDeletionRequestDTO
                                .build(fileRef.getMetaInfo().getChecksum(), storage, owner, forceDelete),
                                                                 requestGroupId));
                    }
                }
            }
        } while (pageResults.hasNext());
    }

}
