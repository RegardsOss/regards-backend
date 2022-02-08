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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import com.google.common.collect.Sets;
import fr.cnes.regards.modules.dam.domain.entities.AbstractEntity;
import fr.cnes.regards.modules.dam.domain.entities.Collection;
import fr.cnes.regards.modules.dam.domain.entities.feature.CollectionFeature;
import fr.cnes.regards.modules.indexer.domain.aggregation.QueryableAttribute;
import fr.cnes.regards.modules.model.domain.Model;
import fr.cnes.regards.modules.search.domain.plugin.CollectionWithStats;
import fr.cnes.regards.modules.search.domain.plugin.SearchType;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.metrics.stats.ParsedStats;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.framework.urn.UniformResourceName;
import fr.cnes.regards.modules.dam.domain.entities.DataObject;
import fr.cnes.regards.modules.indexer.dao.FacetPage;
import fr.cnes.regards.modules.indexer.domain.DataFile;
import fr.cnes.regards.modules.indexer.domain.IIndexable;
import fr.cnes.regards.modules.indexer.domain.SimpleSearchKey;
import fr.cnes.regards.modules.indexer.domain.criterion.ICriterion;
import fr.cnes.regards.modules.indexer.domain.facet.FacetType;
import fr.cnes.regards.modules.indexer.service.ISearchService;
import fr.cnes.regards.modules.indexer.service.Searches;
import fr.cnes.regards.modules.opensearch.service.OpenSearchService;
import fr.cnes.regards.modules.opensearch.service.exception.OpenSearchParseException;
import fr.cnes.regards.modules.opensearch.service.exception.OpenSearchUnknownParameter;
import fr.cnes.regards.modules.search.service.accessright.AccessRightFilterException;
import fr.cnes.regards.modules.search.service.accessright.IAccessRightFilter;
import fr.cnes.regards.modules.search.service.utils.SampleDataUtils;

/**
 * Unit test for {@link CatalogSearchService}.
 * @author Xavier-Alexandre Brochard
 */
public class CatalogSearchServiceTest {

    /**
     * Class under test
     */
    private CatalogSearchService catalogSearchService;

    /**
     * The OpenSearch service building {@link ICriterion} from a request string
     */
    private OpenSearchService openSearchService;

    /**
     * Facet converter
     */
    private IFacetConverter facetConverter;

    /**
     * Facet converter
     */
    private IPageableConverter pageableConverter;

    /**
     * Adds user group and data access filters
     */
    private IAccessRightFilter accessRightFilter;

    /**
     * Service perfoming the ElasticSearch search
     */
    private ISearchService searchService;

    /**
     * Get current tenant at runtime and allows tenant forcing
     */
    private IRuntimeTenantResolver runtimeTenantResolver;

    /**
     * The resource service
     */
    private IResourceService resourceService;

    @Before
    public void setUp() throws AccessRightFilterException, OpenSearchUnknownParameter {
        // Declare mocks
        openSearchService = Mockito.mock(OpenSearchService.class);
        accessRightFilter = Mockito.mock(IAccessRightFilter.class);
        searchService = Mockito.mock(ISearchService.class);
        runtimeTenantResolver = Mockito.mock(IRuntimeTenantResolver.class);
        resourceService = Mockito.mock(IResourceService.class);
        facetConverter = Mockito.mock(IFacetConverter.class);
        pageableConverter = Mockito.mock(IPageableConverter.class);

        Mockito.when(facetConverter.convert(SampleDataUtils.QUERY_FACETS, new HashMap<>()))
                .thenReturn(SampleDataUtils.FACETS);
        Mockito.when(pageableConverter.convert(SampleDataUtils.PAGEABLE)).thenReturn(SampleDataUtils.PAGEABLE);

        // Globally mock what's mockable yet
        Mockito.when(accessRightFilter.addAccessRights(Mockito.any()))
                .thenAnswer(invocation -> invocation.getArguments()[0]);
        Mockito.when(runtimeTenantResolver.getTenant()).thenReturn(SampleDataUtils.TENANT);
        Mockito.when(resourceService.toResource(Mockito.any()))
                .thenAnswer(invocation -> EntityModel.of(invocation.getArguments()[0]));

        // Instanciate the tested class
        catalogSearchService = new CatalogSearchService(searchService, openSearchService, accessRightFilter,
                facetConverter, pageableConverter);
    }

    /**
     * Test the main search method
     * @throws OpenSearchUnknownParameter
     */
    @SuppressWarnings({ "unchecked", "deprecation" })
    @Test
    @Requirement("REGARDS_DSL_DAM_ARC_810")
    public void doSearchShouldPerformASimpleSearch()
            throws SearchException, OpenSearchParseException, UnsupportedEncodingException, OpenSearchUnknownParameter {
        // Prepare test
        SimpleSearchKey<DataObject> searchKey = Searches.onSingleEntity(EntityType.DATA);
        searchKey.setSearchIndex(SampleDataUtils.TENANT);
        MultiValueMap<String, String> q = new LinkedMultiValueMap<>();
        q.add("q=", URLEncoder.encode(SampleDataUtils.QUERY, "UTF-8"));

        PagedResourcesAssembler<DataObject> assembler = SampleDataUtils.ASSEMBLER_DATAOBJECT;
        Pageable pageable = SampleDataUtils.PAGEABLE;

        // Define expected values
        ICriterion expectedCriterion = SampleDataUtils.SIMPLE_STRING_MATCH_CRITERION;
        FacetPage<DataObject> facetPageDataobject = SampleDataUtils.FACET_PAGE_DATAOBJECT;
        // thanks to mockito not properly handling dynamic typing, we have to do this trick
        FacetPage<IIndexable> expectedSearchResult = new FacetPage<>(
                facetPageDataobject.getContent().stream().map(data -> (IIndexable) data).collect(Collectors.toList()),
                facetPageDataobject.getFacets());

        // Mock dependencies
        Mockito.when(searchService.search(Mockito.any(SimpleSearchKey.class), Mockito.any(Pageable.class),
                                          Mockito.any(ICriterion.class), Mockito.any()))
                .thenReturn(expectedSearchResult);
        PagedModel<EntityModel<DataObject>> pageResources = SampleDataUtils.PAGED_RESOURCES_DATAOBJECT;
        Mockito.when(assembler.toModel(Mockito.any())).thenReturn(pageResources);

        // Perform the test
        catalogSearchService.search(expectedCriterion, searchKey, SampleDataUtils.QUERY_FACETS, pageable);

        // Check
        Mockito.verify(searchService).search(searchKey, pageable, expectedCriterion, SampleDataUtils.FACETS);
    }

    /**
     * Le système doit permettre de désactiver la gestion des facettes pour des questions de performance.
     * @throws OpenSearchUnknownParameter
     */
    @SuppressWarnings({ "unchecked", "deprecation" })
    @Test
    @Purpose("Le système doit permettre de désactiver la gestion des facettes pour des questions de performance.")
    @Requirement("REGARDS_DSL_DAM_CAT_620")
    public void doSearchWithNoFacet() throws SearchException, OpenSearchParseException, OpenSearchUnknownParameter {
        // Prepare test
        SimpleSearchKey<DataObject> searchKey = Searches.onSingleEntity(EntityType.DATA);
        searchKey.setSearchIndex(SampleDataUtils.TENANT);
        MultiValueMap<String, String> q = new LinkedMultiValueMap<>();
        q.add("q=", "whatever");

        Map<String, FacetType> facets = new HashMap<>();
        PagedResourcesAssembler<DataObject> assembler = SampleDataUtils.ASSEMBLER_DATAOBJECT;
        Pageable pageable = SampleDataUtils.PAGEABLE;

        // Define expected values
        ICriterion expectedCriterion = SampleDataUtils.SIMPLE_STRING_MATCH_CRITERION;
        FacetPage<DataObject> facetPageDataobject = SampleDataUtils.FACET_PAGE_DATAOBJECT;
        // thanks to mockito not properly handling dynamic typing, we have to do this trick
        FacetPage<IIndexable> expectedSearchResult = new FacetPage<>(
                facetPageDataobject.getContent().stream().map(data -> (IIndexable) data).collect(Collectors.toList()),
                facetPageDataobject.getFacets());

        // Mock dependencies
        Mockito.when(searchService.search(Mockito.any(SimpleSearchKey.class), Mockito.any(Pageable.class),
                                          Mockito.any(ICriterion.class), Mockito.any()))
                .thenReturn(expectedSearchResult);
        PagedModel<EntityModel<DataObject>> pageResources = SampleDataUtils.PAGED_RESOURCES_DATAOBJECT;
        Mockito.when(assembler.toModel(Mockito.any())).thenReturn(pageResources);

        // Perform the test
        catalogSearchService.search(expectedCriterion, searchKey, null, pageable);

        // Check
        Mockito.verify(searchService).search(searchKey, pageable, expectedCriterion, facets);
    }

    /**
     * Test of the method hasAccess
     * @throws EntityNotFoundException
     * @throws EntityOperationForbiddenException
     * @throws OpenSearchUnknownParameter
     */
    @Test
    public void testHasAccess() throws EntityOperationForbiddenException, EntityNotFoundException {
        DataObject toReturn = Mockito.mock(DataObject.class);
        Multimap<DataType, DataFile> multiMap = ArrayListMultimap.create();
        ;
        multiMap.put(DataType.RAWDATA, new DataFile());
        UniformResourceName urn = Mockito.mock(UniformResourceName.class);
        CatalogSearchService mock = Mockito.spy(catalogSearchService);
        Mockito.when(toReturn.getFiles()).thenReturn(multiMap);
        Mockito.doReturn(toReturn).when(mock).get((Mockito.any(UniformResourceName.class)));
        assertTrue(mock.hasAccess(urn));
    }

    /**
     * Test of the method hasAccess the multimap will contain an other value than RAWDATA so we will
     * expect false result for hasAccess
     * @throws EntityNotFoundException
     * @throws EntityOperationForbiddenException
     * @throws OpenSearchUnknownParameter
     */
    @Test
    public void testHasAccessForbidden() throws EntityOperationForbiddenException, EntityNotFoundException {
        UniformResourceName urn = Mockito.mock(UniformResourceName.class);
        CatalogSearchService mock = Mockito.spy(catalogSearchService);
        Mockito.doThrow(EntityOperationForbiddenException.class).when(mock)
                .get((Mockito.any(UniformResourceName.class)));
        assertFalse(mock.hasAccess(urn));
    }

    /**
     * Test of the method hasAccess with the throwing of a EntityNotFoundException
     * @throws EntityNotFoundException
     * @throws EntityOperationForbiddenException
     * @throws OpenSearchUnknownParameter
     */
    @Test(expected = EntityNotFoundException.class)
    public void testHasAccessNotFound() throws EntityOperationForbiddenException, EntityNotFoundException {
        UniformResourceName urn = Mockito.mock(UniformResourceName.class);
        CatalogSearchService mock = Mockito.spy(catalogSearchService);
        Mockito.doThrow(new EntityNotFoundException("")).when(mock).get((Mockito.any(UniformResourceName.class)));
        assertFalse(mock.hasAccess(urn));
    }

    @Test
    public void testGetDocumentWithStats() throws SearchException, EntityNotFoundException,
            EntityOperationForbiddenException, AccessRightFilterException {
        UniformResourceName urn = new UniformResourceName();
        urn.setEntityType(EntityType.COLLECTION);
        urn.setIdentifier("AIP");
        urn.setTenant("perf");
        urn.setEntityId(UUID.fromString("80282ac5-1b01-4e9d-a356-123456789012"));
        SearchType searchType = Mockito.mock(SearchType.class);
        QueryableAttribute creationDateAttribute = new QueryableAttribute("creationDate",null,
                true, 0, false);

        QueryableAttribute geometryAttribute = new QueryableAttribute("geometry",null,
                true, 0, false);
        AbstractEntity<CollectionFeature> returnCollection = new Collection(new Model(), "perf",
                "provider","label");
        ParsedStats parsedStats = new ParsedStats();
        Aggregations value = new Aggregations(Arrays.asList(parsedStats));
        Mockito.when(searchService.get(Mockito.any(UniformResourceName.class))).thenReturn(returnCollection);
        Mockito.when(searchService.getAggregations(Mockito.any(SimpleSearchKey.class), Mockito.any(ICriterion.class),
                Mockito.any(java.util.Collection.class))).thenReturn(value);
        Mockito.when(accessRightFilter.getUserAccessGroups()).thenReturn(null);

        CollectionWithStats documentsWithStats = catalogSearchService.getCollectionWithDataObjectsStats(urn, searchType,
                Sets.newHashSet(creationDateAttribute,geometryAttribute));

        Assert.assertNotNull(documentsWithStats);
        Assert.assertNotNull(documentsWithStats.getAggregationList());
        Assert.assertNotNull(documentsWithStats.getCollection());
    }
}
