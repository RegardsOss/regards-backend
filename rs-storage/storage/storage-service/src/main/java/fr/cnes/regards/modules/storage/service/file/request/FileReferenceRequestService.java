/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.module.validation.ErrorTranslator;
import fr.cnes.regards.framework.modules.jobs.domain.IJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.notification.NotificationLevel;
import fr.cnes.regards.framework.notification.client.INotificationClient;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.utils.RsRuntimeException;
import fr.cnes.regards.framework.utils.plugins.exception.NotAvailablePluginConfigurationException;
import fr.cnes.regards.modules.fileaccess.dto.FileReferenceMetaInfoDto;
import fr.cnes.regards.modules.fileaccess.dto.FileRequestStatus;
import fr.cnes.regards.modules.fileaccess.dto.FileRequestType;
import fr.cnes.regards.modules.fileaccess.dto.request.FileReferenceRequestDto;
import fr.cnes.regards.modules.fileaccess.dto.request.FileStorageRequestAggregationDto;
import fr.cnes.regards.modules.fileaccess.dto.request.FileStorageRequestResultDto;
import fr.cnes.regards.modules.fileaccess.plugin.domain.FileStorageWorkingSubset;
import fr.cnes.regards.modules.fileaccess.plugin.domain.IStorageLocation;
import fr.cnes.regards.modules.filecatalog.amqp.input.FilesReferenceEvent;
import fr.cnes.regards.modules.filecatalog.amqp.output.FileReferenceEvent;
import fr.cnes.regards.modules.storage.dao.IFileReferenceRequestRepository;
import fr.cnes.regards.modules.storage.domain.FileReferenceResult;
import fr.cnes.regards.modules.storage.domain.FileReferenceResultStatusEnum;
import fr.cnes.regards.modules.storage.domain.database.FileLocation;
import fr.cnes.regards.modules.storage.domain.database.FileReference;
import fr.cnes.regards.modules.storage.domain.database.FileReferenceMetaInfo;
import fr.cnes.regards.modules.storage.domain.database.request.FileDeletionRequest;
import fr.cnes.regards.modules.storage.domain.database.request.FileReferenceRequestAggregation;
import fr.cnes.regards.modules.storage.domain.database.request.FileStorageRequestAggregation;
import fr.cnes.regards.modules.storage.domain.predicate.StoragePredicates;
import fr.cnes.regards.modules.storage.service.file.FileReferenceEventPublisher;
import fr.cnes.regards.modules.storage.service.file.FileReferenceService;
import fr.cnes.regards.modules.storage.service.file.job.FileReferenceRequestJob;
import fr.cnes.regards.modules.storage.service.location.StoragePluginConfigurationHandler;
import fr.cnes.regards.modules.storage.service.session.SessionNotifier;
import io.micrometer.core.annotation.Timed;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MimeType;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Service to handle File reference requests.
 *
 * @author Olivier Navarro
 */
@Service
@MultitenantTransactional
@Scope(proxyMode = ScopedProxyMode.TARGET_CLASS)
public class FileReferenceRequestService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileReferenceRequestService.class);

    private final FileReferenceEventPublisher fileRefEventPublisher;

    private final RequestsGroupService reqGroupService;

    private final RequestStatusService requestStatusService;

    private final FileDeletionRequestService fileDeletionRequestService;

    private final IFileReferenceRequestRepository fileRefRequestRepository;

    private final FileReferenceService fileRefService;

    private final Validator validator;

    private final IPluginService pluginService;

    private final StoragePluginConfigurationHandler storagePluginConfHandler;

    private final SessionNotifier sessionNotifier;

    private final INotificationClient notificationClient;

    @Value("${regards.storage.reference.requests.days.before.expiration:5}")
    private Integer nbDaysBeforeExpiration;

    public FileReferenceRequestService(FileReferenceEventPublisher fileRefEventPublisher,
                                       RequestsGroupService reqGroupService,
                                       RequestStatusService requestStatusService,
                                       FileDeletionRequestService fileDeletionRequestService,
                                       IFileReferenceRequestRepository fileRefRequestRepository,
                                       FileReferenceService fileRefService,
                                       Validator validator,
                                       IPluginService pluginService,
                                       StoragePluginConfigurationHandler storagePluginConfHandler,
                                       SessionNotifier sessionNotifier,
                                       INotificationClient notificationClient) {
        this.fileRefEventPublisher = fileRefEventPublisher;
        this.reqGroupService = reqGroupService;
        this.requestStatusService = requestStatusService;
        this.fileDeletionRequestService = fileDeletionRequestService;
        this.fileRefRequestRepository = fileRefRequestRepository;
        this.fileRefService = fileRefService;
        this.validator = validator;
        this.pluginService = pluginService;
        this.storagePluginConfHandler = storagePluginConfHandler;
        this.sessionNotifier = sessionNotifier;
        this.notificationClient = notificationClient;
    }

    /**
     * Initialize new reference requests from events.
     */
    @Timed(value = "file_reference_request_amqp_handler",
           description = "FileReferenceRequestService#createReferenceRequests")
    public void createReferenceRequests(Collection<FilesReferenceEvent> events) {

        // deny invalid request keep only valid request
        final List<FilesReferenceEvent> validEvents = new ArrayList<>(events.size());

        // validate and deny invalid request
        for (FilesReferenceEvent event : events) {
            // validate
            final Errors errors = event.validate(validator);
            if (errors.hasErrors()) {
                denyEvent(event, ErrorTranslator.getErrorsAsString(errors));
            } else {
                validEvents.add(event);
            }
        }

        if (!validEvents.isEmpty()) {
            createNewFileReferenceRequests(validEvents);
        }
    }

    private void denyEvent(FilesReferenceEvent event, String errors) {
        // denied the request
        reqGroupService.denied(event.getGroupId(), FileRequestType.REFERENCE, errors);
        // notify denied requests to the session agent
        event.getFiles().forEach(file -> notifyInvalid(file.getSessionOwner(), file.getSession()));
    }

    public void createNewFileReferenceRequests(Collection<FilesReferenceEvent> messages) {
        for (FilesReferenceEvent message : messages) {
            for (FileReferenceRequestDto file : message.getFiles()) {
                final FileReferenceMetaInfo metaInfo = new FileReferenceMetaInfo(file.getChecksum(),
                                                                                 file.getAlgorithm(),
                                                                                 file.getFileName(),
                                                                                 file.getFileSize(),
                                                                                 MimeType.valueOf(file.getMimeType()));
                createNewFileReferenceRequest(file.getOwner(),
                                              metaInfo,
                                              file.getUrl(),
                                              file.getStorage(),
                                              message.getGroupId(),
                                              file.getSessionOwner(),
                                              file.getSession());
            }
            reqGroupService.granted(message.getGroupId(),
                                    FileRequestType.REFERENCE,
                                    message.getFiles().size(),
                                    getRequestExpirationDate());
        }
    }

    /**
     * Create a new {@link FileStorageRequestAggregation}
     *
     * @param owner        owner of the file to be referenced
     * @param fileMetaInfo meta information of the file to store
     * @param originUrl    file origin location
     * @param storage      storage destination location
     * @param groupId      group identifier of the request
     * @param sessionOwner session owner of the new file reference request
     * @param session      session of the new file reference request
     */
    private void createNewFileReferenceRequest(String owner,
                                               FileReferenceMetaInfo fileMetaInfo,
                                               String originUrl,
                                               String storage,
                                               String groupId,
                                               String sessionOwner,
                                               String session) {
        long startTime = System.currentTimeMillis();
        // notify to the session agent that the request is running
        notifyStartRunning(sessionOwner, session);

        // save a new request
        final FileReferenceRequestAggregation fileReferenceRequest = new FileReferenceRequestAggregation(owner,
                                                                                                         fileMetaInfo,
                                                                                                         originUrl,
                                                                                                         storage,
                                                                                                         null,
                                                                                                         groupId,
                                                                                                         sessionOwner,
                                                                                                         session);
        final FileRequestStatus newStatus = requestStatusService.getNewStatus(fileReferenceRequest);
        fileReferenceRequest.setStatus(newStatus);

        // Should always return a value or throw an exception.
        // But since it depends on the implementation version, to be safe nullity is still checked.
        final FileReferenceRequestAggregation saved = fileRefRequestRepository.save(fileReferenceRequest);
        if (saved == null) {
            LOGGER.warn("[REFERENCE REQUESTS] New file reference request for file <{}>"
                        + " to be stored in {} with status {} failed to be created. Elapsed time: {} ms",
                        fileReferenceRequest.getMetaInfo().getFileName(),
                        fileReferenceRequest.getStorage(),
                        fileReferenceRequest.getStatus(),
                        elapsedMillisecondsFrom(startTime));
        } else {
            LOGGER.trace("[REFERENCE REQUESTS] New file reference request for file <{}>"
                         + " to be stored in {} with status {} has been successfully created in {} ms",
                         fileReferenceRequest.getMetaInfo().getFileName(),
                         fileReferenceRequest.getStorage(),
                         fileReferenceRequest.getStatus(),
                         elapsedMillisecondsFrom(startTime));
        }
    }

    public boolean handleJobCrash(JobInfo jobInfo) {
        // is it of JobInfo concerning FileReferenceRequestJob
        boolean isFileReferenceRequestJob = FileReferenceRequestJob.class.getName().equals(jobInfo.getClassName());
        if (!isFileReferenceRequestJob) {
            return false;
        }

        // let's handle the crash by sending a notification failure.
        try {
            // collect all the concerned FileStorageRequestAggregation
            final FileStorageWorkingSubset workingSubset = IJob.getValue(jobInfo.getParametersAsMap(),
                                                                         FileReferenceRequestJob.WORKING_SUBSET);
            final Set<Long> requestIds = workingSubset.getFileReferenceRequests()
                                                      .stream()
                                                      .map(FileStorageRequestAggregationDto::getId)
                                                      .collect(Collectors.toSet());
            final List<FileReferenceRequestAggregation> requests = fileRefRequestRepository.findAllById(requestIds);

            // handle the error on all those requests
            final String stackTrace = jobInfo.getStatus().getStackTrace();
            requests.stream()
                    .filter(req -> FileRequestStatus.isRunning(req.getStatus()))
                    .forEach(req -> handleError(req, stackTrace));

        } catch (JobParameterMissingException | JobParameterInvalidException e) {
            final String message = String.format("File Reference request job with id \"%s\" fails with status \"%s\"",
                                                 jobInfo.getId(),
                                                 jobInfo.getStatus().getStatus());
            LOGGER.error(message, e);
            notificationClient.notify(message, "Reference job failure", NotificationLevel.ERROR, DefaultRole.ADMIN);
        }

        return true;
    }

    public void handleSuccess(FileStorageRequestAggregationDto request, FileReference reference, boolean modified) {
        final FileStorageRequestResultDto result = FileStorageRequestResultDto.build(request,
                                                                                     reference.getLocation().getUrl(),
                                                                                     reference.getMetaInfo()
                                                                                              .getFileSize(),
                                                                                     reference.getLocation()
                                                                                              .isPendingActionRemaining(),
                                                                                     false);
        handleSuccess(result, modified);
    }

    public void handleSuccess(FileReferenceRequestAggregation request, FileReference reference, boolean modified) {
        final FileStorageRequestResultDto result = FileStorageRequestResultDto.build(request.toDto(),
                                                                                     reference.getLocation().getUrl(),
                                                                                     reference.getMetaInfo()
                                                                                              .getFileSize(),
                                                                                     reference.getLocation()
                                                                                              .isPendingActionRemaining(),
                                                                                     false);
        handleSuccess(result, modified);
    }

    private void handleSuccess(FileStorageRequestResultDto storageResult, boolean modified) {

        // Note that the request is rebuilt from the dto,
        // so the status information is not available and the request need to be retrieved from  the database
        final FileReferenceRequestAggregation request = FileReferenceRequestAggregation.fromDto(storageResult.getRequest());

        final String checksum = request.getMetaInfo().getChecksum();
        final String storage = request.getStorage();

        final FileReference fileRef = fileRefService.search(storage, checksum)
                                                    .orElseThrow(() -> newReferenceMissingException(checksum, storage));

        // reference already exists
        for (String groupId : request.getGroupIds()) {

            reqGroupService.requestSuccess(groupId,
                                           FileRequestType.REFERENCE,
                                           checksum,
                                           storage,
                                           request.getStorageSubDirectory(),
                                           request.getOwners(),
                                           fileRef);
        }

        fileRefRequestRepository.updateStatus(request.getId(), FileRequestStatus.SUCCESS);
        notifySuccess(request.getSessionOwner(), request.getSession(), modified);
    }

    private @NotNull RuntimeException newReferenceMissingException(String checksum, String storage) {
        final String message = String.format("File reference of checksum %s and in storage %s is not found in database",
                                             checksum,
                                             storage);
        return new RsRuntimeException(message);
    }

    private @NotNull RuntimeException newRequestMissingException(String checksum, String storage) {
        final String message = String.format(
            "File reference request of checksum %s and in storage %s is not found in database",
            checksum,
            storage);
        return new RsRuntimeException(message);
    }

    /**
     * Handle a {@link FileReferenceRequestAggregation} error.
     * <ul>
     * <li> Update the request into database </li>
     * <li> Send bus message information about storage error </li>
     * <li> Update group with the error request </li>
     * </ul>
     */
    private void handleError(FileReferenceRequestAggregation request, String errorCause) {
        // The file is not referenced. so handle reference error by modifying the request status.
        // mark the request in ERROR
        fileRefRequestRepository.updateError(request.getId(), FileRequestStatus.ERROR, errorCause);

        // publish the error
        fileRefEventPublisher.storeError(request.getMetaInfo().getChecksum(),
                                         request.getOwners(),
                                         request.getStorage(),
                                         errorCause,
                                         request.getGroupIds());

        for (String groupId : request.getGroupIds()) {
            reqGroupService.requestError(groupId,
                                         FileRequestType.REFERENCE,
                                         request.getMetaInfo().getChecksum(),
                                         request.getStorage(),
                                         request.getStorageSubDirectory(),
                                         request.getOwners(),
                                         errorCause);
        }
        // notify error to the session agent
        notifyError(request.getSessionOwner(), request.getSession());
    }

    private void handleError(String groupId,
                             FileReferenceRequestDto file,
                             String sessionOwner,
                             String session,
                             String errorMessage) {
        fileRefEventPublisher.storeError(file.getChecksum(),
                                         Set.of(file.getOwner()),
                                         file.getStorage(),
                                         errorMessage,
                                         Set.of(groupId));
        reqGroupService.requestError(groupId,
                                     FileRequestType.REFERENCE,
                                     file.getChecksum(),
                                     file.getStorage(),
                                     null,
                                     Set.of(file.getOwner()),
                                     errorMessage);
    }

    private void notifyTrying(String sessionOwner, String session) {
        this.sessionNotifier.incrementReferenceRequests(sessionOwner, session);
    }

    private void notifyTried(String sessionOwner, String session, int counter) {
        this.notifyStopRunning(sessionOwner, session, counter);
        this.sessionNotifier.decrementReferenceRequests(sessionOwner, session);
    }

    private void notifyStartRunning(String sessionOwner, String session) {
        this.sessionNotifier.incrementRunningRequests(sessionOwner, session);
    }

    private void notifyStopRunning(String sessionOwner, String session, int counter) {
        for (int i = 0; i < counter; ++i) {
            this.sessionNotifier.decrementRunningRequests(sessionOwner, session);
        }
    }

    private void notifyInvalid(String sessionOwner, String session) {
        this.sessionNotifier.incrementDeniedRequests(sessionOwner, session);
    }

    private void notifyError(String sessionOwner, String session) {
        // notify error request to the session agent
        this.sessionNotifier.decrementRunningRequests(sessionOwner, session);
        // decrement the number of reference requests
        this.sessionNotifier.decrementReferenceRequests(sessionOwner, session);

        // NOTE : reference requests are not retryable,
        // so session errors for those requests are set as info status and not error status,
        // otherwise errors could not be recovered.
        this.sessionNotifier.incrementDeniedRequests(sessionOwner, session);
    }

    private void notifySuccess(String sessionOwner, String session, boolean modified) {
        // Session handling
        // decrement the number of running requests
        this.sessionNotifier.decrementRunningRequests(sessionOwner, session);
        // decrement the number of reference requests
        this.sessionNotifier.decrementReferenceRequests(sessionOwner, session);

        // notify the number of successful created files
        if (modified) {
            this.sessionNotifier.incrementReferencedFiles(sessionOwner, session);
        }
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
        final Optional<FileReference> oFileRef = fileRefService.search(location.getStorage(), metaInfo.getChecksum());
        final Optional<FileDeletionRequest> oFileDelReq = oFileRef.flatMap(fileDeletionRequestService::search);
        final FileReferenceRequestDto fileRef = FileReferenceRequestDtoBuilders.toFileReferenceDto(owner,
                                                                                                   metaInfo,
                                                                                                   location,
                                                                                                   sessionOwner,
                                                                                                   session);
        return reference(fileRef,
                         oFileRef.orElse(null),
                         oFileDelReq.orElse(null),
                         groupIds,
                         false,
                         location.isPendingActionRemaining());
    }

    /**
     * Reference a new file. No file movement is made here. File is only referenced.
     *
     * @param request                {@link FileReferenceRequestDto}
     * @param fileRef                {@link FileReference} of associated file if already exists
     * @param groupIds               Business requests identifiers associated to the new file reference.
     * @param isReferenced           does the file is a reference (meaning not stored by this service).
     * @param pendingActionRemaining does an asynchronous action needed to consider file as fully stored
     * @return {@link FileReference}
     * @throws ModuleException if the file reference can not be created.
     */
    private FileReferenceResult reference(FileReferenceRequestDto request,
                                          @Nullable FileReference fileRef,
                                          @Nullable FileDeletionRequest fileDelReq,
                                          Collection<String> groupIds,
                                          boolean isReferenced,
                                          boolean pendingActionRemaining) throws ModuleException {
        FileReferenceResult result;
        if (fileRef != null) {
            result = handleAlreadyExists(fileRef, fileDelReq, request, groupIds);
        } else {
            // If referenced file is associated to a known storage location then validate the reference
            // if not valid an ModuleException is thrown
            validateReferenceUrl(request);

            // create a new FileReference
            final FileLocation location = new FileLocation(request.getStorage(),
                                                           request.getUrl(),
                                                           pendingActionRemaining);
            final FileReference newFileRef = fileRefService.create(List.of(request.getOwner()),
                                                                   FileReferenceMetaInfo.buildFromFileReferenceRequestDto(
                                                                       request),
                                                                   location,
                                                                   isReferenced);
            final String message = String.format("New file <%s> referenced at <%s> (checksum: %s)",
                                                 newFileRef.getMetaInfo().getFileName(),
                                                 newFileRef.getLocation().toString(),
                                                 newFileRef.getMetaInfo().getChecksum());
            fileRefEventPublisher.storeSuccess(newFileRef, message, groupIds, List.of(request.getOwner()));
            result = FileReferenceResult.build(newFileRef, FileReferenceResultStatusEnum.CREATED);
        }
        return result;
    }

    /**
     * Validate the url of the given request if a plugin for the storage is available.
     *
     * @param request to be validated
     * @throws ModuleException on invalid url.
     */
    private void validateReferenceUrl(FileReferenceRequestDto request) throws ModuleException {
        final PluginConfiguration pluginConfHandler = storagePluginConfHandler.getConfiguredStorage(request.getStorage())
                                                                              .orElse(null);
        if (pluginConfHandler != null) {
            try {
                IStorageLocation storagePlugin = pluginService.getPlugin(pluginConfHandler.getBusinessId());
                final Set<String> errors = Sets.newHashSet();
                if (!storagePlugin.isValidUrl(request.getUrl(), errors)) {
                    throw new ModuleException(String.format(
                        "File reference %s url=%s format is not valid for storage location %s. Cause : %s",
                        request.getFileName(),
                        request.getUrl(),
                        pluginConfHandler.getBusinessId(),
                        errors));
                }
            } catch (NotAvailablePluginConfigurationException e) {
                final String error = String.format("File reference %s cannot be validated by the %s plugin.",
                                                   request.getFileName(),
                                                   pluginConfHandler.getBusinessId());
                throw new ModuleException(error, e);
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
                                                    @Nullable FileDeletionRequest deletionRequest,
                                                    FileReferenceRequestDto request,
                                                    Collection<String> groupIds) throws ModuleException {
        if (deletionRequest != null) {
            if (deletionRequest.getStatus() == FileRequestStatus.PENDING) {
                // A deletion is pending on the existing file reference
                // but the new reference request does not indicate the new file location
                String message = String.format("File %s is being deleted. Please try later.", request.getChecksum());
                groupIds.forEach(id -> fileRefEventPublisher.storeError(request.getChecksum(),
                                                                        Set.of(request.getOwner()),
                                                                        request.getStorage(),
                                                                        message,
                                                                        Sets.newHashSet(id)));
                throw new ModuleException(message);
            }
            // Delete not running deletion request to add the new owner
            fileDeletionRequestService.delete(deletionRequest);
        }

        if (!fileReference.getMetaInfo().equals(FileReferenceMetaInfo.buildFromFileReferenceRequestDto(request))) {
            LOGGER.debug("Existing referenced file meta information differs "
                         + "from new reference meta information. Previous ones are maintained");
        }
        final String message = String.format(
            "New owner <%s> added to existing referenced file <%s> at <%s> (checksum: %s) ",
            request.getOwner(),
            fileReference.getMetaInfo().getFileName(),
            fileReference.getLocation().toString(),
            fileReference.getMetaInfo().getChecksum());
        fileRefEventPublisher.storeSuccess(fileReference, message, groupIds, List.of(request.getOwner()));
        FileReferenceResultStatusEnum status = FileReferenceResultStatusEnum.UNMODIFIED;
        if (fileRefService.addOwner(fileReference.getId(), request.getOwner())) {
            status = FileReferenceResultStatusEnum.UPDATED;
        }
        return FileReferenceResult.build(fileReference, status);
    }

    /**
     * Retrieve expiration date.
     */
    private OffsetDateTime getRequestExpirationDate() {
        OffsetDateTime expirationDate = null;
        if ((nbDaysBeforeExpiration != null) && (nbDaysBeforeExpiration > 0)) {
            expirationDate = OffsetDateTime.now().plusDays(nbDaysBeforeExpiration);
        }
        return expirationDate;
    }

    /**
     * Try and reference the file of the given request. All the request concerned file on a same storage.
     * Called by {@link FileRequestScheduler#handleFileReferenceRequests}
     *
     * @param fileReferenceRequests the reference requests to try and reference.
     * @see FileReferenceRequestJob
     * @see FileReferenceRequestJobSchedulingService#scheduleJobs
     */
    public void tryAndReference(Set<FileStorageRequestAggregationDto> fileReferenceRequests) {
        final Set<String> checksums = fileReferenceRequests.stream()
                                                           .map(FileStorageRequestAggregationDto::getMetaInfo)
                                                           .map(FileReferenceMetaInfoDto::getChecksum)
                                                           .filter(Objects::nonNull)
                                                           .collect(Collectors.toSet());

        final Set<String> urls = fileReferenceRequests.stream()
                                                      .map(FileStorageRequestAggregationDto::getOriginUrl)
                                                      .filter(Objects::nonNull)
                                                      .collect(Collectors.toSet());

        for (FileStorageRequestAggregationDto request : fileReferenceRequests) {
            final long startTime = System.currentTimeMillis();
            // notify reference request to the session agent
            final String sessionOwner = request.getSessionOwner();
            final String session = request.getSession();
            notifyTrying(sessionOwner, session);

            final Set<FileReference> fileRefsWithSameChecksum = fileRefService.search(checksums);
            final Set<FileReference> fileRefsWithSameUrl = fileRefService.searchByUrls(urls);
            final Set<FileDeletionRequest> deletionRequests = fileDeletionRequestService.search(fileRefsWithSameChecksum);

            // any mismatching checksum, the request will be in error.
            final boolean mismatched = handleErrorIfAnyMismatchingCheckSum(request, fileRefsWithSameUrl);
            if (mismatched) {
                continue;
            }

            // presence of a deletion request, delay the reference request
            final boolean delayed = delayIfDeletionInProgress(request, deletionRequests);
            if (delayed) {
                fileRefRequestRepository.updateStatus(request.getId(), FileRequestStatus.DELAYED);
                notifyTried(sessionOwner, session, request.getGroupIds().size());
                continue;
            }

            try {
                final FileReferenceResult referenceResult = tryAndReference(request, fileRefsWithSameChecksum);
                final FileReference reference = referenceResult.getFileReference();
                final boolean modified = referenceResult.getStatus() != FileReferenceResultStatusEnum.UNMODIFIED;
                fileRefRequestRepository.updateStatus(request.getId(), FileRequestStatus.SUCCESS);
                handleSuccess(request, reference, modified);

            } catch (ModuleException e) {
                LOGGER.error(e.getMessage(), e);
                fileRefRequestRepository.updateError(request.getId(), FileRequestStatus.ERROR, e.getMessage());

                for (String groupId : request.getGroupIds()) {
                    handleError(groupId,
                                FileReferenceRequestDtoBuilders.toFileReferenceDto(request),
                                sessionOwner,
                                session,
                                e.getMessage());
                    notifyError(sessionOwner, session);
                }
            } finally {
                LOGGER.trace("[REFERENCE REQUEST] New reference request ({}) handled in {}ms",
                             request.getMetaInfo().getFileName(),
                             elapsedMillisecondsFrom(startTime));
            }
        }
    }

    private boolean delayIfDeletionInProgress(FileStorageRequestAggregationDto request,
                                              Set<FileDeletionRequest> deletionRequests) {

        final String checksum = request.getMetaInfo().getChecksum();
        final String storage = request.getStorage();

        // Check any file deletion request to be done
        final Optional<FileDeletionRequest> oFileDeletionReq = deletionRequests.stream()
                                                                               .filter(StoragePredicates.fileDeletionRequestWithSameStorageAndChecksum(
                                                                                   storage,
                                                                                   checksum))
                                                                               .filter(fdr -> !FileRequestStatus.isFinished(
                                                                                   fdr.getStatus()))
                                                                               .findFirst();

        boolean delayed = oFileDeletionReq.isPresent();
        if (delayed) {
            fileRefRequestRepository.updateStatus(request.getId(), FileRequestStatus.DELAYED);
        }
        return delayed;
    }

    /**
     * Check not any checksum of the given {@link FileReference}s with the same url as the one of the given request
     * is different with the one of the given request. if any mismatch an error is produced.
     *
     * @param request             - dto of the FileReference request
     * @param fileRefsWithSameUrl collection of {@link FileReference} with the same url to be checked.
     *                            return boolean indicating whether any mismatching checksum has been detected and handled.
     */
    private boolean handleErrorIfAnyMismatchingCheckSum(FileStorageRequestAggregationDto request,
                                                        Collection<FileReference> fileRefsWithSameUrl) {

        final String checksum = request.getMetaInfo().getChecksum();
        final String storage = request.getStorage();
        final String url = request.getOriginUrl();

        // Check if the file already exists for the storage destination with the same url
        final Optional<FileReference> oFileRefSameUrl = fileRefsWithSameUrl.stream()
                                                                           .filter(StoragePredicates.fileReferenceWithSameStorageAndUrl(
                                                                               storage,
                                                                               url))
                                                                           .findFirst();
        // ensure the checksum of the request is matching the checksum of the FileReference with the same url.
        final String checkSumOfSameUrl = oFileRefSameUrl.map(FileReference::getMetaInfo)
                                                        .map(FileReferenceMetaInfo::getChecksum)
                                                        .orElse(null);
        final boolean mismatching = oFileRefSameUrl.isPresent() && !Objects.equals(checksum, checkSumOfSameUrl);
        if (mismatching) {
            // A file with the same referenced url already exists but has a different checksum
            final String error = String.format("The new file (with checksum %s)"
                                               + " and the existing file (with checksum %s)"
                                               + " both reference the same url %s,"
                                               + " but their checksums don't match.", checksum, checkSumOfSameUrl, url);
            final FileReferenceRequestAggregation entity = FileReferenceRequestAggregation.toFileReferenceAggregation(
                request);
            handleError(entity, error);
        }
        return mismatching;
    }

    /**
     * Initialize new reference requests for a given group identifier.
     * is different with the one of the given request. if any mismatch an error is produced.
     *
     * @param request                  - dto of the FileReference request to be created
     * @param fileRefsWithSameChecksum collection of {@link FileReference} with the same checksum.
     *                                 return @{link FileReferenceResult}
     */
    private FileReferenceResult tryAndReference(FileStorageRequestAggregationDto request,
                                                Collection<FileReference> fileRefsWithSameChecksum)
        throws ModuleException {

        final String checksum = request.getMetaInfo().getChecksum();
        final String storage = request.getStorage();

        // Check if the file already exists for the storage destination and the same checksum
        final FileReference foundFileRef = fileRefsWithSameChecksum.stream()
                                                                   .filter(StoragePredicates.fileReferenceWithSameStorageAndChecksum(
                                                                       storage,
                                                                       checksum))
                                                                   .findFirst()
                                                                   .orElse(null);

        final FileReferenceRequestDto fileReferenceRequestDto = FileReferenceRequestDtoBuilders.toFileReferenceDto(
            request);
        final FileReferenceResult referenceResult = reference(fileReferenceRequestDto,
                                                              foundFileRef,
                                                              null,
                                                              request.getGroupIds(),
                                                              true,
                                                              false);
        fileRefsWithSameChecksum.add(referenceResult.getFileReference());
        return referenceResult;
    }

    public void deleteAllTerminatedRequestOfGroups(Set<String> groupIds) {
        groupIds.forEach(this::deleteAllTerminatedRequestOfGroup);
    }

    private void deleteAllTerminatedRequestOfGroup(String groupId) {
        final Predicate<FileReferenceRequestAggregation> singleGroup = request -> request.getGroupIds().size() == 1;
        final Map<Boolean, List<FileReferenceRequestAggregation>> requestsByGroupIdCount = fileRefRequestRepository.findByGroupIds(
            groupId).stream().collect(Collectors.partitioningBy(singleGroup));

        // request belonging to a single group can be deleted
        final List<FileReferenceRequestAggregation> singleGroupRequests = requestsByGroupIdCount.get(Boolean.TRUE);
        final Set<Long> requestToDeleteIds = singleGroupRequests.stream()
                                                                .map(FileReferenceRequestAggregation::getId)
                                                                .collect(Collectors.toSet());
        fileRefRequestRepository.deleteAllByIdInBatch(requestToDeleteIds);

        // request belonging to more than one group can not be deleted. only the group can be removed.
        final List<FileReferenceRequestAggregation> multiGroupRequests = requestsByGroupIdCount.get(Boolean.FALSE);
        multiGroupRequests.stream()
                          .map(FileReferenceRequestAggregation::getGroupIds)
                          .forEach(ids -> ids.remove(groupId));
        fileRefRequestRepository.saveAll(multiGroupRequests);
    }

    /**
     * Return the elapsed time in milliseconds from the given time to now.
     * Note: the return value is purposely Long and not the primitive type long. Since it is used as a
     * logging parameter expecting Object, the autoboxing is done in this method only and not at each call level in
     * the logging statement.
     *
     * @param startTime initial time from which is computed the elapsed time.
     * @return Long representing a duration in milliseconds.
     */
    private static Long elapsedMillisecondsFrom(long startTime) {
        return System.currentTimeMillis() - startTime;
    }
}
