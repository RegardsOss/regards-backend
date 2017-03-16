/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.search.rest;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.hateoas.Resource;
import org.springframework.http.ResponseEntity;

import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.module.rest.exception.SearchException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.crawler.domain.criterion.ICriterion;
import fr.cnes.regards.modules.crawler.service.IIndexerService;
import fr.cnes.regards.modules.entities.domain.Collection;
import fr.cnes.regards.modules.entities.domain.Dataset;
import fr.cnes.regards.modules.entities.urn.UniformResourceName;
import fr.cnes.regards.modules.search.service.accessright.IAccessRightFilter;
import fr.cnes.regards.modules.search.service.converter.IConverter;
import fr.cnes.regards.modules.search.service.filter.IFilterPlugin;
import fr.cnes.regards.modules.search.service.queryparser.RegardsQueryParser;

/**
 * Unit test for {@link CatalogController}.
 *
 * @author Xavier-Alexandre Brochard
 */
public class CatalogControllerIT {

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
    private IIndexerService indexerService;

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
        indexerService = Mockito.mock(IIndexerService.class);
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
        Mockito.when(runtimeTenantResolver.getTenant()).thenReturn("tenant");
        Mockito.when(resourceService.toResource(Mockito.any()))
                .thenAnswer(invocation -> new Resource<>(invocation.getArguments()[0]));

        // Instanciate the tested class
        catalogController = new CatalogController(queryParser, filterPlugin, accessRightFilter, indexerService,
                converter, runtimeTenantResolver, resourceService);
    }

    /**
     * Check that the system allows to retrieve a specific collection from its URN.
     *
     * @throws SearchException
     */
    @Test
    @Purpose("Check that the system allows to retrieve a specific collection from its URN.")
    @Requirement("REGARDS_DSL_DAM_COL_310")
    public final void getCollection_shouldCallServiceWithCorrectParams() throws SearchException {
        // Prepare the test
        UniformResourceName urn = CatalogControllerTestUtils.URN_COLLECTION;
        Collection expected = CatalogControllerTestUtils.COLLECTION;
        Mockito.when(indexerService.get(urn)).thenReturn(expected);

        ResponseEntity<Resource<Collection>> actual = catalogController.getCollection(urn);

        Assert.assertEquals(expected, actual.getBody().getContent());
        Mockito.verify(indexerService).get(urn);
    }

    /**
     * Check that the system allows to retrieve a specific dataset from its URN.
     *
     * @throws SearchException
     */
    @Test
    @Purpose("Check that the system allows to retrieve a specific dataset from its URN.")
    public final void getDataobject_shouldCallServiceWithCorrectParams() throws SearchException {
        // Prepare the test
        UniformResourceName urn = CatalogControllerTestUtils.URN_DATASET;
        Dataset entity = CatalogControllerTestUtils.DATASET;
        Resource<Dataset> expected = new Resource<>(entity);
        Mockito.when(indexerService.get(urn)).thenReturn(entity);

        ResponseEntity<Resource<Dataset>> actual = catalogController.getDataset(urn);

        Assert.assertEquals(expected, actual);
        Mockito.verify(indexerService).get(urn);
    }

    /**
     * TODO
     */
    @Test
    @Purpose("TODO.")
    @Requirement("TODO")
    public final void getDataset_shouldCallServiceWithCorrectParams() {
        Assert.fail("Not tested");
    }

    /**
     * TODO
     */
    @Test
    @Purpose("TODO.")
    @Requirement("TODO")
    public final void getDocument_shouldCallServiceWithCorrectParams() {
        Assert.fail("Not tested");
    }

    /**
     * TODO
     */
    @Test
    @Purpose("TODO.")
    @Requirement("TODO")
    public final void searchAll_shouldReturnExpectedResult() {
        Assert.fail("Not tested");
    }

    /**
     * TODO
     */
    @Test
    @Purpose("TODO.")
    @Requirement("TODO")
    public final void searchCollections_shouldReturnExpectedResult() {
        Assert.fail("Not tested");
    }

    /**
     * TODO
     */
    @Test
    @Purpose("TODO.")
    @Requirement("TODO")
    public final void searchDataobjects_shouldReturnExpectedResult() {
        Assert.fail("Not tested");
    }

    /**
     * TODO
     */
    @Test
    @Purpose("TODO.")
    @Requirement("TODO")
    public final void searchDatasets_shouldReturnExpectedResult() {
        Assert.fail("Not tested");
    }

    /**
     * TODO
     */
    @Test
    @Purpose("TODO.")
    @Requirement("TODO")
    public final void searchDocuments_shouldReturnExpectedResult() {
        Assert.fail("Not tested");
    }

}
