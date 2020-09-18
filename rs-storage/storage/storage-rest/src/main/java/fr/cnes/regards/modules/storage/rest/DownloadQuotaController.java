/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.storage.domain.database.DownloadQuotaLimits;
import fr.cnes.regards.modules.storage.domain.database.UserCurrentQuotas;
import fr.cnes.regards.modules.storage.domain.database.repository.IDownloadQuotaRepository;
import fr.cnes.regards.modules.storage.domain.dto.quota.DownloadQuotaLimitsDto;
import fr.cnes.regards.modules.storage.service.file.download.IQuotaService;
import io.vavr.control.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import javax.validation.Valid;

@RestController
public class DownloadQuotaController {

    private static final Logger LOGGER = LoggerFactory.getLogger(DownloadQuotaController.class);

    public static final String PATH_QUOTA = "/quota";

    public static final String PATH_CURRENT_QUOTA = "/quota/current";

    @Autowired
    private IDownloadQuotaRepository quotaRepository;

    @Autowired
    private IQuotaService<ResponseEntity<StreamingResponseBody>> quotaService;

    @Autowired
    private IRuntimeTenantResolver tenantResolver;

    @Autowired
    private IAuthenticationResolver authResolver;

    @RequestMapping(method = RequestMethod.POST, path = PATH_QUOTA)
    @ResponseBody
    public ResponseEntity<DownloadQuotaLimitsDto> createQuotaLimits(@Valid @RequestBody DownloadQuotaLimitsDto toBeCreated) {
        return Try.of(() ->
            quotaRepository.save(
                new DownloadQuotaLimits(
                    tenantResolver.getTenant(),
                    toBeCreated.getEmail(),
                    toBeCreated.getMaxQuota(),
                    toBeCreated.getRateLimit()
                )
            ))
            .map(DownloadQuotaLimitsDto::fromDownloadQuotaLimits)
            .map(dto -> new ResponseEntity<>(dto, HttpStatus.CREATED))
            .get();
    }

    @RequestMapping(method = RequestMethod.GET, path = PATH_QUOTA)
    @ResponseBody
    public ResponseEntity<DownloadQuotaLimitsDto> getQuotaLimits(@Valid String userEmail) {
        return quotaRepository.findByEmail(userEmail)
            .map(DownloadQuotaLimitsDto::fromDownloadQuotaLimits)
            .map(dto -> new ResponseEntity<>(dto, HttpStatus.OK))
            .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @RequestMapping(method = RequestMethod.GET, path = PATH_CURRENT_QUOTA)
    @ResponseBody
    public ResponseEntity<UserCurrentQuotas> getCurrentQuotas(@Valid String userEmail) {
        return new ResponseEntity<>(
            quotaService.getCurrentQuotas(userEmail),
            HttpStatus.OK
        );
    }

//    /**
//     * End-point to Download a file referenced by a storage location with the given checksum.
//     * @param checksum checksum of the file to download
//     * @return {@link InputStreamResource}
//     */
//    @RequestMapping(path = DOWNLOAD_PATH, method = RequestMethod.GET, produces = MediaType.ALL_VALUE)
//    @ResourceAccess(description = "Download one file by checksum.", role = DefaultRole.PROJECT_ADMIN)
//    public ResponseEntity<StreamingResponseBody> downloadFile(
//        @PathVariable("checksum") String checksum,
//        HttpServletResponse response)
//    {
//        return downloadWithQuota(checksum, response)
//            .recover(EntityOperationForbiddenException.class, t -> {
//                LOGGER.error(String.format("File %s is not downloadable for now. Try again later.", checksum));
//                LOGGER.debug(t.getMessage(), t);
//                return new ResponseEntity<>(HttpStatus.ACCEPTED);
//            }).recover(EntityNotFoundException.class, t -> {
//                LOGGER.warn(String
//                    .format("Unable to download file with checksum=%s. Cause file does not exists on any known storage location",
//                        checksum));
//                LOGGER.debug(t.getMessage(), t);
//                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
//            }).recover(ModuleException.class, t -> {
//                LOGGER.error(t.getMessage(), t);
//                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
//            }).get();
//    }
//
//    /**
//     * End-point to Download a file referenced by a storage location with the given checksum.
//     * @param checksum checksum of the file to download
//     * @return {@link InputStreamResource}
//     */
//    @RequestMapping(path = FileDownloadService.DOWNLOAD_TOKEN_PATH, method = RequestMethod.GET,
//            produces = MediaType.ALL_VALUE)
//    @ResourceAccess(description = "Download one file by checksum.", role = DefaultRole.PUBLIC)
//    public ResponseEntity<StreamingResponseBody> downloadFileWithToken(
//        @PathVariable("checksum") String checksum,
//        @RequestParam(name = FileDownloadService.TOKEN_PARAM, required = true) String token,
//        HttpServletResponse response)
//    {
//        if (! downloadService.checkToken(checksum, token)) {
//            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
//        }
//        return downloadWithQuota(checksum, response)
//            .recover(ModuleException.class, t -> {
//                LOGGER.error(t.getMessage());
//                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
//            }).get();
//    }
//
//    @RequestMapping(method = RequestMethod.GET, path = EXPORT_PATH)
//    @ResourceAccess(description = "Export all file referenced in csv file", role = DefaultRole.PROJECT_ADMIN)
//    public void export(HttpServletResponse response) throws IOException {
//        response.addHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=files.csv");
//        response.setContentType("text/csv");
//        BufferedWriter writer = new BufferedWriter(response.getWriter());
//        CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader("id", "url", "storage", "owners"));
//        Pageable page = null;
//        Page<FileReference> results;
//        do {
//            if (page == null) {
//                page = PageRequest.of(0, 100);
//            } else {
//                page = page.next();
//            }
//            results = fileRefService.search(page);
//            for (FileReference fileRef : results.getContent()) {
//                printer.printRecord(fileRef.getId(), fileRef.getLocation().getUrl(), fileRef.getLocation().getStorage(),
//                                    fileRef.getOwners().stream().collect(Collectors.joining(",")));
//            }
//        } while (results.hasNext());
//        printer.close();
//        writer.close();
//    }
//
//    @RequestMapping(method = RequestMethod.POST, path = STORE_PATH)
//    @ResourceAccess(description = "Configure a storage location by his name", role = DefaultRole.PROJECT_ADMIN)
//    public ResponseEntity<Void> store(@Valid @RequestBody Collection<StorageFlowItem> items) {
//        items.stream().map(i -> TenantWrapper.build(i, tenantResolver.getTenant())).forEach(storageHandler::handle);
//        return new ResponseEntity<>(HttpStatus.OK);
//    }

}
