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
package fr.cnes.regards.modules.order.service.utils;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import fr.cnes.regards.modules.dam.domain.entities.feature.EntityFeature;
import fr.cnes.regards.modules.order.domain.basket.BasketDatasetSelection;
import fr.cnes.regards.modules.order.service.BasketService;
import fr.cnes.regards.modules.search.client.IComplexSearchClient;
import fr.cnes.regards.modules.search.domain.plugin.legacy.FacettedPagedModel;
import reactor.core.publisher.Flux;

/**
 * TODO : Class description
 *
 * @author Guillaume Andrieu
 *
 */
@Component
public class BasketSelectionPageSearch {

    public static final int MAX_PAGE_SIZE = 10_000;

    @Autowired
    private IComplexSearchClient searchClient;

    public List<EntityFeature> searchDataObjects(BasketDatasetSelection dsSel, int page) {
        ResponseEntity<FacettedPagedModel<EntityModel<EntityFeature>>> pagedResourcesResponseEntity = searchClient
                .searchDataObjects(BasketService.buildSearchRequest(dsSel, page, MAX_PAGE_SIZE));
        // It is mandatory to check NOW, at creation instant of order from basket, if data object files are still downloadable
        Collection<EntityModel<EntityFeature>> objects = pagedResourcesResponseEntity.getBody().getContent();
        // If a lot of objects, parallelisation is very useful, if not we don't really care
        return objects.parallelStream().map(EntityModel::getContent).collect(Collectors.toList());
    }

    public Iterable<List<EntityFeature>> pagedSearchDataObjects(BasketDatasetSelection dsSel) {
        return () -> new Iterator<List<EntityFeature>>() {

            int page = -1;

            boolean lastSearchYieldedEmpty = false;

            @Override
            public boolean hasNext() {
                return (page < 0) || !lastSearchYieldedEmpty;
            }

            @Override
            public List<EntityFeature> next() {
                List<EntityFeature> entityFeatures = searchDataObjects(dsSel, ++page);
                lastSearchYieldedEmpty = entityFeatures.isEmpty();
                return entityFeatures;
            }
        };
    }

    public Flux<EntityFeature> fluxSearchDataObjects(BasketDatasetSelection dsSel) {
        return Flux.fromIterable(pagedSearchDataObjects(dsSel)).flatMapIterable(l -> l);
    }

}
