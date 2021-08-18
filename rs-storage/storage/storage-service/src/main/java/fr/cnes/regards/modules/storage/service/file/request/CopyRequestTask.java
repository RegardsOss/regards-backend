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
package fr.cnes.regards.modules.storage.service.file.request;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;

import com.google.common.collect.Sets;

import fr.cnes.regards.modules.storage.dao.IFileCopyRequestRepository;
import fr.cnes.regards.modules.storage.domain.database.request.FileCopyRequest;
import fr.cnes.regards.modules.storage.domain.database.request.FileRequestStatus;
import fr.cnes.regards.modules.storage.domain.event.FileRequestType;
import fr.cnes.regards.modules.storage.domain.flow.AvailabilityFlowItem;
import net.javacrumbs.shedlock.core.LockingTaskExecutor.Task;

/**
 * Task to be executed once locked by {@link FileCopyRequestService}
 *
 * @author Sébastien Binda
 *
 */
public class CopyRequestTask implements Task {

    private static final Logger LOGGER = LoggerFactory.getLogger(CopyRequestTask.class);

    /**
     * Status of {@link FileCopyRequest}s to handle
     */
    private final FileRequestStatus status;

    private final FileCacheRequestService fileCacheReqService;

    private final IFileCopyRequestRepository copyRepository;

    private final RequestsGroupService reqGrpService;

    public CopyRequestTask(FileCacheRequestService fileCacheReqService, IFileCopyRequestRepository copyRepository,
            RequestsGroupService reqGrpService, FileRequestStatus status) {
        super();
        this.status = status;
        this.fileCacheReqService = fileCacheReqService;
        this.copyRepository = copyRepository;
        this.reqGrpService = reqGrpService;
    }

    @Override
    public void call() throws Throwable {
        LOGGER.trace("[COPY REQUESTS] handling copy requests ...");
        long start = System.currentTimeMillis();
        Long maxId = 0L;
        // Always search the first page of requests until there is no requests anymore.
        // To do so, we order on id to ensure to not handle same requests multiple times.
        Pageable page = PageRequest.of(0, AvailabilityFlowItem.MAX_REQUEST_PER_GROUP, Direction.ASC, "id");
        Page<FileCopyRequest> pageResp = null;
        // Allow file availability for one day to let enough time to next storage process to be perform.
        OffsetDateTime expDate = OffsetDateTime.now().plusDays(1);
        do {
            String fileCacheGroupId = UUID.randomUUID().toString();
            pageResp = copyRepository.findByStatusAndIdGreaterThan(status, maxId, page);
            if (pageResp.hasContent()) {
                maxId = pageResp.stream().max(Comparator.comparing(FileCopyRequest::getId)).get().getId();
                Set<String> checksums = Sets.newHashSet();
                for (FileCopyRequest request : pageResp) {
                    checksums.add(request.getMetaInfo().getChecksum());
                    request.setFileCacheGroupId(fileCacheGroupId);
                    request.setStatus(FileRequestStatus.PENDING);
                }
                copyRepository.saveAll(pageResp.getContent());

                if (!checksums.isEmpty()) {
                    reqGrpService.granted(fileCacheGroupId, FileRequestType.AVAILABILITY, checksums.size(), true,
                                          expDate);
                    fileCacheReqService.makeAvailable(checksums, expDate, fileCacheGroupId);
                }
            }
        } while (pageResp.hasContent());
        LOGGER.debug("[COPY REQUESTS] Copy requests handled in {} ms", System.currentTimeMillis() - start);
    }

}
