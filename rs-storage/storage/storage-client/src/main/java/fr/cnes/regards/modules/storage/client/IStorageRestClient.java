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
package fr.cnes.regards.modules.storage.client;

import java.util.List;

import org.springframework.hateoas.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import feign.Response;
import fr.cnes.regards.framework.feign.annotation.RestClient;
import fr.cnes.regards.modules.storage.domain.dto.StorageLocationDTO;

/**
 * REST Client to to access storage microservice
 * @author Sébastien Binda
 */
@RestClient(name = "rs-storage", contextId = "rs-storage.rest.client")
public interface IStorageRestClient {

    public static final String FILE_PATH = "/files";

    public static final String DOWNLOAD_PATH = "/{checksum}/download";

    public static final String STORAGES_PATH = "/storages";

    public static final String EXPORT_PATH = "/csv";

    /**
     * Download a file by his checksum.
     * @param checksum file to download
     * @return
     */
    @RequestMapping(method = RequestMethod.GET, path = FILE_PATH + DOWNLOAD_PATH,
            produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    Response downloadFile(@PathVariable("checksum") String checksum);

    @RequestMapping(method = RequestMethod.GET, path = STORAGES_PATH, produces = MediaType.ALL_VALUE)
    ResponseEntity<List<Resource<StorageLocationDTO>>> retrieve();

    @RequestMapping(method = RequestMethod.GET, path = FILE_PATH + EXPORT_PATH, produces = MediaType.ALL_VALUE)
    Response export();

}
