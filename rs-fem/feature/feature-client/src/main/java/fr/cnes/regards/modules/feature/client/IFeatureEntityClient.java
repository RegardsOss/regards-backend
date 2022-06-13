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

import fr.cnes.regards.framework.feign.annotation.RestClient;
import fr.cnes.regards.modules.feature.dto.FeatureEntityDto;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.OffsetDateTime;

/**
 * @author Kevin Marchois
 */
@RestClient(name = "rs-fem", contextId = "rs-fem.model-att-assoc.client")
public interface IFeatureEntityClient {

    String PATH_DATA_FEATURE_OBJECT = "/admin/features";

    /**
     * You better use {@link #findAll(String, OffsetDateTime, int, int, Sort)} which explicitly asks for sort
     */
    @GetMapping(path = PATH_DATA_FEATURE_OBJECT, consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    ResponseEntity<PagedModel<EntityModel<FeatureEntityDto>>> findAll(@SpringQueryMap Pageable pageable,
                                                                      @RequestParam("model") String model,
                                                                      @RequestParam("from")
                                                                      OffsetDateTime lastUpdateDate);

    default ResponseEntity<PagedModel<EntityModel<FeatureEntityDto>>> findAll(String model,
                                                                              OffsetDateTime lastUpdateDate,
                                                                              int page,
                                                                              int size,
                                                                              Sort sort) {
        return findAll(PageRequest.of(page, size, sort), model, lastUpdateDate);
    }
}
