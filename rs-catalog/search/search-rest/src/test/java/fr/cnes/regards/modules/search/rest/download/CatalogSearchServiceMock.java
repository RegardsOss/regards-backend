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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.framework.urn.UniformResourceName;
import fr.cnes.regards.modules.dam.domain.entities.AbstractEntity;
import fr.cnes.regards.modules.dam.domain.entities.DataObject;
import fr.cnes.regards.modules.dam.domain.entities.metadata.DataObjectMetadata;
import fr.cnes.regards.modules.indexer.domain.DataFile;
import fr.cnes.regards.modules.indexer.service.ISearchService;
import fr.cnes.regards.modules.search.rest.FakeFileFactory;
import fr.cnes.regards.modules.search.rest.FakeProductFactory;
import fr.cnes.regards.modules.search.service.CatalogSearchService;
import fr.cnes.regards.modules.search.service.ICatalogSearchService;
import fr.cnes.regards.modules.search.service.IFacetConverter;
import fr.cnes.regards.modules.search.service.IPageableConverter;
import fr.cnes.regards.modules.search.service.accessright.IAccessRightFilter;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

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

    private final FakeFileFactory files;

    private final FakeProductFactory products;

    private boolean mockGet;

    public CatalogSearchServiceMock(ISearchService searchService,
                                    IAccessRightFilter accessRightFilter,
                                    IFacetConverter facetConverter,
                                    IPageableConverter pageableConverter) {
        super(searchService, accessRightFilter, facetConverter, pageableConverter);
        products = new FakeProductFactory();
        files = new FakeFileFactory();
        accessVerification = mockAccessVerification();
        // Enables the activation of mock for searchService.get
        // depending on context.
        mockGet = false;
    }

    public void mockGet() {
        mockGet = true;
    }

    private ICatalogSearchService mockAccessVerification() {
        try {
            DataObject fakeProduct = fakeProduct();
            ICatalogSearchService searchService = mock(ICatalogSearchService.class);
            when(searchService.get(products.unknownProduct())).thenThrow(new EntityNotFoundException("product not found"));
            when(searchService.get(products.unauthorizedProduct())).thenThrow(new EntityOperationForbiddenException(
                "product unauthorized"));

            when(searchService.get(products.validProduct())).thenReturn(fakeProduct);
            return searchService;
        } catch (EntityNotFoundException | EntityOperationForbiddenException e) {
            throw new AssertionError("problems while mocking product access verification");
        }
    }

    private DataObject fakeProduct() {
        DataObject entity = mock(DataObject.class);
        when(entity.getFiles()).thenReturn(oneFileOfEachType());
        when(entity.getMetadata()).thenReturn(new DataObjectMetadata());
        return entity;
    }

    private Multimap<DataType, DataFile> oneFileOfEachType() {
        Multimap<DataType, DataFile> multimap = ArrayListMultimap.create();
        files.allDataFiles()
             .stream()
             .collect(Collectors.toMap(DataFile::getDataType, file -> file))
             .forEach(multimap::put);
        return multimap;
    }

    @Override
    public <E extends AbstractEntity<?>> E get(UniformResourceName urn)
        throws EntityOperationForbiddenException, EntityNotFoundException {
        if (mockGet) {
            return accessVerification.get(urn);
        }
        return super.get(urn);
    }
}
