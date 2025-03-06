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
package fr.cnes.regards.modules.storage.rest;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.net.HttpHeaders;
import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.fileaccess.dto.availability.FileAvailabilityStatusDto;
import fr.cnes.regards.modules.fileaccess.dto.availability.FilesAvailabilityRequestDto;
import fr.cnes.regards.modules.fileaccess.plugin.domain.NearlineFileNotAvailableException;
import fr.cnes.regards.modules.storage.domain.DownloadableFile;
import fr.cnes.regards.modules.storage.domain.database.FileReference;
import fr.cnes.regards.modules.storage.service.DownloadTokenService;
import fr.cnes.regards.modules.storage.service.availability.FileAvailabilityService;
import fr.cnes.regards.modules.storage.service.file.FileDownloadService;
import fr.cnes.regards.modules.storage.service.file.download.IQuotaExceededReporter;
import fr.cnes.regards.modules.storage.service.file.download.IQuotaService;
import fr.cnes.regards.modules.storage.service.file.exception.DownloadLimitExceededException;
import io.jsonwebtoken.io.IOException;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.vavr.control.Try;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.OutputStream;
import java.util.List;
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

    public static final String DOWNLOAD_PATH = "/{checksum}/download";

    public static final String DOWNLOAD_RESOURCE_PATH = "/files";

    public static final String STATUS_AVAILABILITY_PATH = "/availability/status";

    private static final Logger LOGGER = LoggerFactory.getLogger(FileReferenceController.class);

    @Autowired
    private FileDownloadService downloadService;

    @Autowired
    private DownloadTokenService downloadTokenService;

    @Autowired
    private IQuotaService<ResponseEntity<StreamingResponseBody>> downloadQuotaService;

    @Autowired
    private IQuotaExceededReporter<DownloadableFile> quotaExceededReporter;

    @Autowired
    private IRuntimeTenantResolver tenantResolver;

    @Autowired
    private IAuthenticationResolver authResolver;

    @Autowired
    private FileAvailabilityService availabilityService;

    /**
     * End-point to Download a file referenced by a storage location with the given checksum.
     *
     * @param checksum checksum of the file to download
     * @return {@link InputStreamResource}
     */
    @RequestMapping(path = DOWNLOAD_PATH, method = RequestMethod.GET, produces = MediaType.ALL_VALUE)
    @ResourceAccess(description = "Download one file by checksum.", role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<StreamingResponseBody> downloadFile(@PathVariable("checksum") String checksum,
                                                              @RequestParam(name = "isContentInline", required = false)
                                                              Boolean isContentInline,
                                                              HttpServletResponse response) {
        return downloadWithQuota(checksum, isContentInline, response).recover(EntityOperationForbiddenException.class,
                                                                              t -> {
                                                                                  LOGGER.error(String.format(
                                                                                      "File %s is not downloadable for now. Try again later.",
                                                                                      checksum));
                                                                                  LOGGER.debug(t.getMessage(), t);
                                                                                  return new ResponseEntity<>(HttpStatus.ACCEPTED);
                                                                              })
                                                                     .recover(EntityNotFoundException.class, t -> {
                                                                         LOGGER.warn(String.format(
                                                                             "Unable to download file with checksum=%s. Cause file does not exists on any known storage location",
                                                                             checksum));
                                                                         LOGGER.debug(t.getMessage(), t);
                                                                         return new ResponseEntity<>(HttpStatus.NOT_FOUND);
                                                                     })
                                                                     .recover(NearlineFileNotAvailableException.class,
                                                                              t -> {
                                                                                  LOGGER.warn(String.format(
                                                                                      "Unable to download nearline file with checksum=%s. Cause file is expired or does not exists on any known storage location",
                                                                                      checksum));
                                                                                  LOGGER.debug(t.getMessage(), t);
                                                                                  return new ResponseEntity<>(HttpStatus.GONE);
                                                                              })
                                                                     .recover(ModuleException.class, t -> {
                                                                         LOGGER.error(t.getMessage(), t);
                                                                         return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
                                                                     })
                                                                     .get();
    }

    /**
     * End-point to Download a file referenced by a storage location with the given checksum.
     *
     * @param checksum checksum of the file to download
     * @return {@link InputStreamResource}
     */
    @RequestMapping(path = FileDownloadService.DOWNLOAD_TOKEN_PATH,
                    method = RequestMethod.GET,
                    produces = MediaType.ALL_VALUE)
    @ResourceAccess(description = "Download one file by checksum.", role = DefaultRole.PUBLIC)
    public ResponseEntity<StreamingResponseBody> downloadFileWithToken(@PathVariable("checksum") String checksum,
                                                                       @RequestParam(name = FileDownloadService.TOKEN_PARAM,
                                                                                     required = true) String token,
                                                                       boolean isContentInline,
                                                                       HttpServletResponse response) {
        if (!downloadTokenService.checkToken(checksum, token)) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
        // Do not check for quota, because this endpoint needs to be used internally (storage -> storage) during copy process
        // with no specific users (public access).
        return Try.of(() -> downloadService.downloadFile(checksum))
                  .mapTry(Callable::call)
                  .flatMap(dlFile -> downloadFile(dlFile, isContentInline, response))
                  .recover(NearlineFileNotAvailableException.class, t -> {
                      LOGGER.warn(String.format(
                          "Unable to download nearline file with checksum=%s. Cause file is expired or does not exists on any known storage location",
                          checksum));
                      LOGGER.debug(t.getMessage(), t);
                      return new ResponseEntity<>(HttpStatus.GONE);
                  })
                  .recover(ModuleException.class, t -> {
                      LOGGER.error(t.getMessage());
                      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
                  })
                  .get();
    }

    @VisibleForTesting
    protected Try<ResponseEntity<StreamingResponseBody>> downloadWithQuota(String checksum,
                                                                           Boolean isContentInline,
                                                                           HttpServletResponse response) {
        return Try.of(() -> downloadService.downloadFile(checksum)).mapTry(Callable::call).flatMap(dlFile -> {
            if (dlFile instanceof FileDownloadService.QuotaLimitedDownloadableFile) {
                return downloadQuotaService.withQuota(authResolver.getUser(),
                                                      (quotaHandler) -> Try.success((FileDownloadService.QuotaLimitedDownloadableFile) dlFile)
                                                                           .map(impureId(quotaHandler::start)) // map instead of peek to wrap potential errors
                                                                           .map(d -> DownloadableFileWrapper.wrap(d,
                                                                                                                  quotaHandler))
                                                                           .flatMap(d -> downloadFile(d,
                                                                                                      isContentInline,
                                                                                                      response))) // idempotent close of stream (and quotaHandler) if anything failed, just in case
                                           .onFailure(ignored -> Try.run(dlFile::close))
                                           .recover(DownloadLimitExceededException.class, t -> {
                                               quotaExceededReporter.report(t,
                                                                            dlFile,
                                                                            authResolver.getUser(),
                                                                            tenantResolver.getTenant());
                                               return new ResponseEntity<>(outputStream -> outputStream.write(t.getMessage()
                                                                                                               .getBytes()),
                                                                           HttpStatus.TOO_MANY_REQUESTS);
                                           });

            }
            // no quota handling, just download
            return downloadFile(dlFile, isContentInline, response);
        });
    }

    private <T> Function<T, T> impureId(Runnable action) {
        return x -> {
            action.run();
            return x;
        };
    }

    @VisibleForTesting
    protected Try<ResponseEntity<StreamingResponseBody>> downloadFile(DownloadableFile downloadFile,
                                                                      Boolean isContentInline,
                                                                      HttpServletResponse response) {
        return Try.of(() -> {
            response.setContentLengthLong(downloadFile.getRealFileSize());
            response.setContentType(downloadFile.getMimeType().toString());
            // By default, return the attachment header, forcing browser to download the file
            if (isContentInline == null || !isContentInline) {
                response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                                   ContentDisposition.builder("attachment")
                                                     .filename(downloadFile.getFileName())
                                                     .size(downloadFile.getRealFileSize())
                                                     .build()
                                                     .toString());
            } else {
                response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                                   ContentDisposition.builder("inline")
                                                     .filename(downloadFile.getFileName())
                                                     .size(downloadFile.getRealFileSize())
                                                     .build()
                                                     .toString());
                // Allows iframe to display inside REGARDS interface
                response.setHeader(HttpHeaders.X_FRAME_OPTIONS, "SAMEORIGIN");
            }
            StreamingResponseBody stream = out -> {
                try (OutputStream outs = response.getOutputStream()) {
                    byte[] bytes = new byte[1024];
                    int length;
                    while ((length = downloadFile.getFileInputStream().read(bytes)) >= 0) {
                        outs.write(bytes, 0, length);
                    }
                } catch (final IOException e) {
                    LOGGER.error("Exception while reading and streaming data of file url=[{}] size={}",
                                 downloadFile.getFileName(),
                                 downloadFile.getRealFileSize(),
                                 e);
                } finally {
                    downloadFile.close();
                }
            };
            return new ResponseEntity<>(stream, HttpStatus.OK);
        });
    }

    @PostMapping(path = STATUS_AVAILABILITY_PATH)
    @ApiResponses(value = { @ApiResponse(responseCode = "200",
                                         description = "List of availability status of input files. If any file is "
                                                       + "not present in response (without error), that means "
                                                       + "that file is not found.") })
    public ResponseEntity<List<FileAvailabilityStatusDto>> checkFileAvailability(
        @RequestBody FilesAvailabilityRequestDto filesAvailabilityRequestDto) throws EntityInvalidException {
        return new ResponseEntity<>(availabilityService.checkFileAvailability(filesAvailabilityRequestDto),
                                    HttpStatus.OK);
    }

}
