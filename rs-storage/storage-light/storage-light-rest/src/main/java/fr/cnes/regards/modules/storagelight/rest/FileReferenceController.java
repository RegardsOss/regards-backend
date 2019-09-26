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
package fr.cnes.regards.modules.storagelight.rest;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MimeType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.storagelight.domain.DownloadableFile;
import fr.cnes.regards.modules.storagelight.domain.database.FileReference;
import fr.cnes.regards.modules.storagelight.service.file.FileDownloadService;

/**
 * Controller to access {@link FileReference} by rest API.
 * @author Sébastien Binda
 */
@RestController
@RequestMapping(FileReferenceController.FILE_PATH)
public class FileReferenceController {

    public static final String FILE_PATH = "/files";

    public static final String DOWNLOAD_PATH = "/{checksum}/download";

    @Autowired
    private FileDownloadService downloadService;

    /**
     * End-point to Download a file referenced by a storage location with the given checksum.
     * @param checksum checksum of the file to download
     * @return {@link InputStreamResource}
     * @throws ModuleException
     * @throws IOException
     */
    @RequestMapping(path = DOWNLOAD_PATH, method = RequestMethod.GET, produces = MediaType.ALL_VALUE)
    @ResourceAccess(description = "Download one file by checksum.", role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<InputStreamResource> downloadFile(@PathVariable("checksum") String checksum)
            throws ModuleException, IOException {
        DownloadableFile downloadFile = downloadService.downloadFile(checksum);
        InputStreamResource isr = new InputStreamResource(downloadFile.getFileInputStream());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentLength(downloadFile.getRealFileSize());
        headers.setContentType(asMediaType(downloadFile.getMimeType()));
        headers.setContentDispositionFormData("attachement;filename=", downloadFile.getFileName());
        return new ResponseEntity<>(isr, headers, HttpStatus.OK);
    }

    private static MediaType asMediaType(MimeType mimeType) {
        if (mimeType instanceof MediaType) {
            return (MediaType) mimeType;
        }
        return new MediaType(mimeType.getType(), mimeType.getSubtype(), mimeType.getParameters());
    }
}
