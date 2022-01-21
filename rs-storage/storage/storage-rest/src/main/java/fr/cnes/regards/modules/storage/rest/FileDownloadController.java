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
package fr.cnes.regards.modules.storage.rest;

import com.google.common.annotations.VisibleForTesting;
import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.storage.domain.DownloadableFile;
import fr.cnes.regards.modules.storage.domain.database.FileReference;
import fr.cnes.regards.modules.storage.service.file.FileDownloadService;
import fr.cnes.regards.modules.storage.service.file.download.IQuotaExceededReporter;
import fr.cnes.regards.modules.storage.service.file.download.IQuotaService;
import fr.cnes.regards.modules.storage.service.file.exception.DownloadLimitExceededException;
import io.vavr.control.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.util.concurrent.Callable;
import java.util.function.Function;

/**
 * Controller to access {@link FileReference} by rest API.
 *
 * @author Sébastien Binda
 */
@RestController
@RequestMapping(FileDownloadController.DOWNLOAD_RESOURCE_PATH)
public class FileDownloadController {

    public static final String DOWNLOAD_RESOURCE_PATH = "resources";

    public static final String DOWNLOAD_PATH = "/{checksum}/download";

    private static final Logger LOGGER = LoggerFactory.getLogger(FileDownloadController.class);

    @Autowired
    private FileDownloadService downloadService;

    @Autowired
    private IQuotaService<ResponseEntity<Resource>> downloadQuotaService;

    @Autowired
    private IQuotaExceededReporter<DownloadableFile> quotaExceededReporter;

    @Autowired
    private IRuntimeTenantResolver tenantResolver;

    @Autowired
    private IAuthenticationResolver authResolver;

    /**
     * End-point to Download a file referenced by a storage location with the given checksum.
     *
     * @param checksum checksum of the file to download
     * @return {@link Resource}
     */
    @RequestMapping(path = DOWNLOAD_PATH, method = RequestMethod.GET, produces = MediaType.ALL_VALUE)
    @ResourceAccess(description = "Download one file by checksum.", role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<Resource> downloadFile(@PathVariable("checksum") String checksum,
            @RequestParam(name = "isContentInline", required = false) Boolean isContentInline) {
        return downloadWithQuota(checksum, isContentInline).recover(EntityOperationForbiddenException.class, t -> {
            LOGGER.error(String.format("File %s is not downloadable for now. Try again later.", checksum));
            LOGGER.debug(t.getMessage(), t);
            return new ResponseEntity<>(HttpStatus.ACCEPTED);
        }).recover(EntityNotFoundException.class, t -> {
            LOGGER.warn(String.format(
                    "Unable to download file with checksum=%s. Cause file does not exists on any known storage location",
                    checksum));
            LOGGER.debug(t.getMessage(), t);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }).recover(ModuleException.class, t -> {
            LOGGER.error(t.getMessage(), t);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }).get();
    }

    /**
     * End-point to Download a file referenced by a storage location with the given checksum.
     *
     * @param checksum checksum of the file to download
     * @return {@link Resource}
     */
    @RequestMapping(path = FileDownloadService.DOWNLOAD_TOKEN_PATH, method = RequestMethod.GET,
            produces = MediaType.ALL_VALUE)
    @ResourceAccess(description = "Download one file by checksum.", role = DefaultRole.PUBLIC)
    public ResponseEntity<Resource> downloadFileWithToken(@PathVariable("checksum") String checksum,
            @RequestParam(name = FileDownloadService.TOKEN_PARAM) String token, boolean isContentInline) {
        if (!downloadService.checkToken(checksum, token)) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
        // Do not check for quota, because this endpoint needs to be used internally (storage -> storage) during copy process
        // with no specific users (public access).
        return Try.of(() -> downloadService.downloadFile(checksum)).mapTry(Callable::call)
                .flatMap(dlFile -> downloadFile(dlFile, isContentInline)).recover(ModuleException.class, t -> {
                    LOGGER.error(t.getMessage());
                    return new ResponseEntity<>(HttpStatus.NOT_FOUND);
                }).get();
    }

    @VisibleForTesting
    protected Try<ResponseEntity<Resource>> downloadWithQuota(String checksum, Boolean isContentInline) {
        return Try.of(() -> downloadService.downloadFile(checksum)).mapTry(Callable::call).flatMap(dlFile -> {
            if (dlFile instanceof FileDownloadService.QuotaLimitedDownloadableFile) {
                return downloadQuotaService.withQuota(authResolver.getUser(), (quotaHandler) -> Try
                        .success((FileDownloadService.QuotaLimitedDownloadableFile) dlFile)
                        .map(impureId(quotaHandler::start)) // map instead of peek to wrap potential errors
                        .map(d -> DownloadableFileWrapper.wrap(d, quotaHandler)).flatMap(d -> downloadFile(d,
                                                                                   isContentInline))) // idempotent close of stream (and quotaHandler) if anything failed, just in case
                        .onFailure(ignored -> Try.run(dlFile::close))
                        .recover(DownloadLimitExceededException.class, t -> {
                            quotaExceededReporter.report(t, dlFile, authResolver.getUser(), tenantResolver.getTenant());
                            return new ResponseEntity<>(
                                    new InputStreamResource(new ByteArrayInputStream(t.getMessage().getBytes())),
                                    HttpStatus.TOO_MANY_REQUESTS);
                        });
            }
            // no quota handling, just download
            return downloadFile(dlFile, isContentInline);
        });
    }

    private <T> Function<T, T> impureId(Runnable action) {
        return x -> {
            action.run();
            return x;
        };
    }

    @VisibleForTesting
    protected Try<ResponseEntity<Resource>> downloadFile(DownloadableFile downloadFile, Boolean isContentInline) {
        return Try.of(() -> {
            HttpHeaders headers = new HttpHeaders();
            MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM; // Default value
            // By default, return the attachment header, forcing browser to download the file
            if (isContentInline == null || !isContentInline) {
                headers.add(HttpHeaders.CONTENT_DISPOSITION,
                            ContentDisposition.builder("attachment").filename(downloadFile.getFileName()).build()
                                    .toString());
            } else {
                // Override media type to get exact one
                mediaType = MediaType.asMediaType(downloadFile.getMimeType());
                headers.add(HttpHeaders.CONTENT_DISPOSITION,
                            ContentDisposition.builder("inline").filename(downloadFile.getFileName()).build()
                                    .toString());
                // Allows iframe to display inside REGARDS interface
                headers.add("X-Frame-Options", "SAMEORIGIN");
            }
            return ResponseEntity.ok().headers(headers).contentType(mediaType)
                    .contentLength(downloadFile.getRealFileSize().intValue())
                    .body(new InputStreamResource(downloadFile.getFileInputStream()));
        });
    }

}
