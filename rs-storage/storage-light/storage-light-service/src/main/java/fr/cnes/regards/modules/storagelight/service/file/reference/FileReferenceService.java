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
import fr.cnes.regards.modules.storagelight.domain.database.FileLocation;
import fr.cnes.regards.modules.storagelight.domain.database.FileReference;
import fr.cnes.regards.modules.storagelight.domain.database.FileReferenceMetaInfo;
import fr.cnes.regards.modules.storagelight.domain.database.PrioritizedStorage;
import fr.cnes.regards.modules.storagelight.domain.database.StorageMonitoringAggregation;
import fr.cnes.regards.modules.storagelight.domain.database.request.FileDeletionRequest;
import fr.cnes.regards.modules.storagelight.domain.database.request.FileRequestStatus;
import fr.cnes.regards.modules.storagelight.domain.database.request.FileStorageRequest;
import fr.cnes.regards.modules.storagelight.domain.dto.FileCopyRequestDTO;
import fr.cnes.regards.modules.storagelight.domain.dto.FileDeletionRequestDTO;
import fr.cnes.regards.modules.storagelight.domain.dto.FileReferenceRequestDTO;
import fr.cnes.regards.modules.storagelight.domain.dto.FileStorageRequestDTO;
import fr.cnes.regards.modules.storagelight.domain.event.FileReferenceEvent;
import fr.cnes.regards.modules.storagelight.domain.event.FileRequestEvent.ErrorFile;
import fr.cnes.regards.modules.storagelight.domain.event.FileRequestType;
import fr.cnes.regards.modules.storagelight.domain.flow.CopyFlowItem;
import fr.cnes.regards.modules.storagelight.domain.flow.DeletionFlowItem;
import fr.cnes.regards.modules.storagelight.domain.flow.ReferenceFlowItem;
import fr.cnes.regards.modules.storagelight.domain.flow.StorageFlowItem;
import fr.cnes.regards.modules.storagelight.domain.plugin.IOnlineStorageLocation;
import fr.cnes.regards.modules.storagelight.domain.plugin.IStorageLocation;
import fr.cnes.regards.modules.storagelight.service.file.reference.flow.FileReferenceEventPublisher;
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
 * File references can be created using AMQP messages {@link ReferenceFlowItem}.<br/>
 * File references can be deleted using AMQP messages {@link DeletionFlowItem}.<br/>
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
    private FileReferenceEventPublisher eventPublisher;

    @Autowired
    private FileStorageRequestService fileStorageRequestService;

    @Autowired
    private FileDeletionRequestService fileDeletionRequestService;

    @Autowired
    private FileCopyRequestService fileCopyRequestService;

    @Autowired
    private NLFileReferenceService nearlineFileService;

    @Autowired
    private StoragePluginConfigurationHandler storageHandler;

    @Autowired
    private PrioritizedStorageService prioritizedStorageService;

    @Autowired
    private IPluginService pluginService;

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

    /**
     * Handle many {@link FileCopyRequestDTO} to copy files to a given storage location.
     * @param files copy requests
     * @param requestId business request identifier
     */
    public void copy(Collection<FileCopyRequestDTO> files, String requestId) {
        files.forEach(f -> {
            fileCopyRequestService.create(f, requestId);
            eventPublisher.requestGranted(requestId, FileRequestType.COPY);
        });
    }

    /**
     * Handle many {@link CopyFlowItem} to copy files to a given storage location.
     * @param items copy flow items
     */
    public void copy(Collection<CopyFlowItem> items) {
        items.forEach(i -> copy(i.getFiles(), i.getRequestId()));
    }

    /**
     * Handle many {@link StorageFlowItem} to store files to a given storage location.
     * @param items storage flow items
     */
    public void storeFiles(Collection<StorageFlowItem> items) {
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
                storeFile(request, oFileRef, item.getRequestId());
                // Performance optimization.
                flushCount++;
                if (flushCount > 100) {
                    em.flush();
                    em.clear();
                    flushCount = 0;
                }
            }
            eventPublisher.requestGranted(item.getRequestId(), FileRequestType.STORAGE);
        }
    }

    /**
     * Store a new file to a given storage destination
     * @param owner Owner of the new file
     * @param metaInfo information about file
     * @param originUrl current location of file. This URL must be locally accessible to be copied.
     * @param storage name of the storage destination. Must be a existing plugin configuration of a {@link IStorageLocation}
     * @param subDirectory where to store file in the destination location.
     * @param requestId business request identifier
     * @return {@link FileReference} if the file is already referenced.
     */
    public Optional<FileReference> storeFile(String owner, FileReferenceMetaInfo metaInfo, URL originUrl,
            String storage, Optional<String> subDirectory, String requestId) {
        Optional<FileReference> oFileRef = fileRefRepo.findByLocationStorageAndMetaInfoChecksum(storage,
                                                                                                metaInfo.getChecksum());
        return storeFile(FileStorageRequestDTO.build(metaInfo.getFileName(), metaInfo.getChecksum(),
                                                     metaInfo.getAlgorithm(), metaInfo.getMimeType().toString(), owner,
                                                     originUrl, storage, subDirectory),
                         oFileRef, requestId);
    }

    /**
     * Store a new file to a given storage destination
     * @param request {@link FileStorageRequestDTO} info about file to store
     * @param fileRef {@link FileReference} of associated file if already exists
     * @param requestId business request indentifier
     * @return {@link FileReference} if the file is already referenced.
     */
    private Optional<FileReference> storeFile(FileStorageRequestDTO request, Optional<FileReference> fileRef,
            String requestId) {
        if (fileRef.isPresent()) {
            return handleAlreadyExists(fileRef.get(), request, requestId);
        } else {
            fileStorageRequestService.create(Sets.newHashSet(request.getOwner()), request.buildMetaInfo(),
                                             request.getOriginUrl(), request.getStorage(), request.getSubDirectory(),
                                             requestId);
            return Optional.empty();
        }
    }

    /**
     * Call {@link #addFileReference(ReferenceFlowItem)} method for each item.
     * @param items
     */
    public void referenceFiles(List<ReferenceFlowItem> items) {
        int flushCount = 0;
        // Retrieve already existing ones by checksum only to improve performance. The associated storage location is checked later
        Set<String> checksums = Sets.newHashSet();
        items.forEach(i -> {
            i.getFiles().stream().forEach(f -> checksums.add(f.getChecksum()));
        });
        Set<FileReference> existingOnes = fileRefRepo.findByMetaInfoChecksumIn(checksums);
        for (ReferenceFlowItem item : items) {
            Set<ErrorFile> errors = Sets.newHashSet();
            for (FileReferenceRequestDTO file : item.getFiles()) {
                // Check if the file already exists for the storage destination
                Optional<FileReference> oFileRef = existingOnes.stream()
                        .filter(f -> f.getMetaInfo().getChecksum().equals(file.getChecksum())
                                && f.getLocation().getStorage().contentEquals(file.getStorage()))
                        .findFirst();
                try {
                    FileReference fileRef = referenceFile(file, oFileRef, Sets.newHashSet(item.getRequestId()));
                    String message = String.format("New file <%s> referenced at <%s> (checksum: %s)",
                                                   fileRef.getMetaInfo().getFileName(),
                                                   fileRef.getLocation().toString(),
                                                   fileRef.getMetaInfo().getChecksum());
                    LOGGER.debug(message);
                    eventPublisher.storeSuccess(fileRef, message, item.getRequestId());
                } catch (ModuleException e) {
                    eventPublisher.storeError(file.getChecksum(), Sets.newHashSet(file.getOwner()), file.getStorage(),
                                              e.getMessage(), item.getRequestId());
                    errors.add(ErrorFile.build(file.getChecksum(), file.getStorage(), e.getMessage()));
                }
                // Performance optimization.
                flushCount++;
                if (flushCount > 100) {
                    em.flush();
                    em.clear();
                    flushCount = 0;
                }
            }
            eventPublisher.requestGranted(item.getRequestId(), FileRequestType.REFERENCE);
            if (errors.isEmpty()) {
                eventPublisher.requestDone(item.getRequestId(), FileRequestType.REFERENCE);
            } else {
                eventPublisher.requestError(item.getRequestId(), FileRequestType.REFERENCE, errors);
            }
        }
    }

    /**
     * Reference a new file. No file movement is made here. File is only referenced.
     * @param owner Owner of the new {@link FileReference}
     * @param metaInfo information about file
     * @param location location of file
     * @param requestIds Business requests identifiers associated to the new file reference.
     * @return {@link FileReference}
     * @throws ModuleException if the file reference can not be created.
     */
    public FileReference referenceFile(String owner, FileReferenceMetaInfo metaInfo, FileLocation location,
            Collection<String> requestIds) throws ModuleException {
        Optional<FileReference> oFileRef = fileRefRepo.findByLocationStorageAndMetaInfoChecksum(location.getStorage(),
                                                                                                metaInfo.getChecksum());
        return referenceFile(FileReferenceRequestDTO
                .build(metaInfo.getFileName(), metaInfo.getChecksum(), metaInfo.getAlgorithm(),
                       metaInfo.getMimeType().toString(), metaInfo.getFileSize(), owner, location.getStorage(),
                       location.getUrl())
                .withHeight(metaInfo.getHeight()).withWidth(metaInfo.getWidth()).withType(metaInfo.getType()), oFileRef,
                             requestIds);
    }

    /**
     * Reference a new file. No file movement is made here. File is only referenced.
     * @param request {@link FileReferenceRequestDTO}
     * @param fileRef {@link FileReference} of associated file if already exists
     * @param requestIds Business requests identifiers associated to the new file reference.
     * @return {@link FileReference}
     * @throws ModuleException if the file reference can not be created.
     */
    private FileReference referenceFile(FileReferenceRequestDTO request, Optional<FileReference> fileRef,
            Collection<String> requestIds) throws ModuleException {
        if (fileRef.isPresent()) {
            return handleAlreadyExists(fileRef.get(), request, requestIds);
        } else {
            return create(Lists.newArrayList(request.getOwner()), request.buildMetaInfo(),
                          new FileLocation(request.getStorage(), request.getUrl()), requestIds);
        }
    }

    /**
     * Delete the given {@link FileReference} in database and send a AMQP {@link FileReferenceEvent} as FULLY_DELETED.
     * This method does not delete file physically.
     *
     * @param fileRef {@link FileReference} to delete.
     * @param deletion request businness identifier
     */
    public void delete(FileReference fileRef, String requestId) {
        Assert.notNull(fileRef, "File reference to delete cannot be null");
        Assert.notNull(fileRef.getId(), "File reference identifier to delete cannot be null");
        fileRefRepo.deleteById(fileRef.getId());
        String message = String.format("File reference %s (checksum: %s) as been completly deleted for all owners.",
                                       fileRef.getMetaInfo().getFileName(), fileRef.getMetaInfo().getChecksum());
        eventPublisher.deletionSuccess(fileRef, message, requestId);
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
                    removeOwner(oFileRef.get(), request.getOwner(), request.isForceDelete(), item.getRequestId());
                }
            }
            eventPublisher.requestGranted(item.getRequestId(), FileRequestType.DELETION);
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
            LOGGER.error(e.getMessage(), e);
            throw new ModuleException(e.getMessage(), e);
        }
    }

    /**
     * Remove the given owner of the to the {@link FileReference} matching the given checksum and storage location.
     * If the owner is the last one this method tries to delete file physically if the storage location is a configured {@link IStorageLocation}.
     * @param forceDelete allows to delete fileReference even if the deletion is in error.
     * @param requestId Business identifier of the deletion request
     * @throws EntityNotFoundException
     */
    public void removeOwner(String checksum, String storage, String owner, boolean forceDelete, String requestId)
            throws EntityNotFoundException {
        Assert.notNull(checksum, "Checksum is mandatory to delete a file reference");
        Assert.notNull(storage, "Storage is mandatory to delete a file reference");
        Assert.notNull(owner, "Owner is mandatory to delete a file reference");
        Optional<FileReference> oFileRef = fileRefRepo.findByLocationStorageAndMetaInfoChecksum(storage, checksum);
        if (oFileRef.isPresent()) {
            removeOwner(oFileRef.get(), owner, forceDelete, requestId);
        } else {
            throw new EntityNotFoundException(String
                    .format("File reference with ckesum: <%s> and storage: <%s> doest not exists", checksum, storage));
        }
    }

    /**
     * Remove the given owner of the to the given {@link FileReference}.
     * If the owner is the last one this method tries to delete file physically if the storage location is a configured {@link IStorageLocation}.
     * @param forceDelete allows to delete fileReference even if the deletion is in error.
     * @param requestId Business identifier of the deletion request
     */
    private void removeOwner(FileReference fileReference, String owner, boolean forceDelete, String requestId) {
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
        eventPublisher.deletionForOwnerSuccess(fileReference, owner, message, requestId);
        // If file reference does not belongs to anyone anymore, delete file reference
        if (fileReference.getOwners().isEmpty()) {
            if (storageHandler.getConfiguredStorages().contains(fileReference.getLocation().getStorage())) {
                // If the file is stored on an accessible storage, create a new deletion request
                fileDeletionRequestService.create(fileReference, forceDelete, requestId);
            } else {
                // Else, directly delete the file reference
                delete(fileReference, requestId);
            }
        }
        // Sessions are always successful as files are only deleted from database. Error during physical deletion are handled
        // by file in deletion requests.
        eventPublisher.requestDone(requestId, FileRequestType.DELETION);
    }

    /**
     * Creates a new {@link FileReference} with given parameters. this method does not handle physical files.
     * After success, an AMQP message {@link FileReferenceEvent} is sent with STORED state.
     *
     * @param owners new file owners
     * @param fileMetaInfo file information
     * @param location file location
     * @param requestIds business request identifiers
     * @return {@link FileReference} created.
     */
    private FileReference create(Collection<String> owners, FileReferenceMetaInfo fileMetaInfo, FileLocation location,
            Collection<String> requestIds) {
        FileReference fileRef = new FileReference(owners, fileMetaInfo, location);
        fileRef = fileRefRepo.save(fileRef);
        return fileRef;
    }

    /**
     * Ensure availability of given files by their checksum for download.
     * @param checksums
     * @param expirationDate availability expiration date.
     */
    public void makeAvailable(Collection<String> checksums, OffsetDateTime expirationDate, String requestId) {

        Set<FileReference> onlines = Sets.newHashSet();
        Set<FileReference> offlines = Sets.newHashSet();
        Set<FileReference> nearlines = Sets.newHashSet();
        Set<FileReference> refs = fileRefRepo.findByMetaInfoChecksumIn(checksums);
        Set<String> remainingChecksums = Sets.newHashSet(checksums);

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
            // add unknown to offline files
            offlines.addAll(refs.stream().filter(ref -> remainingChecksums.contains(ref.getMetaInfo().getChecksum()))
                    .collect(Collectors.toSet()));
        }
        notifyAvailables(onlines, requestId);
        notifyNotAvailables(offlines, requestId);
        // Hack to handle offline errors as request errors to be notified in request ERROR notification.
        nearlines.addAll(offlines);
        int nbRequests = nearlineFileService.makeAvailable(nearlines, expirationDate, requestId);
        eventPublisher.requestGranted(requestId, FileRequestType.AVAILABILITY);
        // If no cache requests are needed, we have to notify the end of the availability request
        if (nearlines.isEmpty() || (nbRequests == 0)) {
            eventPublisher.requestDone(requestId, FileRequestType.AVAILABILITY);
        }
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
     * @throws ModuleException If file reference can not be created
     */
    private FileReference handleAlreadyExists(FileReference fileReference, FileReferenceRequestDTO request,
            Collection<String> requestIds) throws ModuleException {
        Optional<FileDeletionRequest> deletionRequest = fileDeletionRequestService.search(fileReference);
        if (deletionRequest.isPresent() && (deletionRequest.get().getStatus() == FileRequestStatus.PENDING)) {
            // A deletion is pending on the existing file reference but the new reference request does not indicates the new file location
            String message = String.format("File %s is being deleted. Please try later.", request.getChecksum());
            requestIds
                    .forEach(id -> eventPublisher.storeError(request.getChecksum(), Sets.newHashSet(request.getOwner()),
                                                             request.getStorage(), message, id));
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
                eventPublisher.storeSuccess(fileReference, message, requestIds);
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
     * @param requestId new business request identifier
     * @return {@link FileReference} updated or null.
     */
    private Optional<FileReference> handleAlreadyExists(FileReference fileReference, FileStorageRequestDTO request,
            String requestId) {
        FileReference updatedFileRef = null;
        FileReferenceMetaInfo newMetaInfo = request.buildMetaInfo();
        Optional<FileDeletionRequest> deletionRequest = fileDeletionRequestService.search(fileReference);
        if (deletionRequest.isPresent() && (deletionRequest.get().getStatus() == FileRequestStatus.PENDING)) {
            // Deletion is running write now, so delay the new file reference creation with a FileReferenceRequest
            fileStorageRequestService.create(Sets.newHashSet(request.getOwner()), newMetaInfo, request.getOriginUrl(),
                                             request.getStorage(), request.getSubDirectory(), FileRequestStatus.DELAYED,
                                             requestId);
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
                eventPublisher.storeSuccess(fileReference, message, requestId);
            } else {
                String message = String.format("File %s already referenced at %s (checksum: %s) for owners %s",
                                               fileReference.getMetaInfo().getFileName(),
                                               fileReference.getLocation().toString(),
                                               fileReference.getMetaInfo().getChecksum(),
                                               Arrays.toString(fileReference.getOwners().toArray()));
                updatedFileRef = fileReference;
                eventPublisher.storeSuccess(fileReference, message, requestId);
            }
        }
        return Optional.ofNullable(updatedFileRef);
    }

    /**
     * Send {@link FileReferenceEvent} for available given files.
     * @param availables newly available files
     * @param availabilityRequestId business request identifier of the availability request associated.
     */
    private void notifyAvailables(Set<FileReference> availables, String availabilityRequestId) {
        availables.forEach(f -> eventPublisher
                .available(f.getMetaInfo().getChecksum(), f.getLocation().getStorage(), f.getLocation().getUrl(),
                           f.getOwners(),
                           String.format("file %s (checksum %s) is available for download.",
                                         f.getMetaInfo().getFileName(), f.getMetaInfo().getChecksum()),
                           availabilityRequestId, false));
    }

    /**
     * Send {@link FileReferenceEvent} for not available given files.
     * @param availables newly available files
     * @param availabilityRequestId business request identifier of the availability request associated.
     */
    private void notifyNotAvailables(Set<FileReference> notAvailable, String requestId) {
        notAvailable.forEach(f -> eventPublisher
                .notAvailable(f.getMetaInfo().getChecksum(),
                              String.format("file %s (checksum %s) is not available for download.",
                                            f.getMetaInfo().getFileName(), f.getMetaInfo().getChecksum()),
                              requestId, false));
    }
}
