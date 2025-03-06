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

import com.google.common.collect.Sets;
import com.google.common.net.HttpHeaders;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.fileaccess.dto.FileReferenceDto;
import fr.cnes.regards.modules.filecatalog.amqp.input.FilesStorageRequestEvent;
import fr.cnes.regards.modules.storage.domain.database.FileReference;
import fr.cnes.regards.modules.storage.service.file.FileDownloadService;
import fr.cnes.regards.modules.storage.service.file.FileReferenceService;
import fr.cnes.regards.modules.storage.service.file.handler.FilesStorageRequestEventHandler;
import io.vavr.control.Try;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.apache.commons.compress.utils.Lists;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Controller to access {@link FileReference} by rest API.
 *
 * @author Sébastien Binda
 */
@Profile({ "!downloader" })
@RestController
@RequestMapping(FileReferenceController.FILE_PATH)
public class FileReferenceController {

    public static final String FILE_PATH = FileDownloadService.FILES_PATH;

    public static final String STORE_PATH = "/store";

    public static final String EXPORT_PATH = "/csv";

    public static final String LOCATIONS_PATH = "/{storage}/locations";

    @Autowired
    private FileReferenceService fileRefService;

    @Autowired
    private IRuntimeTenantResolver tenantResolver;

    @Autowired
    private FilesStorageRequestEventHandler storageHandler;

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
