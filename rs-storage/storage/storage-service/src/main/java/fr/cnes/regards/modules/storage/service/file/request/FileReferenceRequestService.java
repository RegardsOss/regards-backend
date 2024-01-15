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
package fr.cnes.regards.modules.storage.service.file.request;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.module.validation.ErrorTranslator;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.utils.plugins.exception.NotAvailablePluginConfigurationException;
import fr.cnes.regards.modules.fileaccess.plugin.domain.FileReferenceResultStatusEnum;
import fr.cnes.regards.modules.fileaccess.plugin.domain.IStorageLocation;
import fr.cnes.regards.modules.filecatalog.amqp.input.FilesReferenceEvent;
import fr.cnes.regards.modules.filecatalog.amqp.output.FileReferenceEvent;
import fr.cnes.regards.modules.filecatalog.dto.FileRequestStatus;
import fr.cnes.regards.modules.filecatalog.dto.FileRequestType;
import fr.cnes.regards.modules.filecatalog.dto.request.FileReferenceRequestDto;
import fr.cnes.regards.modules.storage.dao.IFileReferenceRepository;
import fr.cnes.regards.modules.storage.domain.FileReferenceResult;
import fr.cnes.regards.modules.storage.domain.database.FileLocation;
import fr.cnes.regards.modules.storage.domain.database.FileReference;
import fr.cnes.regards.modules.storage.domain.database.FileReferenceMetaInfo;
import fr.cnes.regards.modules.storage.domain.database.request.FileDeletionRequest;
import fr.cnes.regards.modules.storage.service.file.FileReferenceEventPublisher;
import fr.cnes.regards.modules.storage.service.file.FileReferenceService;
import fr.cnes.regards.modules.storage.service.location.StoragePluginConfigurationHandler;
import fr.cnes.regards.modules.storage.service.session.SessionNotifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service to handle File reference requests.
 *
 * @author Sébastien Binda
 */
@Service
@MultitenantTransactional
public class FileReferenceRequestService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileReferenceRequestService.class);

    private final FileReferenceEventPublisher fileRefEventPublisher;

    private final RequestsGroupService reqGrpService;

    private final FileDeletionRequestService fileDeletionRequestService;

    private final FileReferenceService fileRefService;

    private final Validator validator;

    private final IPluginService pluginService;

    private final StoragePluginConfigurationHandler storagePluginConfHandler;

    private final SessionNotifier sessionNotifier;

    @Value("${regards.storage.reference.requests.days.before.expiration:5}")
    private Integer nbDaysBeforeExpiration;

    public FileReferenceRequestService(FileReferenceEventPublisher fileRefEventPublisher,
                                       RequestsGroupService reqGrpService,
                                       FileDeletionRequestService fileDeletionRequestService,
                                       FileReferenceService fileRefService,
                                       Validator validator,
                                       IPluginService pluginService,
                                       StoragePluginConfigurationHandler storagePluginConfHandler,
                                       SessionNotifier sessionNotifier) {
        this.fileRefEventPublisher = fileRefEventPublisher;
        this.reqGrpService = reqGrpService;
        this.fileDeletionRequestService = fileDeletionRequestService;
        this.fileRefService = fileRefService;
        this.validator = validator;
        this.pluginService = pluginService;
        this.storagePluginConfHandler = storagePluginConfHandler;
        this.sessionNotifier = sessionNotifier;
    }

    /**
     * Initialize new reference requests from Flow items.
     */
    public void reference(List<FilesReferenceEvent> list) {
        Set<FileReference> existingOnesWithSameChecksum = fileRefService.search(list.stream()
                                                                                    .map(FilesReferenceEvent::getFiles)
                                                                                    .flatMap(Set::stream)
                                                                                    .map(FileReferenceRequestDto::getChecksum)
                                                                                    .collect(Collectors.toSet()));

        Set<FileReference> existingOnesWithSameUrl = fileRefService.searchByUrls(list.stream()
                                                                                     .map(FilesReferenceEvent::getFiles)
                                                                                     .flatMap(Set::stream)
                                                                                     .map(FileReferenceRequestDto::getUrl)
                                                                                     .collect(Collectors.toSet()));
        Set<FileDeletionRequest> existingDeletionRequests = fileDeletionRequestService.search(
            existingOnesWithSameChecksum);

        for (FilesReferenceEvent item : list) {
            Errors errors = item.validate(validator);
            if (errors.hasErrors()) {
                reqGrpService.denied(item.getGroupId(),
                                     FileRequestType.REFERENCE,
                                     ErrorTranslator.getErrorsAsString(errors));
                // notify denied requests to the session agent
                item.getFiles().forEach(file -> {
                    String sessionOwner = file.getSessionOwner();
                    String session = file.getSession();
                    this.sessionNotifier.incrementReferenceRequests(sessionOwner, session);
                    this.sessionNotifier.incrementDeniedRequests(sessionOwner, session);
                });
            } else {
                reqGrpService.granted(item.getGroupId(),
                                      FileRequestType.REFERENCE,
                                      item.getFiles().size(),
                                      getRequestExpirationDate());
                reference(item.getFiles(),
                          item.getGroupId(),
                          existingOnesWithSameChecksum,
                          existingOnesWithSameUrl,
                          existingDeletionRequests);
            }
        }
    }

    /**
     * Initialize new reference requests for a given group identifier. Parameter existingOnesWithSameChecksum is passed to improve performance in bulk creation to
     * avoid requesting {@link IFileReferenceRepository} on each request.
     *
     * @return referenced files
     */
    private Collection<FileReference> reference(Collection<FileReferenceRequestDto> requests,
                                                String groupId,
                                                Collection<FileReference> existingOnesWithSameChecksum,
                                                Collection<FileReference> existingOnesWithSameUrl,
                                                Collection<FileDeletionRequest> existingDeletionRequests) {
        Set<FileReference> fileRefs = Sets.newHashSet();
        for (FileReferenceRequestDto file : requests) {
            long start = System.currentTimeMillis();

            // notify reference request to the session agent
            String sessionOwner = file.getSessionOwner();
            String session = file.getSession();
            this.sessionNotifier.incrementReferenceRequests(sessionOwner, session);
            this.sessionNotifier.incrementRunningRequests(sessionOwner, session);

            // Check if the file already exists for the storage destination
            Optional<FileReference> oFileRef = existingOnesWithSameChecksum.stream()
                                                                           .filter(f -> f.getMetaInfo()
                                                                                         .getChecksum()
                                                                                         .equals(file.getChecksum())
                                                                                        && f.getLocation()
                                                                                            .getStorage()
                                                                                            .equals(file.getStorage()))
                                                                           .findFirst();

            Optional<FileReference> oFileRefSameUrl = existingOnesWithSameUrl.stream()
                                                                             .filter(f -> f.getLocation()
                                                                                           .getUrl()
                                                                                           .equals(file.getUrl())
                                                                                          && f.getLocation()
                                                                                              .getStorage()
                                                                                              .equals(file.getStorage()))
                                                                             .findFirst();
            if (oFileRefSameUrl.isPresent() && !oFileRefSameUrl.get()
                                                               .getMetaInfo()
                                                               .getChecksum()
                                                               .equals(file.getChecksum())) {
                // A file with the same referenced url already exists but has a different checksum
                handleError(groupId,
                            file,
                            sessionOwner,
                            session,
                            String.format("The new file %s and the new file %s both reference the same url %s, but "
                                          + "their checksums don't match.",
                                          file.getChecksum(),
                                          oFileRefSameUrl.get().getMetaInfo().getChecksum(),
                                          file.getUrl()));
            } else {

                Optional<FileDeletionRequest> oFileDeletionReq = existingDeletionRequests.stream()
                                                                                         .filter(r -> r.getFileReference()
                                                                                                       .getMetaInfo()
                                                                                                       .getChecksum()
                                                                                                       .equals(file.getChecksum())
                                                                                                      && r.getFileReference()
                                                                                                          .getLocation()
                                                                                                          .getStorage()
                                                                                                          .equals(file.getStorage()))
                                                                                         .findFirst();
                try {
                    FileReferenceResult fileRefResult = reference(file,
                                                                  oFileRef,
                                                                  oFileDeletionReq,
                                                                  Sets.newHashSet(groupId),
                                                                  true,
                                                                  false);
                    FileReference fileRef = fileRefResult.getFileReference();
                    reqGrpService.requestSuccess(groupId,
                                                 FileRequestType.REFERENCE,
                                                 fileRef.getMetaInfo().getChecksum(),
                                                 fileRef.getLocation().getStorage(),
                                                 null,
                                                 Lists.newArrayList(file.getOwner()),
                                                 fileRef);
                    fileRefs.add(fileRef);
                    // Add newly created fileRef to existing file refs in case of the requests contains multiple time the same file to reference
                    existingOnesWithSameChecksum.add(fileRef);
                    // Notify reference success to session agent
                    this.sessionNotifier.decrementRunningRequests(sessionOwner, session);
                    if (fileRefResult.getStatus() != FileReferenceResultStatusEnum.UNMODIFIED) {
                        this.sessionNotifier.incrementReferencedFiles(sessionOwner, session);
                    }
                } catch (ModuleException e) {
                    LOGGER.error(e.getMessage(), e);
                    handleError(groupId, file, sessionOwner, session, e.getMessage());
                } finally {
                    LOGGER.trace("[REFERENCE REQUEST] New reference request ({}) handled in {}ms",
                                 file.getFileName(),
                                 System.currentTimeMillis() - start);
                }
            }
        }
        return fileRefs;
    }

    private void handleError(String groupId,
                             FileReferenceRequestDto file,
                             String sessionOwner,
                             String session,
                             String errorMessage) {
        fileRefEventPublisher.storeError(file.getChecksum(),
                                         Sets.newHashSet(file.getOwner()),
                                         file.getStorage(),
                                         errorMessage,
                                         Sets.newHashSet(groupId));
        reqGrpService.requestError(groupId,
                                   FileRequestType.REFERENCE,
                                   file.getChecksum(),
                                   file.getStorage(),
                                   null,
                                   Sets.newHashSet(file.getOwner()),
                                   errorMessage);
        // notify error request to the session agent
        this.sessionNotifier.decrementRunningRequests(sessionOwner, session);
        // NOTE : As reference requests are not retryable, session errors for those requests are set in info
        // status and not error status. Else, the errors could not be recovered.
        this.sessionNotifier.incrementDeniedRequests(sessionOwner, session);
    }

    /**
     * Reference a new file. No file movement is made here. File is only referenced.
     *
     * @param owner        Owner of the new {@link FileReference}
     * @param metaInfo     information about file
     * @param location     location of file
     * @param groupIds     Business requests identifiers associated to the new file reference.
     * @param sessionOwner Source of the request
     * @param session      tag name for the ongoing session
     * @return {@link FileReference}
     * @throws ModuleException if the file reference can not be created.
     */
    @Transactional(noRollbackFor = ModuleException.class)
    public FileReferenceResult reference(String owner,
                                         FileReferenceMetaInfo metaInfo,
                                         FileLocation location,
                                         Collection<String> groupIds,
                                         String sessionOwner,
                                         String session) throws ModuleException {
        Optional<FileReference> oFileRef = fileRefService.search(location.getStorage(), metaInfo.getChecksum());
        Optional<FileDeletionRequest> oFileDelReq = Optional.empty();
        if (oFileRef.isPresent()) {
            oFileDelReq = fileDeletionRequestService.search(oFileRef.get());
        }
        FileReferenceRequestDto fileRef = FileReferenceRequestDto.build(metaInfo.getFileName(),
                                                                        metaInfo.getChecksum(),
                                                                        metaInfo.getAlgorithm(),
                                                                        metaInfo.getMimeType().toString(),
                                                                        metaInfo.getFileSize(),
                                                                        owner,
                                                                        location.getStorage(),
                                                                        location.getUrl(),
                                                                        sessionOwner,
                                                                        session);
        fileRef.withHeight(metaInfo.getHeight());
        fileRef.withWidth(metaInfo.getWidth());
        fileRef.withType(metaInfo.getType());
        return reference(fileRef, oFileRef, oFileDelReq, groupIds, false, location.isPendingActionRemaining());
    }

    /**
     * Reference a new file. No file movement is made here. File is only referenced.
     *
     * @param request                {@link FileReferenceRequestDto}
     * @param fileRef                {@link FileReference} of associated file if already exists
     * @param groupIds               Business requests identifiers associated to the new file reference.
     * @param isReferenced           does the file is a reference (meaning not stored b this service)
     * @param pendingActionRemaining does an asynchronous action needed to consider file as fully stored
     * @return {@link FileReference}
     * @throws ModuleException if the file reference can not be created.
     */
    private FileReferenceResult reference(FileReferenceRequestDto request,
                                          Optional<FileReference> fileRef,
                                          Optional<FileDeletionRequest> fileDelReq,
                                          Collection<String> groupIds,
                                          boolean isReferenced,
                                          boolean pendingActionRemaining) throws ModuleException {
        if (fileRef.isPresent()) {
            return handleAlreadyExists(fileRef.get(), fileDelReq, request, groupIds);
        } else {
            // If referenced file is associated to a known storage location then validate the reference
            validateReferenceUrl(request);
            FileReference newFileRef = fileRefService.create(Lists.newArrayList(request.getOwner()),
                                                             FileReferenceMetaInfo.buildFromFileReferenceRequestDto(
                                                                 request),
                                                             new FileLocation(request.getStorage(),
                                                                              request.getUrl(),
                                                                              pendingActionRemaining),
                                                             isReferenced);
            String message = String.format("New file <%s> referenced at <%s> (checksum: %s)",
                                           newFileRef.getMetaInfo().getFileName(),
                                           newFileRef.getLocation().toString(),
                                           newFileRef.getMetaInfo().getChecksum());
            fileRefEventPublisher.storeSuccess(newFileRef, message, groupIds, Lists.newArrayList(request.getOwner()));
            return FileReferenceResult.build(newFileRef, FileReferenceResultStatusEnum.CREATED);
        }
    }

    private void validateReferenceUrl(FileReferenceRequestDto request) throws ModuleException {
        Optional<PluginConfiguration> conf = storagePluginConfHandler.getConfiguredStorage(request.getStorage());
        if (conf.isPresent()) {
            try {
                IStorageLocation storagePlugin = pluginService.getPlugin(conf.get().getBusinessId());
                Set<String> errors = Sets.newHashSet();
                if (!storagePlugin.isValidUrl(request.getUrl(), errors)) {
                    throw new ModuleException(String.format(
                        "File reference %s url=%s format is not valid for storage location %s. Cause : %s",
                        request.getFileName(),
                        request.getUrl(),
                        conf.get().getBusinessId(),
                        errors));
                }
            } catch (NotAvailablePluginConfigurationException e) {
                throw new ModuleException(String.format("File reference %s cannot be validated by the %s plugin.",
                                                        request.getFileName(),
                                                        conf.get().getBusinessId()), e);
            }
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
     * @return {@link FileReferenceResult} file reference and update status. Update is false if file reference has not been updated.
     * @throws ModuleException If file reference can not be created
     */
    private FileReferenceResult handleAlreadyExists(FileReference fileReference,
                                                    Optional<FileDeletionRequest> deletionRequest,
                                                    FileReferenceRequestDto request,
                                                    Collection<String> groupIds) throws ModuleException {
        if (deletionRequest.isPresent() && (deletionRequest.get().getStatus() == FileRequestStatus.PENDING)) {
            // A deletion is pending on the existing file reference but the new reference request does not indicates the new file location
            String message = String.format("File %s is being deleted. Please try later.", request.getChecksum());
            groupIds.forEach(id -> fileRefEventPublisher.storeError(request.getChecksum(),
                                                                    Sets.newHashSet(request.getOwner()),
                                                                    request.getStorage(),
                                                                    message,
                                                                    Sets.newHashSet(id)));
            throw new ModuleException(message);
        } else {
            if (deletionRequest.isPresent()) {
                // Delete not running deletion request to add the new owner
                fileDeletionRequestService.delete(deletionRequest.get());
            }
            if (!fileReference.getMetaInfo().equals(FileReferenceMetaInfo.buildFromFileReferenceRequestDto(request))) {
                LOGGER.debug("Existing referenced file meta information differs "
                             + "from new reference meta information. Previous ones are maintained");
            }
            String message = String.format(
                "New owner <%s> added to existing referenced file <%s> at <%s> (checksum: %s) ",
                request.getOwner(),
                fileReference.getMetaInfo().getFileName(),
                fileReference.getLocation().toString(),
                fileReference.getMetaInfo().getChecksum());
            fileRefEventPublisher.storeSuccess(fileReference,
                                               message,
                                               groupIds,
                                               Lists.newArrayList(request.getOwner()));
            FileReferenceResultStatusEnum status = FileReferenceResultStatusEnum.UNMODIFIED;
            if (fileRefService.addOwner(fileReference.getId(), request.getOwner())) {
                status = FileReferenceResultStatusEnum.UPDATED;
            }
            return FileReferenceResult.build(fileReference, status);
        }
    }

    /**
     * Retrieve expiration date for deletion request
     */
    public OffsetDateTime getRequestExpirationDate() {
        if ((nbDaysBeforeExpiration != null) && (nbDaysBeforeExpiration > 0)) {
            return OffsetDateTime.now().plusDays(nbDaysBeforeExpiration);
        } else {
            return null;
        }
    }
}
