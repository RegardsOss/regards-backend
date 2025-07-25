/*
 * Copyright 2017-2025 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.feature.client;

import fr.cnes.regards.framework.feign.annotation.RestClient;
import fr.cnes.regards.modules.feature.domain.SearchFeatureSimpleEntityParameters;
import fr.cnes.regards.modules.feature.dto.FeatureEntityDto;
import fr.cnes.regards.modules.feature.dto.FeatureEntityRawDto;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.time.OffsetDateTime;

/**
 * @author Thibaud Michaudel
 * Client interface for accessing feature entity data. This client doesn't retrieve dissemination information in
 * features
 */
@RestClient(name = "rs-fem", contextId = "rs-fem.feature-raw.client")
public interface IFeatureEntityRawClient {

    String PATH_DATA_FEATURE_RAW_OBJECT = "/admin/features/raw";

    @PostMapping(path = PATH_DATA_FEATURE_RAW_OBJECT, consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<PagedModel<EntityModel<FeatureEntityDto>>> findAllRaw(
        @RequestBody SearchFeatureSimpleEntityParameters filters, @SpringQueryMap Pageable pageable);

    /**
     * Return a FeatureEntityDto without dissemination information.
     * {@link FeatureEntityDto} and {@link FeatureEntityRawDto} are the same object at client level
     */
    default ResponseEntity<PagedModel<EntityModel<FeatureEntityDto>>> findAllRaw(String model,
                                                                                 @Nullable
                                                                                 OffsetDateTime lastUpdateDateAfter,
                                                                                 @Nullable
                                                                                 OffsetDateTime lastUpdateDateBefore,
                                                                                 int page,
                                                                                 int size,
                                                                                 Sort sort) {
        SearchFeatureSimpleEntityParameters filters = new SearchFeatureSimpleEntityParameters().withModel(model)
                                                                                               .withLastUpdateAfter(
                                                                                                   lastUpdateDateAfter)
                                                                                               .withLastUpdateBefore(
                                                                                                   lastUpdateDateBefore);
        return findAllRaw(filters, PageRequest.of(page, size, sort));
    }
}