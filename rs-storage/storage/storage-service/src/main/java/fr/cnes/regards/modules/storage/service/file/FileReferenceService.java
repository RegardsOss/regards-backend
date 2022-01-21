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
package fr.cnes.regards.modules.storage.service.file;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.modules.storage.dao.IFileReferenceRepository;
import fr.cnes.regards.modules.storage.dao.IFileReferenceWithOwnersRepository;
import fr.cnes.regards.modules.storage.domain.database.FileLocation;
import fr.cnes.regards.modules.storage.domain.database.FileReference;
import fr.cnes.regards.modules.storage.domain.database.FileReferenceMetaInfo;
import fr.cnes.regards.modules.storage.domain.database.StorageMonitoringAggregation;
import fr.cnes.regards.modules.storage.domain.event.FileReferenceEvent;
import fr.cnes.regards.modules.storage.service.file.request.RequestsGroupService;
import fr.cnes.regards.modules.storage.service.session.SessionNotifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

/**
 * Service to handle actions on {@link FileReference}s entities.
 *
 * @author Sébastien Binda
 */
@Service
@MultitenantTransactional
public class FileReferenceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileReferenceService.class);

    @Autowired
    private IFileReferenceRepository fileRefRepo;

    @Autowired
    private IFileReferenceWithOwnersRepository fileRefWithOwnersRepo;

    @Autowired
    private RequestsGroupService requInfoService;

    @Autowired
    private FileReferenceEventPublisher fileRefEventPublisher;

    @Autowired
    private SessionNotifier sessionNotifier;

    /**
     * Calculate the total file size by adding fileSize of each {@link FileReference} with an id over the given id.
     */
    public Collection<StorageMonitoringAggregation> aggragateFilesSizePerStorage(Long lastReferencedFileId) {
        if (lastReferencedFileId != null) {
            return fileRefRepo.getTotalFileSizeAggregation(lastReferencedFileId);
        } else {
            return fileRefRepo.getTotalFileSizeAggregation();
        }
    }

    /**
     * Creates a new {@link FileReference} with given parameters. this method does not handle physical files.
     * After success, an AMQP message {@link FileReferenceEvent} is sent with STORED state.
     *
     * @param owners new file owners
     * @param fileMetaInfo file information
     * @param location file location
     */
    public FileReference create(Collection<String> owners, FileReferenceMetaInfo fileMetaInfo, FileLocation location,
     boolean isReferenced) {
        FileReference fileRef = new FileReference(owners, fileMetaInfo, location);
        // set referenced to true if the file is not stored physically
        if(isReferenced) {
            fileRef.setReferenced(true);
        }
        fileRef = fileRefRepo.save(fileRef);
        return fileRef;
    }

    /**
     * Delete the given {@link FileReference} in database and send a AMQP {@link FileReferenceEvent} as FULLY_DELETED.
     * This method does not delete file physically.
     *  @param fileRef {@link FileReference} to delete.
     * @param groupId request business identifier
     * @param sessionOwner source of data
     * @param session data management session
     */
    public void delete(FileReference fileRef, String groupId, String sessionOwner, String session) {
        Assert.notNull(fileRef, "File reference to delete cannot be null");
        Assert.notNull(fileRef.getId(), "File reference identifier to delete cannot be null");

        // Check if there is request information associated
        requInfoService.deleteRequestInfoForFile(fileRef.getId());
        fileRefRepo.delete(fileRef);
        String message = String.format("File reference %s (checksum: %s) as been completly deleted for all owners.",
                                       fileRef.getMetaInfo().getFileName(), fileRef.getMetaInfo().getChecksum());
        fileRefEventPublisher.deletionSuccess(fileRef, message, groupId);

        // Decrement the number of running requests to the session agent
        this.sessionNotifier.decrementRunningRequests(sessionOwner, session);
        // Notify successfully deleted file
        this.sessionNotifier.notifyDeletedFiles(sessionOwner, session, fileRef.isReferenced());
    }

    /**
     * Remove given owner form the given fileReference.
     * @param fileReference
     * @param owner
     */
    public void removeOwner(FileReference fileReference, String owner, String groupId) {
        String message;
        if (!fileRefRepo.isOwnedBy(fileReference.getId(), owner)) {
            message = String.format("File <%s (checksum: %s)> at %s does not to belongs to %s",
                                    fileReference.getMetaInfo().getFileName(),
                                    fileReference.getMetaInfo().getChecksum(), fileReference.getLocation().toString(),
                                    owner);
        } else {
            fileRefRepo.removeOwner(fileReference.getId(), owner);
            message = String.format("File reference <%s (checksum: %s)> at %s does not belongs to %s anymore",
                                    fileReference.getMetaInfo().getFileName(),
                                    fileReference.getMetaInfo().getChecksum(), fileReference.getLocation().toString(),
                                    owner);
        }
        LOGGER.trace(message);
        fileRefEventPublisher.deletionForOwnerSuccess(fileReference, owner, message, groupId);
    }

    /**
     * Search for all {@link FileReference}s associated to the given storage location.
     * @param storage
     * @param pageable
     * @return {@link FileReference}s
     */
    @Transactional(readOnly = true)
    public Page<FileReference> search(String storage, Pageable pageable) {
        return fileRefRepo.findByLocationStorage(storage, pageable);
    }

    public Collection<String> getOwners(Long fileRefId) {
        return fileRefRepo.findOwnersById(fileRefId);
    }

    /**
     * Search for all {@link FileReference}s associated to the given storage location.
     * @param storage
     * @param pageable
     * @return {@link FileReference}s
     */
    @Transactional(readOnly = true)
    public Page<FileReference> search(String storage, Collection<String> types, Pageable pageable) {
        return fileRefRepo.findByLocationStorageAndMetaInfoTypeIn(storage, types, pageable);
    }

    /**
     * Search for a {@link FileReference} associated to the given storage location and matching the given checksum.
     */
    @Transactional(readOnly = true)
    public Optional<FileReference> search(String storage, String checksum) {
        return fileRefRepo.findByLocationStorageAndMetaInfoChecksum(storage, checksum);
    }

    /**
     * Search for all {@link FileReference}s with matching checksums on the given storage location.
     * @param storage
     * @param checksums
     * @return {@link FileReference}s
     */
    @Transactional(readOnly = true)
    public Set<FileReference> search(String storage, Collection<String> checksums) {
        return fileRefRepo.findByLocationStorageAndMetaInfoChecksumIn(storage, checksums);
    }

    @Transactional(readOnly = true)
    public Optional<FileReference> searchWithOwners(String storage, String checksum) {
        return fileRefWithOwnersRepo.findByLocationStorageAndMetaInfoChecksum(storage, checksum);
    }

    /**
     * Search for all {@link FileReference}s associated to the given checksums.
     * @param checksums
     * @return {@link FileReference}s
     */
    @Transactional(readOnly = true)
    public Set<FileReference> search(Collection<String> checksums) {
        return fileRefRepo.findByMetaInfoChecksumIn(checksums);
    }

    /**
     * Search for all {@link FileReference}s associated to the given checksum.
     * @return {@link FileReference}s
     */
    @Transactional(readOnly = true)
    public Set<FileReference> search(String checksum) {
        return fileRefRepo.findByMetaInfoChecksum(checksum);
    }

    /**
     * Search for all {@link FileReference}s.
     * @param pageable
     * @return {@link FileReference}s
     */
    @Transactional(readOnly = true)
    public Page<FileReference> search(Pageable pageable) {
        return fileRefRepo.findAll(pageable);
    }

    /**
     * Search for all {@link FileReference}s matching the given criterion.
     * @param spec criterion
     * @param page
     * @return {@link FileReference}s
     */
    @Transactional(readOnly = true)
    public Page<FileReference> search(Specification<FileReference> spec, Pageable page) {
        return fileRefRepo.findAll(spec, page);
    }

    /**
     * Update the given fileReference
     * @param updatedFile
     */
    public FileReference update(String checksum, String storage, FileReference updatedFile) {
        Assert.notNull(updatedFile, "File reference to update can not be null");
        Assert.notNull(updatedFile.getId(), "File reference id to update can not be null");
        FileReference saved = fileRefRepo.save(updatedFile);
        fileRefEventPublisher.updated(checksum, storage, saved);
        return saved;
    }

    /**
     * @param id
     * @param owner
     */
    public void addOwner(Long id, String owner) {
        if (!fileRefRepo.isOwnedBy(id, owner)) {
            fileRefRepo.addOwner(id, owner);
        }
    }

    public boolean hasOwner(Long id) {
        return fileRefRepo.hasOwner(id);
    }

    /**
     * @param storage
     * @param pageable
     * @return
     */
    public Page<FileReference> searchWithOwners(String storage, Pageable pageable) {
        return fileRefWithOwnersRepo.findAllByLocationStorage(storage, pageable);
    }

    public Page<FileReference> searchWithOwners(Pageable pageable) {
        return fileRefWithOwnersRepo.findAll(pageable);
    }

}
