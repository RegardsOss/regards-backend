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
package fr.cnes.regards.modules.storage.client;

import feign.Response;
import fr.cnes.regards.framework.feign.annotation.RestClient;
import fr.cnes.regards.modules.storage.domain.dto.FileReferenceDTO;
import fr.cnes.regards.modules.storage.domain.dto.StorageLocationDTO;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * REST Client to access storage microservice
 *
 * @author Sébastien Binda
 */
@RestClient(name = "rs-storage", contextId = "rs-storage.rest.client")
public interface IStorageRestClient extends IStorageDownloadQuotaRestClient {

    String FILE_PATH = "/files";

    String DOWNLOAD_PATH = "/{checksum}/download";

    String STORAGES_PATH = "/storages";

    String EXPORT_PATH = "/csv";

    String LOCATIONS_PATH = "/{storage}/locations";

    /**
     * Download a file by his checksum.
     *
     * @param checksum file to download
     */
    @RequestMapping(method = RequestMethod.GET, path = FILE_PATH + DOWNLOAD_PATH,
        produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    Response downloadFile(@PathVariable("checksum") String checksum,
                          @RequestParam(name = "isContentInline", required = false) Boolean isContentInline);

    @RequestMapping(method = RequestMethod.GET, path = STORAGES_PATH, produces = MediaType.ALL_VALUE)
    ResponseEntity<List<EntityModel<StorageLocationDTO>>> retrieve();

    @RequestMapping(method = RequestMethod.GET, path = FILE_PATH + EXPORT_PATH, produces = MediaType.ALL_VALUE)
    Response export();

    @RequestMapping(method = RequestMethod.POST, path = FILE_PATH + LOCATIONS_PATH,
        consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<Set<FileReferenceDTO>> getFileReferencesWithoutOwners(
        @PathVariable(name = "storage") final String storage, @RequestBody final Set<String> checksums);
}
