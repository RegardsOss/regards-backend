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
package fr.cnes.regards.modules.storagelight.service.file.request;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.storagelight.dao.IFileReferenceRepository;
import fr.cnes.regards.modules.storagelight.domain.database.FileLocation;
import fr.cnes.regards.modules.storagelight.domain.database.FileReference;
import fr.cnes.regards.modules.storagelight.domain.database.FileReferenceMetaInfo;
import fr.cnes.regards.modules.storagelight.domain.database.request.FileDeletionRequest;
import fr.cnes.regards.modules.storagelight.domain.database.request.FileRequestStatus;
import fr.cnes.regards.modules.storagelight.domain.dto.request.FileReferenceRequestDTO;
import fr.cnes.regards.modules.storagelight.domain.event.FileReferenceEvent;
import fr.cnes.regards.modules.storagelight.domain.event.FileRequestType;
import fr.cnes.regards.modules.storagelight.domain.flow.ReferenceFlowItem;
import fr.cnes.regards.modules.storagelight.service.file.FileReferenceEventPublisher;
import fr.cnes.regards.modules.storagelight.service.file.FileReferenceService;

/**
 * Service to handle File reference requests.
 *
 * @author Sébastien Binda
 */
@Service
@MultitenantTransactional
public class FileReferenceRequestService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileReferenceRequestService.class);

    @Autowired
    private FileReferenceEventPublisher fileRefEventPublisher;

    @Autowired
    private RequestsGroupService reqGrpService;

    @Autowired
    private FileDeletionRequestService fileDeletionRequestService;

    @Autowired
    private FileReferenceService fileRefService;
    
    /**
     * Initialize new reference requests from Flow items.
     * @param list
     */
    public void reference(List<ReferenceFlowItem> list) {
    	Set<FileReference> existingOnes = fileRefService
                .search(list.stream().map(ReferenceFlowItem::getFiles).flatMap(Set::stream).map(FileReferenceRequestDTO::getChecksum).collect(Collectors.toSet()));
    	Set<String> groupsToGrant = Sets.newHashSet();
        for (ReferenceFlowItem item : list) {
            reference(item.getFiles(), item.getGroupId(), existingOnes);
            groupsToGrant.add(item.getGroupId());
        }
        reqGrpService.granted(groupsToGrant, FileRequestType.REFERENCE);
    }
    
    /**
     * Initialize new reference requests for a given group identifier
     * @param requests
     * @param groupId
     */
    public void reference(Collection<FileReferenceRequestDTO> requests, String groupId) {
    	// Retrieve already existing ones by checksum only to improve performance. The associated storage location is checked later
    	Set<FileReference> existingOnes = fileRefService
                .search(requests.stream().map(FileReferenceRequestDTO::getChecksum).collect(Collectors.toSet()));
    	reference(requests, groupId, existingOnes);
    }

   /**
    * Initialize new reference requests for a given group identifier. Parameter existingOnes is passed to improve performance in bulk creation to
    * avoid requesting {@link IFileReferenceRepository} on each request.
    * @param requests
    * @param groupId
    * @param existingOnes
    */
    public void reference(Collection<FileReferenceRequestDTO> requests, String groupId, Collection<FileReference> existingOnes) {
        for (FileReferenceRequestDTO file : requests) {
            // Check if the file already exists for the storage destination
            Optional<FileReference> oFileRef = existingOnes.stream()
                    .filter(f -> f.getMetaInfo().getChecksum().equals(file.getChecksum())
                            && f.getLocation().getStorage().contentEquals(file.getStorage()))
                    .findFirst();
            try {
                FileReference fileRef = reference(file, oFileRef, Sets.newHashSet(groupId));
                reqGrpService.requestSuccess(groupId, FileRequestType.REFERENCE, fileRef.getMetaInfo().getChecksum(),
                                             fileRef.getLocation().getStorage(), fileRef);
            } catch (ModuleException e) {
                LOGGER.error(e.getMessage(), e);
                fileRefEventPublisher.storeError(file.getChecksum(), Sets.newHashSet(file.getOwner()),
                                                 file.getStorage(), e.getMessage(), Sets.newHashSet(groupId));
                reqGrpService.requestError(groupId, FileRequestType.REFERENCE, file.getChecksum(), file.getStorage(),
                                           e.getMessage());
            }
        }
    }

    /**
     * Reference a new file. No file movement is made here. File is only referenced.
     * @param owner Owner of the new {@link FileReference}
     * @param metaInfo information about file
     * @param location location of file
     * @param groupIds Business requests identifiers associated to the new file reference.
     * @return {@link FileReference}
     * @throws ModuleException if the file reference can not be created.
     */
    public FileReference reference(String owner, FileReferenceMetaInfo metaInfo, FileLocation location,
            Collection<String> groupIds) throws ModuleException {
        Optional<FileReference> oFileRef = fileRefService.search(location.getStorage(), metaInfo.getChecksum());
        return reference(FileReferenceRequestDTO
                .build(metaInfo.getFileName(), metaInfo.getChecksum(), metaInfo.getAlgorithm(),
                       metaInfo.getMimeType().toString(), metaInfo.getFileSize(), owner, location.getStorage(),
                       location.getUrl())
                .withHeight(metaInfo.getHeight()).withWidth(metaInfo.getWidth()).withType(metaInfo.getType()), oFileRef,
                         groupIds);
    }

    /**
     * Reference a new file. No file movement is made here. File is only referenced.
     * @param request {@link FileReferenceRequestDTO}
     * @param fileRef {@link FileReference} of associated file if already exists
     * @param groupIds Business requests identifiers associated to the new file reference.
     * @return {@link FileReference}
     * @throws ModuleException if the file reference can not be created.
     */
    private FileReference reference(FileReferenceRequestDTO request, Optional<FileReference> fileRef,
            Collection<String> groupIds) throws ModuleException {
        if (fileRef.isPresent()) {
            return handleAlreadyExists(fileRef.get(), request, groupIds);
        } else {
            FileReference newFileRef = fileRefService.create(Lists.newArrayList(request.getOwner()),
                                                             request.buildMetaInfo(),
                                                             new FileLocation(request.getStorage(), request.getUrl()));
            String message = String.format("New file <%s> referenced at <%s> (checksum: %s)",
                                           newFileRef.getMetaInfo().getFileName(), newFileRef.getLocation().toString(),
                                           newFileRef.getMetaInfo().getChecksum());
            fileRefEventPublisher.storeSuccess(newFileRef, message, groupIds);
            return newFileRef;
        }
    }

    /**
     * Handle the creation of a new {@link FileReference} when the file already exists.
     *
     * <ul>
     * <li>1. If a deletion request exists on the file reference, tries to remove the deletion request</li>
     * <li>2. If a deletion request exists and is pending on the file reference, create a new DELAYED file reference request.
     *  In order to retry storage after deletion is done</li>
     * <li>3. Add the new owners of the existing file reference</li>
     * <li>4. Send a {@link FileReferenceEvent} as STORED with the new owners</li>
     * </ul>
     *
     * @return {@link FileReference} if the file reference is updated. Null if a new store request is created.
     * @throws ModuleException If file reference can not be created
     */
    private FileReference handleAlreadyExists(FileReference fileReference, FileReferenceRequestDTO request,
            Collection<String> groupIds) throws ModuleException {
        Optional<FileDeletionRequest> deletionRequest = fileDeletionRequestService.search(fileReference);
        if (deletionRequest.isPresent() && (deletionRequest.get().getStatus() == FileRequestStatus.PENDING)) {
            // A deletion is pending on the existing file reference but the new reference request does not indicates the new file location
            String message = String.format("File %s is being deleted. Please try later.", request.getChecksum());
            groupIds.forEach(id -> fileRefEventPublisher
                    .storeError(request.getChecksum(), Sets.newHashSet(request.getOwner()), request.getStorage(),
                                message, Sets.newHashSet(id)));
            throw new ModuleException(message);
        } else {
            if (deletionRequest.isPresent()) {
                // Delete not running deletion request to add the new owner
                fileDeletionRequestService.delete(deletionRequest.get());
            }
            if (!fileReference.getMetaInfo().equals(request.buildMetaInfo())) {
                LOGGER.warn("Existing referenced file meta information differs "
                        + "from new reference meta information. Previous ones are maintained");
            }
            String message = String
                    .format("New owner <%s> added to existing referenced file <%s> at <%s> (checksum: %s) ",
                            request.getOwner(), fileReference.getMetaInfo().getFileName(),
                            fileReference.getLocation().toString(), fileReference.getMetaInfo().getChecksum());
            fileRefEventPublisher.storeSuccess(fileReference, message, groupIds);
            return fileRefService.addOwner(fileReference, request.getOwner());
        }
    }
}
