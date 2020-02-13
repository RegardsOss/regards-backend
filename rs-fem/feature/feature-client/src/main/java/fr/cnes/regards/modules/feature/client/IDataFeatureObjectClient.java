package fr.cnes.regards.modules.feature.client;
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

import java.time.OffsetDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import fr.cnes.regards.framework.feign.annotation.RestClient;
import fr.cnes.regards.modules.dam.domain.entities.feature.DataObjectFeature;

/**
 * @author Sylvain Vissiere-Guerinet
 */
@RestClient(name = "rs-fem", contextId = "rs-fem.model-att-assoc.client")
@RequestMapping(IDataFeatureObjectClient.BASE_MAPPING)
public interface IDataFeatureObjectClient {

    /**
     * Client base path
     */
    String BASE_MAPPING = "/dataObjectFeature";

    @RequestMapping(method = RequestMethod.GET)
    ResponseEntity<Page<DataObjectFeature>> findAll(@RequestParam("model") String model, Pageable page,
            @RequestParam("lastUpdateDate") OffsetDateTime lastUpdateDate);

}
