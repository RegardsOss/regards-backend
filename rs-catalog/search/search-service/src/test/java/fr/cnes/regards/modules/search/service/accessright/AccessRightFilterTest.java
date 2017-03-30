/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.search.service.accessright;

import java.util.List;
import java.util.Optional;

import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.hateoas.Resource;
import org.springframework.http.ResponseEntity;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.indexer.domain.criterion.ICriterion;
import fr.cnes.regards.modules.models.client.IAttributeModelClient;
import fr.cnes.regards.modules.models.domain.attributes.AttributeModel;
import fr.cnes.regards.modules.search.service.cache.attributemodel.AttributeModelCache;
import fr.cnes.regards.modules.search.service.cache.attributemodel.IAttributeModelCache;
import fr.cnes.regards.modules.search.service.queryparser.RegardsQueryParser;
import fr.cnes.regards.modules.search.service.utils.SampleDataUtils;

/**
 * Test class for {@link AccessRightFilter}.
 * @author Xavier-Alexandre Brochard
 */
public class AccessRightFilterTest {

    /**
     * Service under test
     */
    private static AccessRightFilter accessRightFilter = new AccessRightFilter();

    /**
     * Query Parse building criterions from a string query. Easier to build criterion then doing it manually
     */
    private static RegardsQueryParser parser;

    /**
     * The "groups" term of a query / the name of the "groups" criterion
     */
    private static String GROUPS_TERM = "groups";

    /**
     * Criterion visitor responsible for finding a "groups" criterion
     */
    private static NamedCriterionFinderVisitor GROUPS_FINDER = new NamedCriterionFinderVisitor(GROUPS_TERM);

    /**
     * The tenant
     */
    private static final String TENANT = "tenant";

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        ISubscriber subscriber = Mockito.mock(ISubscriber.class);
        IAttributeModelClient attributeModelClient = Mockito.mock(IAttributeModelClient.class);
        IRuntimeTenantResolver runtimeTenantResolver = Mockito.mock(IRuntimeTenantResolver.class);
        Mockito.when(runtimeTenantResolver.getTenant()).thenReturn(TENANT);
        IAttributeModelCache attributeModelCache = new AttributeModelCache(attributeModelClient, subscriber,
                runtimeTenantResolver);

        ResponseEntity<List<Resource<AttributeModel>>> clientResponse = SampleDataUtils.CLIENT_RESPONSE;
        Mockito.when(attributeModelClient.getAttributes(null, null)).thenReturn(clientResponse);

        parser = new RegardsQueryParser(attributeModelCache);
    }

    /**
     * Test that a string query cannot use a "groups" term.
     * @throws QueryNodeException
     */
    @Test(expected = QueryNodeException.class)
    public final void test_userCannotInjectGroupsTermInQuery() throws QueryNodeException {
        parser.parse(SampleDataUtils.QUERY_WITH_GROUPS);
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.search.service.accessright.AccessRightFilter#addUserGroups(fr.cnes.regards.modules.indexer.domain.criterion.ICriterion)}.
     * @throws QueryNodeException
     */
    @Test
    public final void testAddUserGroups() throws QueryNodeException {
        ICriterion criterion = parser.parse(SampleDataUtils.QUERY);

        ICriterion criterionWithGroups = accessRightFilter.addUserGroups(criterion);

        Optional<ICriterion> groupsCriterion = criterionWithGroups.accept(GROUPS_FINDER);
        Assert.assertTrue(groupsCriterion.isPresent());
    }

}
