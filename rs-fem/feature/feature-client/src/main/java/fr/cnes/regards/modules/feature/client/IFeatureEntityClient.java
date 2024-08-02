package fr.cnes.regards.modules.feature.client;
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

import fr.cnes.regards.framework.feign.annotation.RestClient;
import fr.cnes.regards.modules.feature.domain.SearchFeatureSimpleEntityParameters;
import fr.cnes.regards.modules.feature.dto.FeatureEntityDto;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import jakarta.annotation.Nullable;
import java.time.OffsetDateTime;

/**
 * @author Kevin Marchois
 */
@RestClient(name = "rs-fem", contextId = "rs-fem.model-att-assoc.client")
public interface IFeatureEntityClient {

    String PATH_DATA_FEATURE_OBJECT = "/admin/features";

    @PostMapping(path = PATH_DATA_FEATURE_OBJECT, consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<PagedModel<EntityModel<FeatureEntityDto>>> findAll(
        @RequestBody SearchFeatureSimpleEntityParameters filters, @SpringQueryMap Pageable pageable);

    default ResponseEntity<PagedModel<EntityModel<FeatureEntityDto>>> findAll(String model,
                                                                              OffsetDateTime lastUpdateDateAfter,
                                                                              @Nullable
                                                                              OffsetDateTime lastUpdateDateBefore,
                                                                              int page,
                                                                              int size,
                                                                              Sort sort) {
        SearchFeatureSimpleEntityParameters filters = new SearchFeatureSimpleEntityParameters().withModel(model)
                                                                                               .withLastUpdateAfter(
                                                                                                   lastUpdateDateAfter);
        if (lastUpdateDateBefore != null) {
            filters.withLastUpdateBefore(lastUpdateDateBefore);
        }
        return findAll(filters, PageRequest.of(page, size, sort));
    }
}