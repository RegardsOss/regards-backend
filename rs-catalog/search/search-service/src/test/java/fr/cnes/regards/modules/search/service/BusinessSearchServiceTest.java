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
package fr.cnes.regards.modules.search.service;

import fr.cnes.regards.modules.dam.domain.entities.Dataset;
import fr.cnes.regards.modules.dam.domain.entities.feature.DatasetFeature;
import fr.cnes.regards.modules.dam.domain.entities.feature.EntityFeature;
import fr.cnes.regards.modules.indexer.dao.FacetPage;
import fr.cnes.regards.modules.indexer.domain.IIndexable;
import fr.cnes.regards.modules.indexer.domain.criterion.ICriterion;
import fr.cnes.regards.modules.opensearch.service.exception.OpenSearchUnknownParameter;
import fr.cnes.regards.modules.search.domain.plugin.SearchType;
import fr.cnes.regards.modules.search.service.accessright.AccessRightFilterException;
import fr.cnes.regards.modules.search.service.accessright.DataAccessRightService;
import fr.cnes.regards.modules.search.service.utils.SampleDataUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;

import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * Unit test for {@link BusinessSearchService}.
 *
 * @author Théo Lasserre
 */
@RunWith(MockitoJUnitRunner.class)
public class BusinessSearchServiceTest {

    /**
     * Catalog search service (entity level search service)
     */
    @Mock
    protected ICatalogSearchService catalogSearchService;

    @Mock
    private DataAccessRightService dataAccessRightService;

    /**
     * Class under test
     */
    @InjectMocks
    private BusinessSearchService businessSearchService;

    /**
     * Test searching datasets
     */
    @Test
    public void doSearchShouldReturnDatasetsWithAccessGrantedSet()
        throws SearchException, OpenSearchUnknownParameter, AccessRightFilterException, ExecutionException {
        // Prepare test
        SearchType searchType = SearchType.DATASETS;
        PagedResourcesAssembler<Dataset> assembler = SampleDataUtils.ASSEMBLER_DATASET;
        Pageable pageable = SampleDataUtils.PAGEABLE;

        Mockito.when(dataAccessRightService.checkContentAccess(Mockito.any())).thenReturn(AccessStatus.GRANTED);

        // Define expected values
        ICriterion expectedCriterion = SampleDataUtils.SIMPLE_STRING_MATCH_CRITERION;
        FacetPage<Dataset> facetPageDataset = SampleDataUtils.FACET_PAGE_DATASET;
        // thanks to mockito not properly handling dynamic typing, we have to do this trick
        FacetPage<IIndexable> expectedSearchResult = new FacetPage<>(facetPageDataset.getContent()
                                                                                     .stream()
                                                                                     .map(data -> (IIndexable) data)
                                                                                     .collect(Collectors.toList()),
                                                                     facetPageDataset.getFacets(),
                                                                     pageable,
                                                                     100);

        // Mock dependencies
        Mockito.when(catalogSearchService.search(Mockito.any(ICriterion.class),
                                                 Mockito.any(SearchType.class),
                                                 Mockito.any(),
                                                 Mockito.any(Pageable.class))).thenReturn(expectedSearchResult);

        // Perform the test
        FacetPage<EntityFeature> facetPage = businessSearchService.search(expectedCriterion,
                                                                          searchType,
                                                                          SampleDataUtils.QUERY_FACETS,
                                                                          pageable);

        // Verify that datasets have access granted set
        for (EntityFeature entityFeature : facetPage.getContent()) {
            DatasetFeature datasetFeature = (DatasetFeature) entityFeature;
            Assert.assertTrue("Dataset must have access granted to true",
                              datasetFeature.getDataObjectsFilesAccessGranted());
        }
    }

    /**
     * Test searching datasets
     */
    @Test
    public void doSearchShouldReturnDatasetsWithNoAccessGrantedSet()
        throws SearchException, OpenSearchUnknownParameter, AccessRightFilterException, ExecutionException {
        // Prepare test
        SearchType searchType = SearchType.DATASETS;
        PagedResourcesAssembler<Dataset> assembler = SampleDataUtils.ASSEMBLER_DATASET;
        Pageable pageable = SampleDataUtils.PAGEABLE;

        Mockito.when(dataAccessRightService.checkContentAccess(Mockito.any())).thenReturn(AccessStatus.FORBIDDEN);

        // Define expected values
        ICriterion expectedCriterion = SampleDataUtils.SIMPLE_STRING_MATCH_CRITERION;
        FacetPage<Dataset> facetPageDataset = SampleDataUtils.FACET_PAGE_DATASET;
        // thanks to mockito not properly handling dynamic typing, we have to do this trick
        FacetPage<IIndexable> expectedSearchResult = new FacetPage<>(facetPageDataset.getContent()
                                                                                     .stream()
                                                                                     .map(data -> (IIndexable) data)
                                                                                     .collect(Collectors.toList()),
                                                                     facetPageDataset.getFacets(),
                                                                     pageable,
                                                                     100);

        // Mock dependencies
        Mockito.when(catalogSearchService.search(Mockito.any(ICriterion.class),
                                                 Mockito.any(SearchType.class),
                                                 Mockito.any(),
                                                 Mockito.any(Pageable.class))).thenReturn(expectedSearchResult);

        // Perform the test
        FacetPage<EntityFeature> facetPage = businessSearchService.search(expectedCriterion,
                                                                          searchType,
                                                                          SampleDataUtils.QUERY_FACETS,
                                                                          pageable);

        // Verify that datasets have access granted set
        for (EntityFeature entityFeature : facetPage.getContent()) {
            DatasetFeature datasetFeature = (DatasetFeature) entityFeature;
            Assert.assertFalse("Dataset must not have access granted to true",
                               datasetFeature.getDataObjectsFilesAccessGranted());
        }
    }
}
