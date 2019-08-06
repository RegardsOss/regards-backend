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
package fr.cnes.regards.modules.storagelight.service.file.reference;

import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.utils.plugins.exception.NotAvailablePluginConfigurationException;
import fr.cnes.regards.modules.storagelight.dao.IFileReferenceRepository;
import fr.cnes.regards.modules.storagelight.domain.DownloadableFile;
import fr.cnes.regards.modules.storagelight.domain.FileRequestStatus;
import fr.cnes.regards.modules.storagelight.domain.database.FileLocation;
import fr.cnes.regards.modules.storagelight.domain.database.FileReference;
import fr.cnes.regards.modules.storagelight.domain.database.FileReferenceMetaInfo;
import fr.cnes.regards.modules.storagelight.domain.database.PrioritizedStorage;
import fr.cnes.regards.modules.storagelight.domain.database.StorageMonitoringAggregation;
import fr.cnes.regards.modules.storagelight.domain.database.request.FileDeletionRequest;
import fr.cnes.regards.modules.storagelight.domain.event.FileReferenceEvent;
import fr.cnes.regards.modules.storagelight.domain.flow.AddFileRefFlowItem;
import fr.cnes.regards.modules.storagelight.domain.flow.DeleteFileRefFlowItem;
import fr.cnes.regards.modules.storagelight.domain.plugin.IOnlineStorageLocation;
import fr.cnes.regards.modules.storagelight.domain.plugin.IStorageLocation;
import fr.cnes.regards.modules.storagelight.service.file.reference.flow.FileRefEventPublisher;
import fr.cnes.regards.modules.storagelight.service.storage.PrioritizedStorageService;
import fr.cnes.regards.modules.storagelight.service.storage.flow.StoragePluginConfigurationHandler;

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
 * File references can be created using AMQP messages {@link AddFileRefFlowItem}.<br/>
 * File references can be deleted using AMQP messages {@link DeleteFileRefFlowItem}.<br/>
 * File references can be copied in cache system using AMQP messages TODO<br/>
 * File references can be download using TODO<br/>
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
    private FileStorageRequestService fileRefRequestService;

    @Autowired
    private FileDeletionRequestService fileDeletionRequestService;

    @Autowired
    private NLFileReferenceService nearlineFileService;

    @Autowired
    private StoragePluginConfigurationHandler storageHandler;

    @Autowired
    private PrioritizedStorageService prioritizedStorageService;

    @Autowired
    private IPluginService pluginService;

    @Autowired
    private FileRefEventPublisher fileRefPublisher;

    @Autowired
    private EntityManager em;

    @Transactional(readOnly = true)
    public Page<FileReference> search(String storage, Pageable pageable) {
        return fileRefRepo.findByLocationStorage(storage, pageable);
    }

    @Transactional(readOnly = true)
    public Optional<FileReference> search(String storage, String checksum) {
        return fileRefRepo.findByLocationStorageAndMetaInfoChecksum(storage, checksum);
    }

    @Transactional(readOnly = true)
    public Set<FileReference> search(String checksum) {
        return fileRefRepo.findByMetaInfoChecksum(checksum);
    }

    @Transactional(readOnly = true)
    public Page<FileReference> search(Pageable pageable) {
        return fileRefRepo.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Page<FileReference> search(Specification<FileReference> spec, Pageable page) {
        return fileRefRepo.findAll(spec, page);
    }

    public Optional<FileReference> addFileReference(AddFileRefFlowItem item) {
        FileReferenceMetaInfo metaInfo = new FileReferenceMetaInfo(item.getChecksum(), item.getAlgorithm(),
                item.getFileName(), item.getFileSize(), MediaType.valueOf(item.getMimeType()));
        return this.addFileReference(Lists.newArrayList(item.getOwner()), metaInfo, item.getOrigine(),
                                     item.getDestination());
    }

    /**
     * Call {@link #addFileReference(AddFileRefFlowItem)} method for each item.
     * @param items
     */
    public void addFileReferences(List<AddFileRefFlowItem> items) {
        int flushCount = 0;
        // Retrieve already existing ones by checksum only to improve performance. The associated storage location is checked later
        Set<FileReference> existingOnes = fileRefRepo
                .findByMetaInfoChecksumIn(items.stream().map(f -> f.getChecksum()).collect(Collectors.toSet()));
        for (AddFileRefFlowItem item : items) {
            FileReferenceMetaInfo metaInfo = new FileReferenceMetaInfo(item.getChecksum(), item.getAlgorithm(),
                    item.getFileName(), item.getFileSize(), MediaType.valueOf(item.getMimeType()));

            // Check if the file already exists for the storage destination
            Optional<FileReference> oFileRef = existingOnes.stream()
                    .filter(f -> f.getMetaInfo().getChecksum().equals(item.getChecksum())
                            && f.getLocation().getStorage().contentEquals(item.getDestination().getStorage()))
                    .findFirst();

            this.addFileReference(Lists.newArrayList(item.getOwner()), metaInfo, item.getOrigine(),
                                  item.getDestination(), oFileRef);

            // Performance optimization.
            flushCount++;
            if (flushCount > 100) {
                em.flush();
                em.clear();
                flushCount = 0;
            }
        }
    }

    /**
     * <b>Method to reference a given file</b> <br/><br />
     * If the file is <b>already referenced</b> in the destination storage,
     * this method only add the requesting owner to the file reference owner list.
     * <br/>
     * If the <b>origin destination equals the destination origin</b>, so reference the file as already stored.
     *
     * @return FileReference if already exists or does not need a new storage job
     */
    public Optional<FileReference> addFileReference(Collection<String> owners, FileReferenceMetaInfo fileMetaInfo,
            FileLocation origin, FileLocation destination) {
        return this.addFileReference(owners, fileMetaInfo, origin, destination, fileRefRepo
                .findByLocationStorageAndMetaInfoChecksum(destination.getStorage(), fileMetaInfo.getChecksum()));

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
    private Optional<FileReference> addFileReference(Collection<String> owners, FileReferenceMetaInfo fileMetaInfo,
            FileLocation origin, FileLocation destination, Optional<FileReference> fileRef) {
        Assert.notNull(owners, "File must have a owner to be referenced");
        Assert.isTrue(!owners.isEmpty(), "File must have a owner to be referenced");
        Assert.notNull(fileMetaInfo, "File must have an origin location to be referenced");
        Assert.notNull(fileMetaInfo.getChecksum(), "File checksum is mandatory");
        Assert.notNull(fileMetaInfo.getAlgorithm(), "File checksum algorithm is mandatory");
        Assert.notNull(fileMetaInfo.getFileName(), "File name is mandatory");
        Assert.notNull(fileMetaInfo.getMimeType(), "File mime type is mandatory");
        Assert.notNull(fileMetaInfo.getFileSize(), "File size is mandatory");
        Assert.notNull(origin, "File must have an origin location to be referenced");
        Assert.notNull(destination, "File must have an origin location to be referenced");

        if (fileRef.isPresent()) {
            this.handleFileReferenceAlreadyExists(fileRef.get(), fileMetaInfo, origin, destination, owners);
        } else {
            // If destination equals origin location so file is already stored and can be referenced directly
            if (destination.equals(origin)) {
                return Optional.of(this.create(owners, fileMetaInfo, destination));
            } else {
                fileRefRequestService.create(owners, fileMetaInfo, origin, destination);
            }
        }
        return fileRef;
    }

    /**
     * Delete the given {@link FileReference} in database and send a AMQP {@link FileReferenceEvent} as FULLY_DELETED.
     * This method does not delete file physically.
     *
     * @param fileRef {@link FileReference} to delete.
     */
    public void delete(FileReference fileRef) {
        Assert.notNull(fileRef, "File reference to delete cannot be null");
        Assert.notNull(fileRef.getId(), "File reference identifier to delete cannot be null");
        fileRefRepo.deleteById(fileRef.getId());
        String message = String.format("File reference %s (checksum: %s) as been completly deleted for all owners.",
                                       fileRef.getMetaInfo().getFileName(), fileRef.getMetaInfo().getChecksum());
        fileRefPublisher.publishFileRefDeleted(fileRef, message);
    }

    public void delete(Collection<DeleteFileRefFlowItem> items) {
        for (DeleteFileRefFlowItem item : items) {
            try {
                removeOwner(item.getChecksum(), item.getStorage(), item.getOwner(), item.isForceDelete());
            } catch (EntityNotFoundException e) {
                LOGGER.debug(e.getMessage(), e);
            }
        }
    }

    /**
     * Download a file thanks to its checksum. If the file is stored in multiple storage location,
     * this method decide which one to retrieve by : <ul>
     *  <li>Only Files on an {@link IOnlineStorageLocation} location can be download</li>
     *  <li>Use the {@link PrioritizedStorage} configuration with the highest priority</li>
     * </ul>
     *
     * @param checksum Checksum of the file to download
     */
    @Transactional(noRollbackFor = { EntityNotFoundException.class })
    public DownloadableFile downloadFile(String checksum) throws ModuleException {
        // 1. Retrieve all the FileReference matching the given checksum
        Set<FileReference> fileRefs = this.search(checksum);
        Map<String, FileReference> storages = fileRefs.stream()
                .collect(Collectors.toMap(f -> f.getLocation().getStorage(), f -> f));
        // 2. get the storage location with the higher priority
        Optional<PrioritizedStorage> storageLocation = prioritizedStorageService
                .searchActiveHigherPriority(storages.keySet());
        if (storageLocation.isPresent()) {
            PluginConfiguration conf = storageLocation.get().getStorageConfiguration();
            FileReference fileToDownload = storages.get(conf.getLabel());
            return new DownloadableFile(downloadFileReference(fileToDownload),
                    fileToDownload.getMetaInfo().getFileSize(), fileToDownload.getMetaInfo().getFileName(),
                    fileToDownload.getMetaInfo().getMimeType());

        } else {
            throw new ModuleException(String
                    .format("No storage location configured for the given file reference (checksum %s). The file can not be download from %s.",
                            checksum, Arrays.toString(storages.keySet().toArray())));
        }
    }

    /**
     * Try to download the given {@link FileReference}.
     * @param fileToDownload
     * @return {@link InputStream} of the file
     * @throws ModuleException
     */
    @Transactional(noRollbackFor = { EntityNotFoundException.class })
    public InputStream downloadFileReference(FileReference fileToDownload) throws ModuleException {
        Optional<PrioritizedStorage> conf = prioritizedStorageService.search(fileToDownload.getLocation().getStorage());
        if (conf.isPresent()) {
            switch (conf.get().getStorageType()) {
                case NEARLINE:
                    return nearlineFileService.download(fileToDownload);
                case ONLINE:
                    return downloadOnline(fileToDownload, conf.get());
                default:
                    break;
            }
        }
        throw new ModuleException(
                String.format("Unable to download file %s (checksum : %s) as its storage location %s is not available",
                              fileToDownload.getMetaInfo().getFileName(), fileToDownload.getMetaInfo().getChecksum(),
                              fileToDownload.getLocation().toString()));
    }

    /**
     * Download a file from an ONLINE storage location.
     * @param fileToDownload
     * @param storagePluginConf
     * @return
     * @throws ModuleException
     */
    private InputStream downloadOnline(FileReference fileToDownload, PrioritizedStorage storagePluginConf)
            throws ModuleException {
        try {
            IOnlineStorageLocation plugin = pluginService
                    .getPlugin(storagePluginConf.getStorageConfiguration().getId());
            return plugin.retrieve(fileToDownload);
        } catch (NotAvailablePluginConfigurationException e) {
            throw new ModuleException(String
                    .format("Unable to download file %s (checksum : %s) as its storage location %s is not active.",
                            fileToDownload.getMetaInfo().getFileName(), fileToDownload.getMetaInfo().getChecksum(),
                            fileToDownload.getLocation().toString()));
        } catch (IOException e) {
            throw new ModuleException(e.getMessage(), e);
        }
    }

    /**
     * Remove the given owner of the to the {@link FileReference} matching the given checksum and storage location.
     * If the owner is the last one this method tries to delete file physically if the storage location is a configured {@link IStorageLocation}.
     * @param forceDelete allows to delete fileReference even if the deletion is in error.
     * @throws EntityNotFoundException
     */
    public void removeOwner(String checksum, String storage, String owner, boolean forceDelete)
            throws EntityNotFoundException {
        Assert.notNull(checksum, "Checksum is mandatory to delete a file reference");
        Assert.notNull(storage, "Storage is mandatory to delete a file reference");
        Assert.notNull(owner, "Owner is mandatory to delete a file reference");
        Optional<FileReference> oFileRef = fileRefRepo.findByLocationStorageAndMetaInfoChecksum(storage, checksum);
        if (oFileRef.isPresent()) {
            this.removeOwner(oFileRef.get(), owner, forceDelete);
        } else {
            throw new EntityNotFoundException(String
                    .format("File reference with ckesum: <%s> and storage: <%s> doest not exists", checksum, storage));
        }
    }

    /**
     * Remove the given owner of the to the given {@link FileReference}.
     * If the owner is the last one this method tries to delete file physically if the storage location is a configured {@link IStorageLocation}.
     * @param forceDelete allows to delete fileReference even if the deletion is in error.
     */
    private void removeOwner(FileReference fileReference, String owner, boolean forceDelete) {
        String message;
        if (!fileReference.getOwners().contains(owner)) {
            message = String.format("File <%s (checksum: %s)> at %s does not to belongs to %s",
                                    fileReference.getMetaInfo().getFileName(),
                                    fileReference.getMetaInfo().getChecksum(), fileReference.getLocation().toString(),
                                    owner);
        } else {
            fileReference.getOwners().remove(owner);
            message = String.format("File reference <%s (checksum: %s)> at %s does not belongs to %s anymore",
                                    fileReference.getMetaInfo().getFileName(),
                                    fileReference.getMetaInfo().getChecksum(), fileReference.getLocation().toString(),
                                    owner);
            fileRefRepo.save(fileReference);
        }
        LOGGER.debug(message);
        // Inform owners that the file reference is considered has delete for him.
        fileRefPublisher.publishFileRefDeletedForOwner(fileReference, owner, message);
        // If file reference does not belongs to anyone anymore, delete file reference
        if (fileReference.getOwners().isEmpty()) {
            if (storageHandler.getConfiguredStorages().contains(fileReference.getLocation().getStorage())) {
                // If the file is stored on an accessible storage, create a new deletion request
                fileDeletionRequestService.create(fileReference, forceDelete);
            } else {
                // Else, directly delete the file reference
                this.delete(fileReference);
            }
        }
    }

    /**
     * Creates a new {@link FileReference} with given parameters. this method does not handle physical files.
     * After success, an AMQP message {@link FileReferenceEvent} is sent with STORED state.
     *
     * @return {@link FileReference} created.
     */
    private FileReference create(Collection<String> owners, FileReferenceMetaInfo fileMetaInfo, FileLocation location) {
        FileReference fileRef = new FileReference(owners, fileMetaInfo, location);
        fileRef = fileRefRepo.save(fileRef);
        String message = String.format("New file <%s> referenced at <%s> (checksum: %s)",
                                       fileRef.getMetaInfo().getFileName(), fileRef.getLocation().toString(),
                                       fileRef.getMetaInfo().getChecksum());
        LOGGER.debug(message);
        fileRefPublisher.publishFileRefStored(fileRef, message);
        return fileRef;
    }

    /**
     * Ensure availability of given files by their checksum for download.
     * @param checksums
     * @param expirationDate availability expiration date.
     */
    public void makeAvailable(Collection<String> checksums, OffsetDateTime expirationDate) {

        Set<FileReference> onlines = Sets.newHashSet();
        Set<FileReference> nearlines = Sets.newHashSet();
        Set<FileReference> offlines = Sets.newHashSet();
        Set<FileReference> refs = fileRefRepo.findByMetaInfoChecksumIn(checksums);
        Set<String> remainingChecksums = Sets.newHashSet(checksums);

        // TODO : Check delegated security ??

        // Dispatch by storage
        ImmutableListMultimap<String, FileReference> filesByStorage = Multimaps
                .index(refs, f -> f.getLocation().getStorage());
        Set<String> remainingStorages = Sets.newHashSet(filesByStorage.keySet());

        Optional<PrioritizedStorage> storage = prioritizedStorageService.searchActiveHigherPriority(remainingStorages);
        // Handle storage by priority
        while (storage.isPresent() && !remainingStorages.isEmpty() && !remainingChecksums.isEmpty()) {
            // For each storage dispatch files in online, near line and not available
            PluginConfiguration conf = storage.get().getStorageConfiguration();
            String storageName = conf.getLabel();
            ImmutableList<FileReference> storageFiles = filesByStorage.get(storageName);
            // Calculate remaining files to make available for the current storage
            Set<FileReference> files = storageFiles.stream()
                    .filter(f -> remainingChecksums.contains(f.getMetaInfo().getChecksum()))
                    .collect(Collectors.toSet());
            switch (storage.get().getStorageType()) {
                case NEARLINE:
                    nearlines.addAll(files);
                    break;
                case ONLINE:
                    // If storage is an online one, so file is already available
                    onlines.addAll(files);
                    break;
                default:
                    // Unknown storage type
                    offlines.addAll(files);
                    break;
            }
            // Remove handled checksum by the current storage
            remainingChecksums.removeAll(storageFiles.stream().map(f -> f.getMetaInfo().getChecksum())
                    .collect(Collectors.toSet()));
            // Remove handled storage
            remainingStorages.remove(storageName);
            // Retrieve the new highest storage priority with the remaining ones
            storage = prioritizedStorageService.searchActiveHigherPriority(remainingStorages);
        }
        if (!remainingChecksums.isEmpty()) {
            // Notify files not available
            offlines.addAll(refs.stream().filter(ref -> remainingChecksums.contains(ref.getMetaInfo().getChecksum()))
                    .collect(Collectors.toSet()));
        }
        notifyAvailables(onlines);
        notifyNotAvailables(offlines);
        nearlineFileService.makeAvailable(nearlines, expirationDate);
    }

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
     */
    private Optional<FileReference> handleFileReferenceAlreadyExists(FileReference fileReference,
            FileReferenceMetaInfo newMetaInfo, FileLocation origin, FileLocation destination,
            Collection<String> owners) {
        Set<String> newOwners = Sets.newHashSet();
        FileReference updatedFileRef = null;

        Optional<FileDeletionRequest> deletionRequest = fileDeletionRequestService.search(fileReference);
        if (deletionRequest.isPresent() && deletionRequest.get().getStatus() == FileRequestStatus.PENDING) {
            // Deletion is running write now, so delay the new file reference creation with a FileReferenceRequest
            fileRefRequestService.create(owners, newMetaInfo, origin, destination, FileRequestStatus.DELAYED);
        } else {
            if (deletionRequest.isPresent()) {
                // Delete not running deletion request to add the new owner
                fileDeletionRequestService.delete(deletionRequest.get());
            }
            for (String owner : owners) {
                if (!fileReference.getOwners().contains(owner)) {
                    newOwners.add(owner);
                    fileReference.getOwners().add(owner);
                    String message = String
                            .format("New owner <%s> added to existing referenced file <%s> at <%s> (checksum: %s) ",
                                    owner, fileReference.getMetaInfo().getFileName(),
                                    fileReference.getLocation().toString(), fileReference.getMetaInfo().getChecksum());
                    LOGGER.debug(message);
                    if (!fileReference.getMetaInfo().equals(newMetaInfo)) {
                        LOGGER.warn("Existing referenced file meta information differs "
                                + "from new reference meta information. Previous ones are maintained");
                    }
                }
            }
            String message = null;
            if (!newOwners.isEmpty()) {
                updatedFileRef = fileRefRepo.save(fileReference);
                message = String.format("New owners %s added to existing referenced file %s at %s (checksum: %s)",
                                        Arrays.toString(newOwners.toArray()), fileReference.getMetaInfo().getFileName(),
                                        fileReference.getLocation().toString(),
                                        fileReference.getMetaInfo().getChecksum());
            } else {
                message = String.format("File %s already referenced at %s (checksum: %s) for owners %s",
                                        fileReference.getMetaInfo().getFileName(),
                                        fileReference.getLocation().toString(),
                                        fileReference.getMetaInfo().getChecksum(),
                                        Arrays.toString(fileReference.getOwners().toArray()));
                updatedFileRef = fileReference;
            }
            fileRefPublisher.publishFileRefStored(fileReference, message);
        }
        return Optional.ofNullable(updatedFileRef);
    }

    private void notifyAvailables(Set<FileReference> availables) {
        availables.forEach(f -> fileRefPublisher
                .publishFileRefAvailable(f.getMetaInfo().getChecksum(),
                                         String.format("file %s (checksum %s) is available for download.",
                                                       f.getMetaInfo().getFileName(), f.getMetaInfo().getChecksum())));
    }

    private void notifyNotAvailables(Set<FileReference> notAvailable) {
        notAvailable.forEach(f -> fileRefPublisher
                .publishFileRefNotAvailable(f.getMetaInfo().getChecksum(),
                                            String.format("file %s (checksum %s) is not available for download.",
                                                          f.getMetaInfo().getFileName(),
                                                          f.getMetaInfo().getChecksum())));
    }
}
