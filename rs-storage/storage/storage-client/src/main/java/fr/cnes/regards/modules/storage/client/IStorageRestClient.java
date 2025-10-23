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
package fr.cnes.regards.modules.storage.client;

import feign.Response;
import fr.cnes.regards.framework.feign.annotation.RestClient;
import fr.cnes.regards.modules.fileaccess.dto.FileReferenceDto;
import fr.cnes.regards.modules.fileaccess.dto.availability.FileAvailabilityStatusDto;
import fr.cnes.regards.modules.fileaccess.dto.availability.FilesAvailabilityRequestDto;
import fr.cnes.regards.modules.filecatalog.dto.StorageLocationDto;
import jakarta.validation.Valid;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * REST Client to access storage or rs-file-catalog microservice
 * Difference with IStorageLocationRestClient is :
 * - for rs-storage microservice : no difference the both clients call rs-storage service
 * - for neotstorage microservices : this one calls rs-file-catalog, IStorageRestClient calls rs-file-access
 * <p>
 * Service to call is configured in inventory thanks to regards.feign.storage.host property.
 *
 * @author Sébastien Binda
 * Download and quota management are deprecated on storage microservice. Use rs-downloader service
 * (IStorageDownloaderRestClient)
 */
@RestClient(name = "${regards.feign.storage.host:rs-storage}", contextId = "rs-storage.rest.client")
public interface IStorageRestClient extends IStorageDownloadQuotaRestClient {

    String FILE_PATH = "/files";

    String DOWNLOAD_PATH = "/{checksum}/download";

    String STORAGES_PATH = "/storages";

    String EXPORT_PATH = "/csv";

    String LOCATIONS_PATH = "/{storage}/locations";

    String STATUS_AVAILABILITY_PATH = "/availability/status";

    /**
     * Download a file by his checksum.
     *
     * @param checksum file to download
     * @deprecated Files download is now deprecated, use the downloader service to download files
     */
    @Deprecated(since = "2.1.0")
    @RequestMapping(method = RequestMethod.GET,
                    path = FILE_PATH + DOWNLOAD_PATH,
                    produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    Response downloadFile(@PathVariable("checksum") String checksum,
                          @RequestParam(name = "isContentInline", required = false, defaultValue = "false")
                          boolean isContentInline);

    @RequestMapping(method = RequestMethod.GET, path = STORAGES_PATH, produces = MediaType.ALL_VALUE)
    ResponseEntity<List<EntityModel<StorageLocationDto>>> retrieve();

    @RequestMapping(method = RequestMethod.GET, path = FILE_PATH + EXPORT_PATH, produces = MediaType.ALL_VALUE)
    Response export();

    @RequestMapping(method = RequestMethod.POST,
                    path = FILE_PATH + LOCATIONS_PATH,
                    consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<Set<FileReferenceDto>> getFileReferencesWithoutOwners(
        @PathVariable(name = "storage") final String storage, @RequestBody final Set<String> checksums);

    @RequestMapping(method = RequestMethod.POST,
                    path = FILE_PATH + STATUS_AVAILABILITY_PATH,
                    consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<List<FileAvailabilityStatusDto>> checkFileAvailability(
        @Valid @RequestBody FilesAvailabilityRequestDto filesAvailabilityRequestDto);
}
