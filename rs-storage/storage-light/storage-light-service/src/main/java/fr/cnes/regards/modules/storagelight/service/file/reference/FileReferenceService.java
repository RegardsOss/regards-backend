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
import java.net.URL;
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
import fr.cnes.regards.modules.storagelight.domain.dto.FileDeletionRequestDTO;
import fr.cnes.regards.modules.storagelight.domain.dto.FileReferenceRequestDTO;
import fr.cnes.regards.modules.storagelight.domain.dto.FileStorageRequestDTO;
import fr.cnes.regards.modules.storagelight.domain.event.FileReferenceEvent;
import fr.cnes.regards.modules.storagelight.domain.flow.DeleteFileRefFlowItem;
import fr.cnes.regards.modules.storagelight.domain.flow.FileReferenceFlowItem;
import fr.cnes.regards.modules.storagelight.domain.flow.FileStorageFlowItem;
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
 * File references can be created using AMQP messages {@link FileReferenceFlowItem}.<br/>
 * File references can be deleted using AMQP messages {@link DeleteFileRefFlowItem}.<br/>
 * File references can be copied in cache system using AMQP messages TODO<br/>
 * File references can be download<br/>
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
    private FileStorageRequestService fileStorageRequestService;

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

    public void storeFiles(List<FileStorageFlowItem> items) {
        int flushCount = 0;
        // Retrieve already existing ones by checksum only to improve performance. The associated storage location is checked later
        Set<String> checksums = Sets.newHashSet();
        items.forEach(i -> {
            i.getFiles().stream().forEach(f -> checksums.add(f.getChecksum()));
        });
        Set<FileReference> existingOnes = fileRefRepo.findByMetaInfoChecksumIn(checksums);
        for (FileStorageFlowItem item : items) {
            for (FileStorageRequestDTO request : item.getFiles()) {
                // Check if the file already exists for the storage destination
                Optional<FileReference> oFileRef = existingOnes.stream()
                        .filter(f -> f.getMetaInfo().getChecksum().equals(request.getChecksum())
                                && f.getLocation().getStorage().contentEquals(request.getStorage()))
                        .findFirst();
                storeFile(request, oFileRef);
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
     * Call {@link #addFileReference(FileReferenceFlowItem)} method for each item.
     * @param items
     */
    public void referenceFiles(List<FileReferenceFlowItem> items) {
        int flushCount = 0;
        // Retrieve already existing ones by checksum only to improve performance. The associated storage location is checked later
        Set<String> checksums = Sets.newHashSet();
        items.forEach(i -> {
            i.getFiles().stream().forEach(f -> checksums.add(f.getChecksum()));
        });
        Set<FileReference> existingOnes = fileRefRepo.findByMetaInfoChecksumIn(checksums);
        for (FileReferenceFlowItem item : items) {
            for (FileReferenceRequestDTO file : item.getFiles()) {
                // Check if the file already exists for the storage destination
                Optional<FileReference> oFileRef = existingOnes.stream()
                        .filter(f -> f.getMetaInfo().getChecksum().equals(file.getChecksum())
                                && f.getLocation().getStorage().contentEquals(file.getStorage()))
                        .findFirst();
                referenceFile(file, oFileRef);
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

    public Optional<FileReference> referenceFile(String owner, FileReferenceMetaInfo metaInfo, FileLocation location) {
        Optional<FileReference> oFileRef = fileRefRepo.findByLocationStorageAndMetaInfoChecksum(location.getStorage(),
                                                                                                metaInfo.getChecksum());
        return referenceFile(FileReferenceRequestDTO
                .build(metaInfo.getFileName(), metaInfo.getChecksum(), metaInfo.getAlgorithm(),
                       metaInfo.getMimeType().toString(), metaInfo.getFileSize(), owner, location.getStorage(),
                       location.getUrl())
                .withHeight(metaInfo.getHeight()).withWidth(metaInfo.getWidth()).withType(metaInfo.getType()),
                             oFileRef);
    }

    private Optional<FileReference> referenceFile(FileReferenceRequestDTO request, Optional<FileReference> fileRef) {
        if (fileRef.isPresent()) {
            return handleAlreadyExists(fileRef.get(), request);
        } else {
            return Optional.of(create(Lists.newArrayList(request.getOwner()), request.buildMetaInfo(),
                                      new FileLocation(request.getStorage(), request.getUrl())));
        }
    }

    public Optional<FileReference> storeFile(String owner, FileReferenceMetaInfo metaInfo, URL originUrl,
            String storage, Optional<String> subDirectory) {
        Optional<FileReference> oFileRef = fileRefRepo.findByLocationStorageAndMetaInfoChecksum(storage,
                                                                                                metaInfo.getChecksum());
        return storeFile(FileStorageRequestDTO.build(metaInfo.getFileName(), metaInfo.getChecksum(),
                                                     metaInfo.getAlgorithm(), metaInfo.getMimeType().toString(), owner,
                                                     originUrl, storage, subDirectory),
                         oFileRef);
    }

    private Optional<FileReference> storeFile(FileStorageRequestDTO request, Optional<FileReference> fileRef) {
        if (fileRef.isPresent()) {
            return handleAlreadyExists(fileRef.get(), request);
        } else {
            fileStorageRequestService.create(request.getOwner(), request.buildMetaInfo(), request.getOriginUrl(),
                                             request.getStorage(), request.getSubDirectory());
            return Optional.empty();
        }
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

    /**
     * Handle the given {@link DeleteFileRefFlowItem}s.
     * @param items
     */
    public void delete(Collection<DeleteFileRefFlowItem> items) {
        long start = System.currentTimeMillis();
        Set<String> checksums = Sets.newHashSet();
        items.forEach(i -> {
            i.getFiles().stream().forEach(f -> checksums.add(f.getChecksum()));
        });
        Set<FileReference> existingOnes = fileRefRepo.findByMetaInfoChecksumIn(checksums);
        for (DeleteFileRefFlowItem item : items) {
            for (FileDeletionRequestDTO request : item.getFiles()) {
                Optional<FileReference> oFileRef = existingOnes.stream()
                        .filter(f -> f.getLocation().getStorage().contentEquals(request.getStorage())).findFirst();
                if (oFileRef.isPresent()) {
                    removeOwner(oFileRef.get(), request.getOwner(), request.isForceDelete());
                }
            }
        }
        LOGGER.debug("...deletion of {} refs handled in {} ms", items.size(), System.currentTimeMillis() - start);
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
        Set<FileReference> fileRefs = search(checksum);
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
                    .getPlugin(storagePluginConf.getStorageConfiguration().getBusinessId());
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
            removeOwner(oFileRef.get(), owner, forceDelete);
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
                delete(fileReference);
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
    private Optional<FileReference> handleAlreadyExists(FileReference fileReference, FileReferenceRequestDTO request) {
        FileReference updatedFileRef = null;
        Optional<FileDeletionRequest> deletionRequest = fileDeletionRequestService.search(fileReference);
        if (deletionRequest.isPresent() && (deletionRequest.get().getStatus() == FileRequestStatus.PENDING)) {
            // A deletion is pending on the existing file reference but the new reference request does not indicates the new file location
            String message = String.format("File is being deleted. Please try later.");
            // TODO publish request denied
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
                updatedFileRef = fileRefRepo.save(fileReference);
                fileRefPublisher.publishFileRefStored(updatedFileRef, message);
                LOGGER.debug(message);
            } else {
                String message = String.format("File %s already referenced at %s (checksum: %s) for owners %s",
                                               fileReference.getMetaInfo().getFileName(),
                                               fileReference.getLocation().toString(),
                                               fileReference.getMetaInfo().getChecksum(),
                                               Arrays.toString(fileReference.getOwners().toArray()));
                updatedFileRef = fileReference;
                fileRefPublisher.publishFileRefStored(fileReference, message);
                LOGGER.debug(message);
            }
        }
        return Optional.ofNullable(updatedFileRef);
    }

    private Optional<FileReference> handleAlreadyExists(FileReference fileReference, FileStorageRequestDTO request) {
        FileReference updatedFileRef = null;
        FileReferenceMetaInfo newMetaInfo = request.buildMetaInfo();
        Optional<FileDeletionRequest> deletionRequest = fileDeletionRequestService.search(fileReference);
        if (deletionRequest.isPresent() && (deletionRequest.get().getStatus() == FileRequestStatus.PENDING)) {
            // Deletion is running write now, so delay the new file reference creation with a FileReferenceRequest
            fileStorageRequestService.create(request.getOwner(), newMetaInfo, request.getOriginUrl(),
                                             request.getStorage(), request.getSubDirectory(),
                                             FileRequestStatus.DELAYED);
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
                fileRefPublisher.publishFileRefStored(fileReference, message);
            } else {
                String message = String.format("File %s already referenced at %s (checksum: %s) for owners %s",
                                               fileReference.getMetaInfo().getFileName(),
                                               fileReference.getLocation().toString(),
                                               fileReference.getMetaInfo().getChecksum(),
                                               Arrays.toString(fileReference.getOwners().toArray()));
                updatedFileRef = fileReference;
                fileRefPublisher.publishFileRefStored(fileReference, message);
            }
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
