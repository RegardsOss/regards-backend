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

import fr.cnes.regards.framework.feign.annotation.RestClient;
import fr.cnes.regards.modules.filecatalog.dto.StorageLocationDto;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.List;

/**
 * REST Client to access storage microservice or file-access microservice.
 * Difference with IStorageRestClient is :
 * - for rs-storage microservice : no difference the both clients call rs-storage service
 * - for neotstorage microservices : this one calls rs-file-access, IStorageRestClient calls rs-file-catalog
 * <p>
 * Service to call is configured in inventory thanks to regards.feign.storage.host property.
 *
 * @author Sébastien Binda
 * Download and quota management are deprecated on storage microservice. Use rs-downloader service
 * (IStorageDownloaderRestClient)
 */
@RestClient(name = "${regards.feign.storage.location.host:rs-storage}", contextId = "rs-storage.location.rest.client")
public interface IStorageLocationRestClient {

    String STORAGES_PATH = "/storages";

    @RequestMapping(method = RequestMethod.GET, path = STORAGES_PATH, produces = MediaType.ALL_VALUE)
    ResponseEntity<List<EntityModel<StorageLocationDto>>> retrieve();

}
