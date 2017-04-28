/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.search.service.accessright;

import java.util.List;

import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.security.core.context.SecurityContextHolder;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.security.utils.jwt.JWTAuthentication;
import fr.cnes.regards.framework.security.utils.jwt.UserDetails;
import fr.cnes.regards.modules.accessrights.client.IProjectUsersClient;
import fr.cnes.regards.modules.dataaccess.client.IUserClient;
import fr.cnes.regards.modules.indexer.domain.criterion.ICriterion;
import fr.cnes.regards.modules.indexer.domain.criterion.StringMatchCriterion;
import fr.cnes.regards.modules.models.client.IAttributeModelClient;
import fr.cnes.regards.modules.queryparser.service.RegardsQueryParser;
import fr.cnes.regards.modules.queryparser.service.cache.attributemodel.AttributeModelCache;
import fr.cnes.regards.modules.queryparser.service.cache.attributemodel.IAttributeModelCache;
import fr.cnes.regards.modules.search.domain.Terms;
import fr.cnes.regards.modules.search.service.cache.accessgroup.AccessGroupClientService;
import fr.cnes.regards.modules.search.service.cache.accessgroup.IAccessGroupClientService;
import fr.cnes.regards.modules.search.service.utils.SampleDataUtils;

/**
 * Test class for {@link AccessRightFilter}.
 * @author Xavier-Alexandre Brochard
 */
public class AccessRightFilterTest {

    /**
     * Service under test
     */
    private AccessRightFilter accessRightFilter;

    /**
     * Query Parse building criterions from a string query. Easier to build criterion then doing it manually
     */
    private RegardsQueryParser parser;

    /**
     * Criterion visitor responsible for finding a "groups" criterion
     */
    private static NamedCriterionFinderVisitor GROUPS_FINDER = new NamedCriterionFinderVisitor(Terms.GROUPS.getName());

    /**
     * The tenant
     */
    private static final String TENANT = "tenant";

    private ISubscriber subscriber;

    private IAttributeModelClient attributeModelClient;

    private IUserClient userClient;

    private IProjectUsersClient projectUsersClient;

    private IRuntimeTenantResolver runtimeTenantResolver;

    private IAttributeModelCache attributeModelCache;

    private IAccessGroupClientService accessGroupCache;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        subscriber = Mockito.mock(ISubscriber.class);
        attributeModelClient = Mockito.mock(IAttributeModelClient.class);
        userClient = Mockito.mock(IUserClient.class);
        projectUsersClient = Mockito.mock(IProjectUsersClient.class);
        runtimeTenantResolver = Mockito.mock(IRuntimeTenantResolver.class);
        Mockito.when(runtimeTenantResolver.getTenant()).thenReturn(TENANT);
        attributeModelCache = new AttributeModelCache(attributeModelClient, subscriber, runtimeTenantResolver);
        accessGroupCache = new AccessGroupClientService(userClient, subscriber);

        Mockito.when(attributeModelClient.getAttributes(null, null))
                .thenReturn(SampleDataUtils.ATTRIBUTE_MODEL_CLIENT_RESPONSE);
        Mockito.when(userClient.retrieveAccessGroupsOfUser(Mockito.anyString(), Mockito.anyInt(), Mockito.anyInt()))
                .thenReturn(SampleDataUtils.USER_CLIENT_RESPONSE);
        Mockito.when(projectUsersClient.isAdmin(Mockito.anyString()))
                .thenReturn(SampleDataUtils.PROJECT_USERS_CLIENT_RESPONSE);

        parser = new RegardsQueryParser(attributeModelCache);

        accessRightFilter = new AccessRightFilter(accessGroupCache, runtimeTenantResolver, projectUsersClient);
    }

    @After
    public void validate() {
        Mockito.validateMockitoUsage();
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
        // Mock authentication
        final JWTAuthentication jwtAuth = new JWTAuthentication("foo");
        final UserDetails details = new UserDetails();
        details.setName(SampleDataUtils.EMAIL);
        jwtAuth.setUser(details);
        SecurityContextHolder.getContext().setAuthentication(jwtAuth);

        // Init the criterion we add groups to
        ICriterion criterion = parser.parse(SampleDataUtils.QUERY);

        // Add groups
        ICriterion criterionWithGroups = accessRightFilter.addUserGroups(criterion);

        // Check we found two "groups" criterion in the generated criterion
        List<ICriterion> found = (List<ICriterion>) criterionWithGroups.accept(GROUPS_FINDER);
        Assert.assertFalse(found.isEmpty());
        Assert.assertThat(found, Matchers.iterableWithSize(2));
        Assert.assertTrue(found.get(0) instanceof StringMatchCriterion);
        Assert.assertEquals(((StringMatchCriterion) found.get(0)).getName(), Terms.GROUPS.getName());
        Assert.assertTrue(found.get(1) instanceof StringMatchCriterion);
        Assert.assertEquals(((StringMatchCriterion) found.get(1)).getName(), Terms.GROUPS.getName());
    }

}
