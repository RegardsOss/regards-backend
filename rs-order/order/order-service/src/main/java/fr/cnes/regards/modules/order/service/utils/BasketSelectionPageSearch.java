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
package fr.cnes.regards.modules.order.service.utils;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.dam.domain.entities.feature.EntityFeature;
import fr.cnes.regards.modules.order.domain.basket.BasketDatasetSelection;
import fr.cnes.regards.modules.order.exception.CatalogSearchException;
import fr.cnes.regards.modules.order.exception.CatalogSearchRuntimeException;
import fr.cnes.regards.modules.order.service.BasketService;
import fr.cnes.regards.modules.search.client.IComplexSearchClient;
import fr.cnes.regards.modules.search.domain.plugin.legacy.FacettedPagedModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * This class is a helper to query ES in OrderService and OrderProcessingService.
 * Its logic used to reside entirely within OrderService, but has been taken out to be
 * used also in case of dataset selections with processsing.
 *
 * @author Guillaume Andrieu
 */
@Component
public class BasketSelectionPageSearch {

    private final Integer dataObjectsPageSize;

    private final IComplexSearchClient searchClient;

    public BasketSelectionPageSearch(@Value("${regards.order.batch.size:10000}") Integer dataObjectsPageSize,
                                     IComplexSearchClient searchClient) {
        this.dataObjectsPageSize = dataObjectsPageSize;
        this.searchClient = searchClient;
    }

    public List<EntityFeature> searchDataObjects(BasketDatasetSelection dsSel, int page) throws CatalogSearchException {
        try {
            ResponseEntity<FacettedPagedModel<EntityModel<EntityFeature>>> pagedResourcesResponseEntity = searchClient.searchDataObjects(
                BasketService.buildSearchRequest(dsSel, page, dataObjectsPageSize));
            // It is mandatory to check NOW, at creation instant of order from basket, if data object files are still downloadable
            FacettedPagedModel<EntityModel<EntityFeature>> response = pagedResourcesResponseEntity.getBody();
            if (response != null) {
                Collection<EntityModel<EntityFeature>> objects = response.getContent();
                // If a lot of objects, parallelization is very useful, if not we don't really care
                return objects.parallelStream()
                              .filter(entity -> entity.getContent() != null)
                              .map(EntityModel::getContent)
                              .toList();
            } else {
                throw new CatalogSearchException(
                    "Error trying to search data objects from catalog. Getting null response from catalog service.");
            }
        } catch (Exception e) {
            throw new CatalogSearchException("Error trying to search data objects from catalog.", e);
        }
    }

    public Iterable<List<EntityFeature>> pagedSearchDataObjects(BasketDatasetSelection dsSel) {
        return () -> new Iterator<>() {

            int page = -1;

            boolean lastSearchYieldedEmpty = false;

            @Override
            public boolean hasNext() {
                return (page < 0) || !lastSearchYieldedEmpty;
            }

            @Override
            public List<EntityFeature> next() {
                ++page;
                if (lastSearchYieldedEmpty) {
                    throw new NoSuchElementException();
                }
                try {
                    List<EntityFeature> entityFeatures = searchDataObjects(dsSel, page);
                    lastSearchYieldedEmpty = entityFeatures.isEmpty();
                    return entityFeatures;
                } catch (ModuleException e) {
                    throw new CatalogSearchRuntimeException(e);
                }
            }
        };
    }

    public Flux<EntityFeature> fluxSearchDataObjects(BasketDatasetSelection dsSel) {
        return Flux.fromIterable(pagedSearchDataObjects(dsSel)).flatMapIterable(l -> l);
    }

}
