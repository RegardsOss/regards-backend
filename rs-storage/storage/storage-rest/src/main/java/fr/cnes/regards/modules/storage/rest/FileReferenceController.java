/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MimeType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.google.common.annotations.VisibleForTesting;

import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.storage.domain.DownloadableFile;
import fr.cnes.regards.modules.storage.domain.database.FileReference;
import fr.cnes.regards.modules.storage.domain.flow.StorageFlowItem;
import fr.cnes.regards.modules.storage.service.file.FileDownloadService;
import fr.cnes.regards.modules.storage.service.file.FileReferenceService;
import fr.cnes.regards.modules.storage.service.file.download.IQuotaExceededReporter;
import fr.cnes.regards.modules.storage.service.file.download.IQuotaService;
import fr.cnes.regards.modules.storage.service.file.exception.DownloadLimitExceededException;
import fr.cnes.regards.modules.storage.service.file.flow.StorageFlowItemHandler;
import io.vavr.control.Try;

/**
 * Controller to access {@link FileReference} by rest API.
 * @author Sébastien Binda
 */
@RestController
@RequestMapping(FileReferenceController.FILE_PATH)
public class FileReferenceController {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileReferenceController.class);

    public static final String FILE_PATH = FileDownloadService.FILES_PATH;

    public static final String DOWNLOAD_PATH = "/{checksum}/download";

    public static final String STORE_PATH = "/store";

    public static final String EXPORT_PATH = "/csv";

    @Autowired
    private FileDownloadService downloadService;

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
    private StorageFlowItemHandler storageHandler;

    /**
     * End-point to Download a file referenced by a storage location with the given checksum.
     * @param checksum checksum of the file to download
     * @return {@link InputStreamResource}
     */
    @RequestMapping(path = DOWNLOAD_PATH, method = RequestMethod.GET, produces = MediaType.ALL_VALUE)
    @ResourceAccess(description = "Download one file by checksum.", role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<StreamingResponseBody> downloadFile(@PathVariable("checksum") String checksum,
            HttpServletResponse response) {
        return downloadWithQuota(checksum, response).recover(EntityOperationForbiddenException.class, t -> {
            LOGGER.error(String.format("File %s is not downloadable for now. Try again later.", checksum));
            LOGGER.debug(t.getMessage(), t);
            return new ResponseEntity<>(HttpStatus.ACCEPTED);
        }).recover(EntityNotFoundException.class, t -> {
            LOGGER.warn(String
                    .format("Unable to download file with checksum=%s. Cause file does not exists on any known storage location",
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
     * @param checksum checksum of the file to download
     * @return {@link InputStreamResource}
     */
    @RequestMapping(path = FileDownloadService.DOWNLOAD_TOKEN_PATH, method = RequestMethod.GET,
            produces = MediaType.ALL_VALUE)
    @ResourceAccess(description = "Download one file by checksum.", role = DefaultRole.PUBLIC)
    public ResponseEntity<StreamingResponseBody> downloadFileWithToken(@PathVariable("checksum") String checksum,
            @RequestParam(name = FileDownloadService.TOKEN_PARAM, required = true) String token,
            HttpServletResponse response) {
        if (!downloadService.checkToken(checksum, token)) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
        return downloadWithQuota(checksum, response).recover(ModuleException.class, t -> {
            LOGGER.error(t.getMessage());
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }).get();
    }

    @VisibleForTesting
    protected Try<ResponseEntity<StreamingResponseBody>> downloadWithQuota(String checksum,
            HttpServletResponse response) {
        return Try.of(() -> downloadService.downloadFile(checksum)).mapTry(Callable::call).flatMap(dlFile -> {
            if (dlFile instanceof FileDownloadService.QuotaLimitedDownloadableFile) {
                return downloadQuotaService
                        .withQuota(authResolver.getUser(),
                                   (quotaHandler) -> Try
                                           .success((FileDownloadService.QuotaLimitedDownloadableFile) dlFile)
                                           .map(impureId(quotaHandler::start)) // map instead of peek to wrap potential errors
                                           .map(d -> wrap(d, quotaHandler)).flatMap(d -> downloadFile(d, response))) // idempotent close of stream (and quotaHandler) if anything failed, just in case
                        .onFailure(ignored -> Try.run(dlFile::close))
                        .recover(DownloadLimitExceededException.class, t -> {
                            quotaExceededReporter.report(t, dlFile, authResolver.getUser(), tenantResolver.getTenant());
                            return new ResponseEntity<>(outputStream -> outputStream.write(t.getMessage().getBytes()),
                                    HttpStatus.TOO_MANY_REQUESTS);
                        });
            }
            // no quota handling, just download
            return downloadFile(dlFile, response);
        });
    }

    private <T> Function<T, T> impureId(Runnable action) {
        return x -> {
            action.run();
            return x;
        };
    }

    private static class DownloadableFileWrapper extends FileDownloadService.QuotaLimitedDownloadableFile {

        private final FileDownloadService.QuotaLimitedDownloadableFile dlFile;

        private final IQuotaService.WithQuotaOperationHandler quotaHandler;

        private DownloadableFileWrapper(FileDownloadService.QuotaLimitedDownloadableFile dlFile,
                IQuotaService.WithQuotaOperationHandler quotaHandler) {
            super(dlFile.getFileInputStream(), dlFile.getRealFileSize(), dlFile.getFileName(), dlFile.getMimeType());
            this.dlFile = dlFile;
            this.quotaHandler = quotaHandler;
        }

        @Override
        public void close() {
            Try.run(quotaHandler::stop);
            dlFile.close();
        }
    }

    static DownloadableFileWrapper wrap(FileDownloadService.QuotaLimitedDownloadableFile dlFile,
            IQuotaService.WithQuotaOperationHandler quotaHandler) {
        return new DownloadableFileWrapper(dlFile, quotaHandler);
    }

    @VisibleForTesting
    protected Try<ResponseEntity<StreamingResponseBody>> downloadFile(DownloadableFile downloadFile,
            HttpServletResponse response) {
        return Try.of(() -> {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentLength(downloadFile.getRealFileSize());
            headers.setContentType(asMediaType(downloadFile.getMimeType()));
            headers.setContentDisposition(ContentDisposition.builder("attachment").filename(downloadFile.getFileName())
                    .size(downloadFile.getRealFileSize()).build());
            StreamingResponseBody stream = out -> {
                try (OutputStream outs = response.getOutputStream()) {
                    byte[] bytes = new byte[1024];
                    int length;
                    while ((length = downloadFile.getFileInputStream().read(bytes)) >= 0) {
                        outs.write(bytes, 0, length);
                    }
                } catch (final IOException e) {
                    LOGGER.error("Exception while reading and streaming data", e);
                } finally {
                    downloadFile.close();
                }
            };
            return new ResponseEntity<>(stream, headers, HttpStatus.OK);
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
                printer.printRecord(fileRef.getId(), fileRef.getLocation().getUrl(), fileRef.getLocation().getStorage(),
                                    fileRef.getLazzyOwners().stream().collect(Collectors.joining(",")));
            }
        } while (results.hasNext());
        printer.close();
        writer.close();
    }

    @RequestMapping(method = RequestMethod.POST, path = STORE_PATH)
    @ResourceAccess(description = "Configure a storage location by his name", role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<Void> store(@Valid @RequestBody Collection<StorageFlowItem> items) {
        items.stream().map(i -> TenantWrapper.build(i, tenantResolver.getTenant())).forEach(storageHandler::handle);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    private static MediaType asMediaType(MimeType mimeType) {
        if (mimeType instanceof MediaType) {
            return (MediaType) mimeType;
        }
        return new MediaType(mimeType.getType(), mimeType.getSubtype(), mimeType.getParameters());
    }
}
