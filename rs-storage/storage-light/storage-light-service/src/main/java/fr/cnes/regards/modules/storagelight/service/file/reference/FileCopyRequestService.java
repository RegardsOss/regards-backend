package fr.cnes.regards.modules.storagelight.service.file.reference;

import java.time.OffsetDateTime;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.notification.NotificationLevel;
import fr.cnes.regards.framework.notification.client.INotificationClient;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.storagelight.dao.IFileCopyRequestRepository;
import fr.cnes.regards.modules.storagelight.domain.FileRequestStatus;
import fr.cnes.regards.modules.storagelight.domain.database.CacheFile;
import fr.cnes.regards.modules.storagelight.domain.database.FileReference;
import fr.cnes.regards.modules.storagelight.domain.database.request.FileCopyRequest;
import fr.cnes.regards.modules.storagelight.domain.dto.FileCopyRequestDTO;
import fr.cnes.regards.modules.storagelight.domain.event.FileReferenceEvent;
import fr.cnes.regards.modules.storagelight.domain.plugin.INearlineStorageLocation;
import fr.cnes.regards.modules.storagelight.service.file.cache.CacheService;

/**
 * Service to handle {@link FileCopyRequest}s.
 * Those requests are created when a file reference need to be restored physically thanks to an existing {@link INearlineStorageLocation} plugin.
 *
 * @author Sébastien Binda
 */
@Service
@MultitenantTransactional
public class FileCopyRequestService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileCopyRequestService.class);

    private static final int NB_REFERENCE_BY_PAGE = 1000;

    @Autowired
    private IFileCopyRequestRepository copyRepository;

    @Autowired
    private CacheService cacheService;

    @Autowired
    private FileReferenceService fileRefService;

    @Autowired
    private INotificationClient notificationClient;

    public Optional<FileCopyRequest> create(FileCopyRequestDTO requestDto, String requestId) {
        // Check a same request already exists
        Optional<FileCopyRequest> request = copyRepository.findOneByMetaInfoChecksumAndStorage(requestDto.getChecksum(),
                                                                                               requestDto.getStorage());
        if (request.isPresent()) {
            return Optional.of(handleAlreadyExists(requestDto, request.get(), requestId));
        } else {
            // get file meta info to copy
            Set<FileReference> refs = fileRefService.search(requestDto.getChecksum());
            if (refs.isEmpty()) {
                String message = String
                        .format("File copy request refused for file %s to %s storage location. Cause file does not exists in any known storage location.",
                                requestDto.getChecksum(), requestDto.getStorage());
                LOGGER.warn(message);
                notificationClient.notify(message, "File copy request refused", NotificationLevel.WARNING,
                                          DefaultRole.PROJECT_ADMIN);
            } else {
                FileCopyRequest newRequest = copyRepository
                        .save(new FileCopyRequest(requestId, refs.stream().findFirst().get().getMetaInfo(),
                                requestDto.getSubDirectory(), requestDto.getStorage()));
                request = Optional.of(newRequest);
            }
        }
        return request;
    }

    private FileCopyRequest handleAlreadyExists(FileCopyRequestDTO requestDto, FileCopyRequest request,
            String newRequestId) {
        if (request.getStatus() == FileRequestStatus.ERROR) {
            request.setStatus(FileRequestStatus.TODO);
            request.setFileCacheRequestId(newRequestId);
            return update(request);
        }
        return request;
    }

    public void scheduleAvailabilityRequests(FileRequestStatus status) {
        Pageable page = PageRequest.of(0, NB_REFERENCE_BY_PAGE, Direction.ASC, "id");
        Page<FileCopyRequest> pageResp = null;
        OffsetDateTime expDate = OffsetDateTime.now().plusDays(1);
        do {
            String fileCacheRequestId = UUID.randomUUID().toString();
            Set<String> checksums = Sets.newHashSet();
            pageResp = copyRepository.findByStatus(status, page);
            for (FileCopyRequest request : pageResp.getContent()) {
                checksums.add(request.getMetaInfo().getChecksum());
                request.setFileCacheRequestId(fileCacheRequestId);
                request.setStatus(FileRequestStatus.PENDING);
            }
            if (!checksums.isEmpty()) {
                fileRefService.makeAvailable(checksums, expDate, fileCacheRequestId);
            }
            page = page.next();
        } while (pageResp.hasNext());
    }

    public void handleSuccess(FileCopyRequest request) {
        LOGGER.info("[COPY SUCCESS] File {} (checksum: {}) successfully copied in {} storage location",
                    request.getMetaInfo().getFileName(), request.getMetaInfo().getChecksum(), request.getStorage());
        // Delete the copy request
        copyRepository.delete(request);

        // Check if associated cache file is always present
        Optional<CacheFile> oCf = cacheService.getCacheFile(request.getMetaInfo().getChecksum());
        if (oCf.isPresent()) {
            // If it is present, check if an other availability request was used for this
            if (!oCf.get().getRequestIds().stream().anyMatch(id -> !id.equals(request.getFileCacheRequestId()))) {
                // If not, delete the cache file
                cacheService.delete(oCf.get());
            }
        }
    }

    public void handleError(FileCopyRequest request, String errorCause) {
        LOGGER.info("[COPY ERROR] Error copying file {} (checksum: {}) to {} storage location. Cause : {}",
                    request.getMetaInfo().getFileName(), request.getMetaInfo().getChecksum(), request.getStorage(),
                    errorCause);
        // Update copy request to error status
        request.setStatus(FileRequestStatus.ERROR);
        request.setErrorCause(errorCause);
        update(request);
    }

    @Transactional(readOnly = true)
    public Optional<FileCopyRequest> search(String checksum, String storage) {
        return copyRepository.findOneByMetaInfoChecksumAndStorage(checksum, storage);
    }

    public Optional<FileCopyRequest> search(FileReferenceEvent event) {
        Optional<FileCopyRequest> req = Optional.empty();
        Iterator<String> it;
        switch (event.getType()) {
            case AVAILABLE:
            case AVAILABILITY_ERROR:
                it = event.getRequestIds().iterator();
                while (it.hasNext() && !req.isPresent()) {
                    req = copyRepository.findByFileCacheRequestId(it.next());
                }
                break;
            case STORED:
            case STORE_ERROR:
                it = event.getRequestIds().iterator();
                while (it.hasNext() && !req.isPresent()) {
                    req = copyRepository.findByFileStorageRequestId(it.next());
                }
                break;
            case DELETED_FOR_OWNER:
            case FULLY_DELETED:
            case DELETION_ERROR:
            default:
                break;
        }
        return req;
    }

    public FileCopyRequest update(FileCopyRequest request) {
        return copyRepository.save(request);
    }
}
