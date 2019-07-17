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
package fr.cnes.regards.modules.storagelight.service;

import java.util.Collection;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.modules.storagelight.dao.IFileReferenceRepository;
import fr.cnes.regards.modules.storagelight.domain.FileRequestStatus;
import fr.cnes.regards.modules.storagelight.domain.database.FileDeletionRequest;
import fr.cnes.regards.modules.storagelight.domain.database.FileLocation;
import fr.cnes.regards.modules.storagelight.domain.database.FileReference;
import fr.cnes.regards.modules.storagelight.domain.database.FileReferenceMetaInfo;
import fr.cnes.regards.modules.storagelight.domain.database.StorageMonitoringAggregation;

/**
 * @author Sébastien Binda
 */
@Service
@MultitenantTransactional
public class FileReferenceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileReferenceService.class);

    @Autowired
    private IFileReferenceRepository fileRefRepo;

    @Autowired
    private FileReferenceRequestService fileRefRequestService;

    @Autowired
    private FileDeletionRequestService fileDeletionRequestService;

    @Autowired
    private StoragePluginConfigurationHandler storageHandler;

    public Page<FileReference> search(String storage, Pageable pageable) {
        return fileRefRepo.findByLocationStorage(storage, pageable);
    }

    public Optional<FileReference> search(String storage, String checksum) {
        return fileRefRepo.findByMetaInfoChecksumAndLocationStorage(checksum, storage);
    }

    public Page<FileReference> search(Pageable pageable) {
        return fileRefRepo.findAll(pageable);
    }

    public Page<FileReference> search(Specification<FileReference> spec, Pageable page) {
        return fileRefRepo.findAll(spec, page);
    }

    /**
     * <b>Method to reference a given file</b> <br/><br />
     * If the file is <b>already referenced</b> in the destination storage,
     * this method only add the requesting owner to the file reference owner list.
     * <br/>
     * If the <b>origin destination equals the destination origin</b>, so reference the file as already stored.
     *
     * @param fileRefRequest file to reference
     * @return FileReference if already exists or does not need a new storage job
     */
    public Optional<FileReference> addFileReference(Collection<String> owners, FileReferenceMetaInfo fileMetaInfo,
            FileLocation origin, FileLocation destination) {

        Assert.notNull(owners, "File must have a owner to be referenced");
        Assert.isTrue(!owners.isEmpty(), "File must have a owner to be referenced");
        Assert.notNull(fileMetaInfo, "File must have an origin location to be referenced");
        Assert.notNull(fileMetaInfo.getChecksum(), "File checksum is mandatory");
        Assert.notNull(fileMetaInfo.getAlgorithm(), "File checksum algorithm is mandatory");
        Assert.notNull(fileMetaInfo.getFileName(), "File name is mandatory");
        Assert.notNull(fileMetaInfo.getMimeType(), "File mime type is mandatory");
        Assert.notNull(origin, "File must have an origin location to be referenced");
        Assert.notNull(destination, "File must have an origin location to be referenced");

        // Does file is already referenced for the destination location ?
        Optional<FileReference> oFileRef = fileRefRepo
                .findByMetaInfoChecksumAndLocationStorage(fileMetaInfo.getChecksum(), destination.getStorage());
        if (oFileRef.isPresent()) {
            this.handleFileReferenceAlreadyExists(oFileRef.get(), fileMetaInfo, origin, destination, owners);
        } else {
            // If destination equals origin location so file is already stored and can be referenced directly
            if (destination.equals(origin)) {
                oFileRef = Optional.of(this.createNewFileReference(owners, fileMetaInfo, destination));
            } else {
                fileRefRequestService.createNewFileReferenceRequest(owners, fileMetaInfo, origin, destination);
            }
        }
        return oFileRef;
    }

    public void deleteFileReference(FileReference fileRef) {
        Assert.notNull(fileRef, "File reference to delete cannot be null");
        Assert.notNull(fileRef.getId(), "File reference identifier to delete cannot be null");
        fileRefRepo.deleteById(fileRef.getId());
        // TODO : Notify owners of the file
    }

    public void removeOwner(String checksum, String storage, String owner) throws EntityNotFoundException {

        Assert.notNull(checksum, "Checksum is mandatory to delete a file reference");
        Assert.notNull(storage, "Storage is mandatory to delete a file reference");
        Assert.notNull(owner, "Owner is mandatory to delete a file reference");

        Optional<FileReference> oFileRef = fileRefRepo.findByMetaInfoChecksumAndLocationStorage(checksum, storage);
        if (oFileRef.isPresent()) {
            this.removeOwner(oFileRef.get(), owner);
        } else {
            throw new EntityNotFoundException(String
                    .format("File reference with ckesum: <%s> and storage: <%s> doest not exists", checksum, storage));
        }
    }

    private void removeOwner(FileReference fileReference, String owner) {
        if (!fileReference.getOwners().contains(owner)) {
            LOGGER.info("File <{} (checksum: {})> at {} does not to belongs to %s",
                        fileReference.getMetaInfo().getFileName(), fileReference.getMetaInfo().getChecksum(),
                        fileReference.getLocation().toString(), owner);
        } else {
            fileReference.getOwners().remove(owner);
            LOGGER.info("File reference <{} (checksum: {})> at {} does not belongs to {} anymore",
                        fileReference.getMetaInfo().getFileName(), fileReference.getMetaInfo().getChecksum(),
                        fileReference.getLocation().toString(), owner);
            fileRefRepo.save(fileReference);
        }

        // If file reference does not belongs to anyone anymore, delete file reference
        if (fileReference.getOwners().isEmpty()) {
            if (storageHandler.getConfiguredStorages().contains(fileReference.getLocation().getStorage())) {
                // If the file is stored on an accessible storage, create a new deletion request
                fileDeletionRequestService.createNewFileDeletionRequest(fileReference);
            } else {
                // Else, directly delete the file reference
                this.deleteFileReference(fileReference);
            }
        }

        // TODO : Notify
    }

    private FileReference createNewFileReference(Collection<String> owners, FileReferenceMetaInfo fileMetaInfo,
            FileLocation location) {
        FileReference fileRef = new FileReference(owners, fileMetaInfo, location);
        fileRef = fileRefRepo.save(fileRef);
        LOGGER.info("New file <{}> referenced at <{}> (checksum: {})", fileRef.getMetaInfo().getFileName(),
                    fileRef.getLocation().toString(), fileRef.getMetaInfo().getChecksum());
        return fileRef;
    }

    public Collection<StorageMonitoringAggregation> calculateTotalFileSizeAggregation(Long lastReferencedFileId) {
        if (lastReferencedFileId != null) {
            return fileRefRepo.getTotalFileSizeAggregation(lastReferencedFileId);
        } else {
            return fileRefRepo.getTotalFileSizeAggregation();
        }
    }

    private void handleFileReferenceAlreadyExists(FileReference fileReference, FileReferenceMetaInfo newMetaInfo,
            FileLocation origin, FileLocation destination, Collection<String> owners) {
        boolean newOwners = false;

        Optional<FileDeletionRequest> deletionRequest = fileDeletionRequestService.search(fileReference);
        if (deletionRequest.isPresent() && (deletionRequest.get().getStatus() == FileRequestStatus.PENDING)) {
            // Deletion is running write now, so delay the new file reference creation with a FileReferenceRequest
            fileRefRequestService.createNewFileReferenceRequest(owners, newMetaInfo, origin, destination,
                                                                FileRequestStatus.DELAYED);
        } else {
            if (deletionRequest.isPresent()) {
                // Delete not running deletion request to add the new owner
                fileDeletionRequestService.deleteFileDeletionRequest(deletionRequest.get());
            }
            for (String owner : owners) {
                if (!fileReference.getOwners().contains(owner)) {
                    newOwners = true;
                    fileReference.getOwners().add(owner);
                    LOGGER.info("New owner <{}> added to existing referenced file <{}> at <{}> (checksum: {}) ", owner,
                                fileReference.getMetaInfo().getFileName(), fileReference.getLocation().toString(),
                                fileReference.getMetaInfo().getChecksum());
                    if (!fileReference.getMetaInfo().equals(newMetaInfo)) {
                        LOGGER.warn("Existing referenced file meta information differs "
                                + "from new reference meta information. Previous ones are maintained");
                    }
                }
            }
            if (newOwners) {
                fileRefRepo.save(fileReference);
            }
        }
    }

}
