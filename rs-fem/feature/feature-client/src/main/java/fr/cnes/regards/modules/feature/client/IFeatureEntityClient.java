package fr.cnes.regards.modules.feature.client;
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

import java.time.OffsetDateTime;

import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import fr.cnes.regards.framework.feign.annotation.RestClient;
import fr.cnes.regards.modules.feature.dto.FeatureEntityDto;

/**
 * @author Kevin Marchois
 */
@RestClient(name = "rs-fem", contextId = "rs-fem.model-att-assoc.client")
@RequestMapping(value = IFeatureEntityClient.PATH_DATA_FEATURE_OBJECT, consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
public interface IFeatureEntityClient {

    static final String PATH_DATA_FEATURE_OBJECT = "/admin/features";

    @RequestMapping(method = RequestMethod.GET)
    @ResponseBody
    ResponseEntity<PagedModel<EntityModel<FeatureEntityDto>>> findAll(@RequestParam("model") String model,
            @RequestParam("from") OffsetDateTime lastUpdateDate, @RequestParam("page") int page,
            @RequestParam("size") int size);
}
