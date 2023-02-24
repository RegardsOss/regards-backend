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
package fr.cnes.regards.modules.toponyms.client;

import fr.cnes.regards.framework.feign.annotation.RestClient;
import fr.cnes.regards.modules.toponyms.domain.ToponymDTO;
import fr.cnes.regards.modules.toponyms.domain.ToponymGeoJson;
import fr.cnes.regards.modules.toponyms.domain.ToponymsRestConfiguration;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Client to search for Toponyms.
 *
 * @author Sébastien Binda
 */
@RestClient(name = "rs-access-instance", contextId = "rs-access-project.toponyms-client")
public interface IToponymsClient {

    @GetMapping(value = ToponymsRestConfiguration.ROOT_MAPPING + ToponymsRestConfiguration.SEARCH,
                consumes = MediaType.APPLICATION_JSON_VALUE,
                produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<List<EntityModel<ToponymDTO>>> search(@RequestParam(name = "partialLabel") String partialLabel,
                                                         @RequestParam(name = "locale") String locale);

    @GetMapping(value = ToponymsRestConfiguration.ROOT_MAPPING,
                consumes = MediaType.APPLICATION_JSON_VALUE,
                produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<PagedModel<EntityModel<ToponymDTO>>> find(@RequestParam(name = "page") int page,
                                                             @RequestParam(name = "size") int size);

    @GetMapping(value = ToponymsRestConfiguration.ROOT_MAPPING + ToponymsRestConfiguration.TOPONYM_ID,
                consumes = MediaType.APPLICATION_JSON_VALUE,
                produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<EntityModel<ToponymDTO>> get(@PathVariable("businessId") String businessId,
                                                @RequestParam(name = "simplified", required = false)
                                                Boolean simplified);

    @GetMapping(value = ToponymsRestConfiguration.ROOT_MAPPING + ToponymsRestConfiguration.TOPONYM_ID,
                consumes = MediaType.APPLICATION_JSON_VALUE,
                produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<EntityModel<ToponymDTO>> get(@PathVariable("businessId") String businessId);

    @PostMapping(value = ToponymsRestConfiguration.ROOT_MAPPING,
                 consumes = MediaType.APPLICATION_JSON_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<EntityModel<ToponymDTO>> createNotVisibleToponym(@RequestBody ToponymGeoJson toponymGeoJson);

}
