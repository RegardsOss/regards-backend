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
package fr.cnes.regards.modules.storagelight.service.file;

import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.persistence.EntityManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.storagelight.dao.IFileReferenceRepository;
import fr.cnes.regards.modules.storagelight.domain.database.FileLocation;
import fr.cnes.regards.modules.storagelight.domain.database.FileReference;
import fr.cnes.regards.modules.storagelight.domain.database.FileReferenceMetaInfo;
import fr.cnes.regards.modules.storagelight.domain.database.request.FileDeletionRequest;
import fr.cnes.regards.modules.storagelight.domain.database.request.FileRequestStatus;
import fr.cnes.regards.modules.storagelight.domain.database.request.FileStorageRequest;
import fr.cnes.regards.modules.storagelight.domain.dto.FileCopyRequestDTO;
import fr.cnes.regards.modules.storagelight.domain.dto.FileDeletionRequestDTO;
import fr.cnes.regards.modules.storagelight.domain.dto.FileReferenceRequestDTO;
import fr.cnes.regards.modules.storagelight.domain.dto.FileStorageRequestDTO;
import fr.cnes.regards.modules.storagelight.domain.event.FileReferenceEvent;
import fr.cnes.regards.modules.storagelight.domain.event.FileRequestType;
import fr.cnes.regards.modules.storagelight.domain.flow.AvailabilityFlowItem;
import fr.cnes.regards.modules.storagelight.domain.flow.CopyFlowItem;
import fr.cnes.regards.modules.storagelight.domain.flow.DeletionFlowItem;
import fr.cnes.regards.modules.storagelight.domain.flow.ReferenceFlowItem;
import fr.cnes.regards.modules.storagelight.domain.flow.StorageFlowItem;
import fr.cnes.regards.modules.storagelight.domain.plugin.IStorageLocation;
import fr.cnes.regards.modules.storagelight.service.file.request.FileCacheRequestService;
import fr.cnes.regards.modules.storagelight.service.file.request.FileCopyRequestService;
import fr.cnes.regards.modules.storagelight.service.file.request.FileDeletionRequestService;
import fr.cnes.regards.modules.storagelight.service.file.request.FileStorageRequestService;
import fr.cnes.regards.modules.storagelight.service.file.request.RequestsGroupService;
import fr.cnes.regards.modules.storagelight.service.location.StoragePluginConfigurationHandler;

/**
 * Service to handle File references.<br/>
 *
 * <b>File reference definition : </b><ul>
 *  <li> Mandatory checksum in {@link FileReferenceMetaInfo} </li>
 *  <li> Mandatory storage location in {@link FileLocation}</li>
 *  <li> Optional list of owners</li>
 * </ul>
 *
 * <b> File reference physical location : </b><br/>
 * A file can be referenced through this system by : <ul>
 * <li> Storing file on a storage location thanks to {@link IStorageLocation} plugins </li>
 * <li> Only reference file assuming the file location is handled externally </li>
 * </ul>
 * A file reference storage/deletion is handled by the service if the storage location is known as a {@link IStorageLocation} plugin configuration.<br/>
 *
 * <b>File reference owners : </b><br/>
 * When a file reference does not have any owner, then it is scheduled for deletion.<br/>
 *
 * <b> Entry points : </b><br/>
 * File references can be created using AMQP messages {@link ReferenceFlowItem}.<br/>
 * File references can be deleted using AMQP messages {@link DeletionFlowItem}.<br/>
 * File references can be copied in cache system using AMQP messages {@link CopyFlowItem}.<br/>
 * File references can be download<br/>
 *
 * @author Sébastien Binda
 */
@Service
@MultitenantTransactional
public class FileRequestService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileRequestService.class);

    @Autowired
    private IFileReferenceRepository fileRefRepo;

    @Autowired
    private FileReferenceEventPublisher fileRefEventPublisher;

    @Autowired
    private RequestsGroupService reqGrpService;

    @Autowired
    private FileStorageRequestService fileStorageRequestService;

    @Autowired
    private FileDeletionRequestService fileDeletionRequestService;

    @Autowired
    private FileCacheRequestService fileCacheRequestService;

    @Autowired
    private FileCopyRequestService fileCopyRequestService;

    @Autowired
    private StoragePluginConfigurationHandler storageHandler;

    @Autowired
    private FileReferenceService fileRefService;

    @Autowired
    private EntityManager em;

    /**
     * Handle many {@link FileCopyRequestDTO} to copy files to a given storage location.
     * @param files copy requests
     * @param groupId business request identifier
     */
    public void copy(Collection<FileCopyRequestDTO> files, String groupId) {
        files.forEach(f -> {
            fileCopyRequestService.create(f, groupId);
        });
    }

    /**
     * Handle many {@link CopyFlowItem} to copy files to a given storage location.
     * @param items copy flow items
     */
    public void copy(Collection<CopyFlowItem> items) {
        items.forEach(i -> copy(i.getFiles(), i.getGroupId()));
    }

    /**
     * Handle many {@link StorageFlowItem} to store files to a given storage location.
     * @param items storage flow items
     */
    public void store(Collection<StorageFlowItem> items) {
        int flushCount = 0;
        // Retrieve already existing ones by checksum only to improve performance. The associated storage location is checked later
        Set<String> checksums = Sets.newHashSet();
        items.forEach(i -> {
            i.getFiles().stream().forEach(f -> checksums.add(f.getChecksum()));
        });
        Set<FileReference> existingOnes = fileRefRepo.findByMetaInfoChecksumIn(checksums);
        for (StorageFlowItem item : items) {
            for (FileStorageRequestDTO request : item.getFiles()) {
                // Check if the file already exists for the storage destination
                Optional<FileReference> oFileRef = existingOnes.stream()
                        .filter(f -> f.getMetaInfo().getChecksum().equals(request.getChecksum())
                                && f.getLocation().getStorage().contentEquals(request.getStorage()))
                        .findFirst();
                store(request, oFileRef, item.getGroupId());
                // Performance optimization.
                flushCount++;
                if (flushCount > 100) {
                    em.flush();
                    em.clear();
                    flushCount = 0;
                }
            }
        }
    }

    /**
     * Store a new file to a given storage destination
     * @param owner Owner of the new file
     * @param metaInfo information about file
     * @param originUrl current location of file. This URL must be locally accessible to be copied.
     * @param storage name of the storage destination. Must be a existing plugin configuration of a {@link IStorageLocation}
     * @param subDirectory where to store file in the destination location.
     * @param groupId business request identifier
     * @return {@link FileReference} if the file is already referenced.
     */
    public Optional<FileReference> store(String owner, FileReferenceMetaInfo metaInfo, URL originUrl, String storage,
            Optional<String> subDirectory, String groupId) {
        Optional<FileReference> oFileRef = fileRefRepo.findByLocationStorageAndMetaInfoChecksum(storage,
                                                                                                metaInfo.getChecksum());
        return store(FileStorageRequestDTO.build(metaInfo.getFileName(), metaInfo.getChecksum(),
                                                 metaInfo.getAlgorithm(), metaInfo.getMimeType().toString(), owner,
                                                 originUrl, storage, subDirectory),
                     oFileRef, groupId);
    }

    /**
     * Store a new file to a given storage destination
     * @param request {@link FileStorageRequestDTO} info about file to store
     * @param fileRef {@link FileReference} of associated file if already exists
     * @param groupId business request identifier
     * @return {@link FileReference} if the file is already referenced.
     */
    private Optional<FileReference> store(FileStorageRequestDTO request, Optional<FileReference> fileRef,
            String groupId) {
        if (fileRef.isPresent()) {
            return handleAlreadyExists(fileRef.get(), request, groupId);
        } else {
            fileStorageRequestService.create(Sets.newHashSet(request.getOwner()), request.buildMetaInfo(),
                                             request.getOriginUrl(), request.getStorage(), request.getSubDirectory(),
                                             groupId);
            return Optional.empty();
        }
    }

    /**
     * Call {@link #addFileReference(ReferenceFlowItem)} method for each item.
     * @param items
     */
    public void reference(List<ReferenceFlowItem> items) {
        int flushCount = 0;
        // Retrieve already existing ones by checksum only to improve performance. The associated storage location is checked later
        Set<String> checksums = Sets.newHashSet();
        items.forEach(i -> {
            i.getFiles().stream().forEach(f -> checksums.add(f.getChecksum()));
        });
        Set<FileReference> existingOnes = fileRefRepo.findByMetaInfoChecksumIn(checksums);
        for (ReferenceFlowItem item : items) {
            for (FileReferenceRequestDTO file : item.getFiles()) {
                // Check if the file already exists for the storage destination
                Optional<FileReference> oFileRef = existingOnes.stream()
                        .filter(f -> f.getMetaInfo().getChecksum().equals(file.getChecksum())
                                && f.getLocation().getStorage().contentEquals(file.getStorage()))
                        .findFirst();
                try {
                    FileReference fileRef = reference(file, oFileRef, Sets.newHashSet(item.getGroupId()));
                    String message = String.format("New file <%s> referenced at <%s> (checksum: %s)",
                                                   fileRef.getMetaInfo().getFileName(),
                                                   fileRef.getLocation().toString(),
                                                   fileRef.getMetaInfo().getChecksum());
                    LOGGER.debug(message);
                    fileRefEventPublisher.storeSuccess(fileRef, message, item.getGroupId());
                    reqGrpService.requestSuccess(item.getGroupId(), FileRequestType.REFERENCE,
                                                 fileRef.getMetaInfo().getChecksum(),
                                                 fileRef.getLocation().getStorage(), fileRef);
                } catch (ModuleException e) {
                    fileRefEventPublisher.storeError(file.getChecksum(), Sets.newHashSet(file.getOwner()),
                                                     file.getStorage(), e.getMessage(),
                                                     Sets.newHashSet(item.getGroupId()));
                    reqGrpService.requestError(item.getGroupId(), FileRequestType.REFERENCE, file.getChecksum(),
                                               file.getStorage(), e.getMessage());
                }
                // Performance optimization.
                flushCount++;
                if (flushCount > 100) {
                    em.flush();
                    em.clear();
                    flushCount = 0;
                }
            }
            reqGrpService.done(item.getGroupId(), FileRequestType.REFERENCE);
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
        Optional<FileReference> oFileRef = fileRefRepo.findByLocationStorageAndMetaInfoChecksum(location.getStorage(),
                                                                                                metaInfo.getChecksum());
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
            return fileRefService.create(Lists.newArrayList(request.getOwner()), request.buildMetaInfo(),
                                         new FileLocation(request.getStorage(), request.getUrl()));
        }
    }

    /**
     * Handle the given {@link DeletionFlowItem}s.
     * @param items
     */
    public void delete(Collection<DeletionFlowItem> items) {
        long start = System.currentTimeMillis();
        Set<String> checksums = Sets.newHashSet();
        items.forEach(i -> {
            i.getFiles().stream().forEach(f -> checksums.add(f.getChecksum()));
        });
        Set<FileReference> existingOnes = fileRefRepo.findByMetaInfoChecksumIn(checksums);
        for (DeletionFlowItem item : items) {
            for (FileDeletionRequestDTO request : item.getFiles()) {
                Optional<FileReference> oFileRef = existingOnes.stream()
                        .filter(f -> f.getLocation().getStorage().contentEquals(request.getStorage())).findFirst();
                if (oFileRef.isPresent()) {
                    removeOwner(oFileRef.get(), request.getOwner(), request.isForceDelete(), item.getGroupId());
                } else {
                    // File does not exists. Handle has deletion success
                    reqGrpService.requestSuccess(item.getGroupId(), FileRequestType.DELETION, request.getChecksum(),
                                                 request.getStorage(), oFileRef.orElse(null));
                }
            }
            reqGrpService.done(item.getGroupId(), FileRequestType.DELETION);
        }
        LOGGER.debug("...deletion of {} refs handled in {} ms", items.size(), System.currentTimeMillis() - start);
    }

    /**
     * Remove the given owner of the to the {@link FileReference} matching the given checksum and storage location.
     * If the owner is the last one this method tries to delete file physically if the storage location is a configured {@link IStorageLocation}.
     * @param forceDelete allows to delete fileReference even if the deletion is in error.
     * @param groupId Business identifier of the deletion request
     * @throws EntityNotFoundException
     */
    public void removeOwner(String checksum, String storage, String owner, boolean forceDelete, String groupId)
            throws EntityNotFoundException {
        Assert.notNull(checksum, "Checksum is mandatory to delete a file reference");
        Assert.notNull(storage, "Storage is mandatory to delete a file reference");
        Assert.notNull(owner, "Owner is mandatory to delete a file reference");
        Optional<FileReference> oFileRef = fileRefRepo.findByLocationStorageAndMetaInfoChecksum(storage, checksum);
        if (oFileRef.isPresent()) {
            removeOwner(oFileRef.get(), owner, forceDelete, groupId);
        } else {
            throw new EntityNotFoundException(String
                    .format("File reference with ckesum: <%s> and storage: <%s> doest not exists", checksum, storage));
        }
    }

    /**
     * Remove the given owner of the to the given {@link FileReference}.
     * If the owner is the last one this method tries to delete file physically if the storage location is a configured {@link IStorageLocation}.
     * @param forceDelete allows to delete fileReference even if the deletion is in error.
     * @param groupId Business identifier of the deletion request
     */
    private void removeOwner(FileReference fileReference, String owner, boolean forceDelete, String groupId) {
        fileRefService.removeOwner(fileReference, owner, groupId);
        // If file reference does not belongs to anyone anymore, delete file reference
        if (fileReference.getOwners().isEmpty()) {
            if (storageHandler.getConfiguredStorages().contains(fileReference.getLocation().getStorage())) {
                // If the file is stored on an accessible storage, create a new deletion request
                fileDeletionRequestService.create(fileReference, forceDelete, groupId);
            } else {
                // Else, directly delete the file reference
                fileRefService.delete(fileReference, groupId);
                reqGrpService.requestSuccess(groupId, FileRequestType.DELETION,
                                             fileReference.getMetaInfo().getChecksum(),
                                             fileReference.getLocation().getStorage(), fileReference);
            }
        }
    }

    public void makeAvailable(Collection<AvailabilityFlowItem> items) {
        items.forEach(i -> fileCacheRequestService.makeAvailable(i.getChecksums(), i.getExpirationDate(),
                                                                 i.getGroupId()));
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
            if (!fileReference.getOwners().contains(request.getOwner())) {
                fileReference.getOwners().add(request.getOwner());
                String message = String
                        .format("New owner <%s> added to existing referenced file <%s> at <%s> (checksum: %s) ",
                                request.getOwner(), fileReference.getMetaInfo().getFileName(),
                                fileReference.getLocation().toString(), fileReference.getMetaInfo().getChecksum());
                if (!fileReference.getMetaInfo().equals(request.buildMetaInfo())) {
                    LOGGER.warn("Existing referenced file meta information differs "
                            + "from new reference meta information. Previous ones are maintained");
                }
                FileReference updatedFileRef = fileRefRepo.save(fileReference);
                LOGGER.debug(message);
                return updatedFileRef;
            } else {
                String message = String.format("File %s already referenced at %s (checksum: %s) for owners %s",
                                               fileReference.getMetaInfo().getFileName(),
                                               fileReference.getLocation().toString(),
                                               fileReference.getMetaInfo().getChecksum(),
                                               Arrays.toString(fileReference.getOwners().toArray()));
                fileRefEventPublisher.storeSuccess(fileReference, message, groupIds);
                LOGGER.debug(message);
                return fileReference;
            }
        }
    }

    /**
     * Update if needed an already existing {@link FileReference} associated to a
     * new {@link FileStorageRequestDTO} request received.<br/>
     * <br/>
     * If a deletion request is running on the existing {@link FileReference} then a new {@link FileStorageRequest}
     * request is created as DELAYED.<br/>
     *
     * @param fileReference {@link FileReference} to update
     * @param request associated {@link FileStorageRequestDTO} new request
     * @param groupId new business request identifier
     * @return {@link FileReference} updated or null.
     */
    private Optional<FileReference> handleAlreadyExists(FileReference fileReference, FileStorageRequestDTO request,
            String groupId) {
        FileReference updatedFileRef = null;
        FileReferenceMetaInfo newMetaInfo = request.buildMetaInfo();
        Optional<FileDeletionRequest> deletionRequest = fileDeletionRequestService.search(fileReference);
        if (deletionRequest.isPresent() && (deletionRequest.get().getStatus() == FileRequestStatus.PENDING)) {
            // Deletion is running write now, so delay the new file reference creation with a FileReferenceRequest
            fileStorageRequestService.create(Sets.newHashSet(request.getOwner()), newMetaInfo, request.getOriginUrl(),
                                             request.getStorage(), request.getSubDirectory(), FileRequestStatus.DELAYED,
                                             groupId);
        } else {
            if (deletionRequest.isPresent()) {
                // Delete not running deletion request to add the new owner
                fileDeletionRequestService.delete(deletionRequest.get());
            }
            if (!fileReference.getOwners().contains(request.getOwner())) {
                fileReference.getOwners().add(request.getOwner());
                String message = String
                        .format("New owner <%s> added to existing referenced file <%s> at <%s> (checksum: %s) ",
                                request.getOwner(), fileReference.getMetaInfo().getFileName(),
                                fileReference.getLocation().toString(), fileReference.getMetaInfo().getChecksum());
                LOGGER.debug(message);
                if (!fileReference.getMetaInfo().equals(newMetaInfo)) {
                    LOGGER.warn("Existing referenced file meta information differs "
                            + "from new reference meta information. Previous ones are maintained");
                }
                updatedFileRef = fileRefRepo.save(fileReference);
                fileRefEventPublisher.storeSuccess(fileReference, message, groupId);
            } else {
                String message = String.format("File %s already referenced at %s (checksum: %s) for owners %s",
                                               fileReference.getMetaInfo().getFileName(),
                                               fileReference.getLocation().toString(),
                                               fileReference.getMetaInfo().getChecksum(),
                                               Arrays.toString(fileReference.getOwners().toArray()));
                updatedFileRef = fileReference;
                fileRefEventPublisher.storeSuccess(fileReference, message, groupId);
            }
        }
        return Optional.ofNullable(updatedFileRef);
    }
}
