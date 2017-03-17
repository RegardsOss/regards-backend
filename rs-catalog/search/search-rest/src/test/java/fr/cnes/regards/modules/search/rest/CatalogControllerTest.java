/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.search.rest;

import java.util.List;

import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;

import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.module.rest.exception.SearchException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.crawler.domain.SearchKey;
import fr.cnes.regards.modules.crawler.domain.criterion.ICriterion;
import fr.cnes.regards.modules.entities.domain.DataObject;
import fr.cnes.regards.modules.search.rest.CatalogController.SearchType;
import fr.cnes.regards.modules.search.service.ISearchService;
import fr.cnes.regards.modules.search.service.accessright.IAccessRightFilter;
import fr.cnes.regards.modules.search.service.converter.IConverter;
import fr.cnes.regards.modules.search.service.filter.IFilterPlugin;
import fr.cnes.regards.modules.search.service.queryparser.RegardsQueryParser;

/**
 * Unit test for {@link CatalogController}.
 *
 * @author Xavier-Alexandre Brochard
 */
public class CatalogControllerTest {

    /**
     * Class under test
     */
    private CatalogController catalogController;

    /**
     * The custom OpenSearch query parser building {@link ICriterion} from tu string query
     */
    private RegardsQueryParser queryParser;

    /**
     * Applies project filters, i.e. the OpenSearch query
     */
    private IFilterPlugin filterPlugin;

    /**
     * Adds user group and data access filters
     */
    private IAccessRightFilter accessRightFilter;

    /**
     * Service perfoming the ElasticSearch search
     */
    private ISearchService searchService;

    /**
     * Converts entities after search
     */
    private IConverter converter;

    /**
     * Get current tenant at runtime and allows tenant forcing
     */
    private IRuntimeTenantResolver runtimeTenantResolver;

    /**
     * The resource service
     */
    private IResourceService resourceService;

    @Before
    public void setUp() {
        // Declare mocks
        queryParser = Mockito.mock(RegardsQueryParser.class);
        filterPlugin = Mockito.mock(IFilterPlugin.class);
        accessRightFilter = Mockito.mock(IAccessRightFilter.class);
        searchService = Mockito.mock(ISearchService.class);
        converter = Mockito.mock(IConverter.class);
        runtimeTenantResolver = Mockito.mock(IRuntimeTenantResolver.class);
        resourceService = Mockito.mock(IResourceService.class);

        // Globally mock what's mockable yet
        Mockito.when(filterPlugin.addFilter(Mockito.any(), Mockito.any()))
                .thenAnswer(invocation -> invocation.getArguments()[1]);
        Mockito.when(accessRightFilter.removeGroupFilter(Mockito.any()))
                .thenAnswer(invocation -> invocation.getArguments()[0]);
        Mockito.when(accessRightFilter.addGroupFilter(Mockito.any()))
                .thenAnswer(invocation -> invocation.getArguments()[0]);
        Mockito.when(accessRightFilter.addAccessRightsFilter(Mockito.any()))
                .thenAnswer(invocation -> invocation.getArguments()[0]);
        Mockito.when(runtimeTenantResolver.getTenant()).thenReturn(CatalogControllerTestUtils.TENANT);
        Mockito.when(resourceService.toResource(Mockito.any()))
                .thenAnswer(invocation -> new Resource<>(invocation.getArguments()[0]));

        // Instanciate the tested class
        catalogController = new CatalogController(queryParser, filterPlugin, accessRightFilter, searchService,
                converter, runtimeTenantResolver, resourceService);
    }

    /**
     * Test the main search method
     *
     * @throws SearchException
     * @throws QueryNodeException
     */
    @SuppressWarnings("unchecked")
    @Test
    public void doSearch_shouldCallServiceWithRightParams() throws SearchException, QueryNodeException {
        // Prepare test
        SearchType searchType = SearchType.DATAOBJECT;
        Class<DataObject> resultClass = DataObject.class;
        String q = CatalogControllerTestUtils.Q;
        List<String> facets = CatalogControllerTestUtils.FACETS;
        Sort sort = CatalogControllerTestUtils.SORT;
        PagedResourcesAssembler<DataObject> assembler = CatalogControllerTestUtils.ASSEMBLER_DATAOBJECT;
        Pageable pageable = CatalogControllerTestUtils.PAGEABLE;

        // Define expected values
        SearchKey<DataObject> expectedSearchKey = new SearchKey<>(CatalogControllerTestUtils.TENANT,
                searchType.toString(), resultClass);
        ICriterion expectedCriterion = CatalogControllerTestUtils.SIMPLE_STRING_MATCH_CRITERION;
        Page<DataObject> expectedSearchResult = CatalogControllerTestUtils.PAGE_DATAOBJECT;

        // Mock dependencies
        Mockito.when(queryParser.parse(q)).thenReturn(expectedCriterion);
        Mockito.when(searchService.search(Mockito.any(SearchKey.class), Mockito.any(Pageable.class),
                                          Mockito.any(ICriterion.class), Mockito.any(), Mockito.any()))
                .thenReturn(expectedSearchResult);
        PagedResources<Resource<DataObject>> pageResources = CatalogControllerTestUtils.PAGED_RESOURCES_DATAOBJECT;
        Mockito.when(assembler.toResource(Mockito.any())).thenReturn(pageResources);

        // Perform the test
        catalogController.doSearch(q, searchType, resultClass, facets, sort, pageable, assembler);

        // Check
        Mockito.verify(searchService).search(Mockito.refEq(expectedSearchKey), Mockito.refEq(pageable),
                                             Mockito.refEq(expectedCriterion), Mockito.any(), Mockito.any());
    }

}
