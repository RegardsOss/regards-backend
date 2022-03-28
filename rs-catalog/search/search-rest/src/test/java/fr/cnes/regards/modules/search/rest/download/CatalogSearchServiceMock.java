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
package fr.cnes.regards.modules.search.rest.download;

import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.urn.UniformResourceName;
import fr.cnes.regards.modules.indexer.service.ISearchService;
import fr.cnes.regards.modules.opensearch.service.IOpenSearchService;
import fr.cnes.regards.modules.search.rest.FakeProductFactory;
import fr.cnes.regards.modules.search.service.CatalogSearchService;
import fr.cnes.regards.modules.search.service.ICatalogSearchService;
import fr.cnes.regards.modules.search.service.IFacetConverter;
import fr.cnes.regards.modules.search.service.IPageableConverter;
import fr.cnes.regards.modules.search.service.accessright.IAccessRightFilter;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Mock simulate access rights on catalog
 *
 * @author Sébastien Binda
 */
@Service
@Primary
public class CatalogSearchServiceMock extends CatalogSearchService {

    private final ICatalogSearchService accessVerification;

    public CatalogSearchServiceMock(ISearchService searchService,
                                    IOpenSearchService openSearchService,
                                    IAccessRightFilter accessRightFilter,
                                    IFacetConverter facetConverter,
                                    IPageableConverter pageableConverter) {
        super(searchService, openSearchService, accessRightFilter, facetConverter, pageableConverter);
        accessVerification = mockAccessVerification();

    }

    private ICatalogSearchService mockAccessVerification() {
        try {
            FakeProductFactory products = new FakeProductFactory();
            ICatalogSearchService searchService = mock(ICatalogSearchService.class);
            when(searchService.hasAccess(products.unknownProduct())).thenThrow(new EntityNotFoundException("not found"));
            when(searchService.hasAccess(products.unauthorizedProduct())).thenReturn(false);
            when(searchService.hasAccess(products.validProduct())).thenReturn(true);
            return searchService;
        } catch (EntityNotFoundException e) {
            throw new RuntimeException("problems while mocking product access verification", e);
        }
    }

    @Override
    public boolean hasAccess(UniformResourceName urn) throws EntityNotFoundException {
        return accessVerification.hasAccess(urn);
    }

}
