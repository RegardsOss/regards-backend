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
import com.google.common.collect.Sets;
import com.google.common.net.HttpHeaders;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.fileaccess.dto.FileReferenceDto;
import fr.cnes.regards.modules.fileaccess.plugin.domain.NearlineFileNotAvailableException;
import fr.cnes.regards.modules.filecatalog.amqp.input.FilesStorageRequestEvent;
import fr.cnes.regards.modules.storage.domain.DownloadableFile;
import fr.cnes.regards.modules.storage.domain.database.FileReference;
import fr.cnes.regards.modules.storage.service.DownloadTokenService;
import fr.cnes.regards.modules.storage.service.file.FileDownloadService;
import fr.cnes.regards.modules.storage.service.file.FileReferenceService;
import fr.cnes.regards.modules.storage.service.file.download.IQuotaExceededReporter;
import fr.cnes.regards.modules.storage.service.file.download.IQuotaService;
import fr.cnes.regards.modules.storage.service.file.exception.DownloadLimitExceededException;
import fr.cnes.regards.modules.storage.service.file.handler.FilesStorageRequestEventHandler;
import io.vavr.control.Try;
import org.apache.commons.compress.utils.Lists;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Controller to access {@link FileReference} by rest API.
 *
 * @author Sébastien Binda
 */
@RestController
@RequestMapping(FileReferenceController.FILE_PATH)
public class FileReferenceController {

    public static final String FILE_PATH = FileDownloadService.FILES_PATH;

    public static final String DOWNLOAD_PATH = "/{checksum}/download";

    public static final String STORE_PATH = "/store";

    public static final String EXPORT_PATH = "/csv";

    public static final String LOCATIONS_PATH = "/{storage}/locations";

    private static final Logger LOGGER = LoggerFactory.getLogger(FileReferenceController.class);

    @Autowired
    private FileDownloadService downloadService;

    @Autowired
    private DownloadTokenService downloadTokenService;

    @Autowired
    private FileReferenceService fileRefService;

    @Autowired
    private IQuotaService<ResponseEntity<StreamingResponseBody>> downloadQuotaService;

    @Autowired
    private IQuotaExceededReporter<DownloadableFile> quotaExceededReporter;

    @Autowired
    private IRuntimeTenantResolver tenantResolver;

    @Autowired
    private IAuthenticationResolver authResolver;

    @Autowired
    private FilesStorageRequestEventHandler storageHandler;

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
                    LOGGER.error("Exception while reading and streaming data of file url=[{}]",
                                 downloadFile.getFileName(),
                                 e);
                } finally {
                    downloadFile.close();
                }
            };
            return new ResponseEntity<>(stream, HttpStatus.OK);
        });
    }

    @RequestMapping(method = RequestMethod.GET, path = EXPORT_PATH)
    @ResourceAccess(description = "Export all file referenced in csv file", role = DefaultRole.PROJECT_ADMIN)
    public void export(HttpServletResponse response) throws IOException {
        response.addHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=files.csv");
        response.setContentType("text/csv");
        BufferedWriter writer = new BufferedWriter(response.getWriter());
        CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader("id", "url", "storage", "owners"));
        Pageable page = null;
        Page<FileReference> results;
        do {
            if (page == null) {
                page = PageRequest.of(0, 100);
            } else {
                page = page.next();
            }
            results = fileRefService.searchWithOwners(page);
            for (FileReference fileRef : results.getContent()) {
                printer.printRecord(fileRef.getId(),
                                    fileRef.getLocation().getUrl(),
                                    fileRef.getLocation().getStorage(),
                                    fileRef.getLazzyOwners().stream().collect(Collectors.joining(",")));
            }
        } while (results.hasNext());
        printer.close();
        writer.close();
    }

    @RequestMapping(method = RequestMethod.POST, path = LOCATIONS_PATH)
    @ResourceAccess(description = "Get file references with matching checksums on a storage",
                    role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<Set<FileReferenceDto>> getFileReferencesWithoutOwners(
        @PathVariable(name = "storage") final String storage, @RequestBody final Set<String> checksums) {
        Set<FileReferenceDto> fileRefDtos = Sets.newHashSet();
        Set<FileReference> fileRefs = fileRefService.search(storage, checksums);
        fileRefs.forEach(fileRef -> fileRefDtos.add(new FileReferenceDto(fileRef.getStorageDate(),
                                                                         fileRef.getMetaInfo().toDto(),
                                                                         fileRef.getLocation().toDto(),
                                                                         Lists.newArrayList())));
        return ResponseEntity.ok(fileRefDtos);
    }

    @RequestMapping(method = RequestMethod.POST, path = STORE_PATH)
    @ResourceAccess(description = "Configure a storage location by his name", role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<Void> store(@Valid @RequestBody Collection<FilesStorageRequestEvent> items) {
        items.stream().map(i -> TenantWrapper.build(i, tenantResolver.getTenant())).forEach(storageHandler::handle);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
