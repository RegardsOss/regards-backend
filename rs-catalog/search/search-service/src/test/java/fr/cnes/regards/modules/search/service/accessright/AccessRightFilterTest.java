/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.search.service.accessright;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.hateoas.HateoasUtils;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.security.utils.jwt.JWTAuthentication;
import fr.cnes.regards.framework.security.utils.jwt.UserDetails;
import fr.cnes.regards.modules.accessrights.client.IProjectUsersClient;
import fr.cnes.regards.modules.dataaccess.client.IAccessGroupClient;
import fr.cnes.regards.modules.dataaccess.client.IUserClient;
import fr.cnes.regards.modules.indexer.domain.criterion.ICriterion;
import fr.cnes.regards.modules.indexer.domain.criterion.StringMatchCriterion;
import fr.cnes.regards.modules.models.client.IAttributeModelClient;
import fr.cnes.regards.modules.opensearch.service.OpenSearchService;
import fr.cnes.regards.modules.opensearch.service.cache.attributemodel.AttributeFinder;
import fr.cnes.regards.modules.opensearch.service.cache.attributemodel.AttributeModelCache;
import fr.cnes.regards.modules.opensearch.service.cache.attributemodel.IAttributeFinder;
import fr.cnes.regards.modules.opensearch.service.cache.attributemodel.IAttributeModelCache;
import fr.cnes.regards.modules.opensearch.service.exception.OpenSearchParseException;
import fr.cnes.regards.modules.search.domain.Terms;
import fr.cnes.regards.modules.search.service.cache.accessgroup.AccessGroupCache;
import fr.cnes.regards.modules.search.service.cache.accessgroup.IAccessGroupCache;
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
     * The OpenSearch service building {@link ICriterion} from a request string.  Easier to build criterion then doing it manually.
     */
    private OpenSearchService openSearchService;

    /**
     * Criterion visitor responsible for finding a "groups" criterion
     */
    private static NamedCriterionFinderVisitor GROUPS_FINDER = new NamedCriterionFinderVisitor(Terms.GROUPS.getName());

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
        IUserClient userClient = Mockito.mock(IUserClient.class);
        Mockito.when(userClient.retrieveAccessGroupsOfUser(Mockito.anyString(), Mockito.anyInt(), Mockito.anyInt()))
                .thenReturn(SampleDataUtils.USER_CLIENT_RESPONSE);
        IProjectUsersClient projectUsersClient = Mockito.mock(IProjectUsersClient.class);
        Mockito.when(projectUsersClient.isAdmin(Mockito.anyString()))
                .thenReturn(SampleDataUtils.PROJECT_USERS_CLIENT_RESPONSE);
        IRuntimeTenantResolver runtimeTenantResolver = Mockito.mock(IRuntimeTenantResolver.class);
        Mockito.when(runtimeTenantResolver.getTenant()).thenReturn(TENANT);
        IAttributeModelClient attributeModelClient = Mockito.mock(IAttributeModelClient.class);
        Mockito.when(attributeModelClient.getAttributes(null, null))
                .thenReturn(new ResponseEntity<>(HateoasUtils.wrapList(SampleDataUtils.LIST), HttpStatus.OK));
        IAttributeModelCache attributeModelCache = new AttributeModelCache(attributeModelClient, subscriber,
                runtimeTenantResolver);
        IAttributeFinder finder = new AttributeFinder(runtimeTenantResolver, attributeModelCache);
        IAccessGroupCache accessGroupCache = new AccessGroupCache(userClient, Mockito.mock(IAccessGroupClient.class));

        openSearchService = new OpenSearchService(finder);
        accessRightFilter = new AccessRightFilter(accessGroupCache, runtimeTenantResolver, projectUsersClient);
    }

    @After
    public void validate() {
        Mockito.validateMockitoUsage();
    }

    /**
     * Test that a string query cannot use a "groups" term.
     * @throws OpenSearchParseException
     * @throws UnsupportedEncodingException
     */
    @Test(expected = OpenSearchParseException.class)
    public final void test_userCannotInjectGroupsTermInQuery()
            throws OpenSearchParseException, UnsupportedEncodingException {
        String q = "q=" + URLEncoder.encode(SampleDataUtils.QUERY_WITH_GROUPS, "UTF-8");
        openSearchService.parse(q);
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.search.service.accessright.AccessRightFilter#addAccessRights(fr.cnes.regards.modules.indexer.domain.criterion.ICriterion)}.
     * @throws OpenSearchParseException
     * @throws UnsupportedEncodingException
     * @throws AccessRightFilterException
     */
    @Test
    public final void testAddUserGroups()
            throws OpenSearchParseException, UnsupportedEncodingException, AccessRightFilterException {
        // Mock authentication
        final JWTAuthentication jwtAuth = new JWTAuthentication("foo");
        final UserDetails details = new UserDetails();
        details.setName(SampleDataUtils.EMAIL);
        jwtAuth.setUser(details);
        SecurityContextHolder.getContext().setAuthentication(jwtAuth);

        // Init the criterion we add groups to
        String q = "q=" + URLEncoder.encode(SampleDataUtils.QUERY, "UTF-8");
        ICriterion criterion = openSearchService.parse(q);

        // Add groups
        ICriterion criterionWithGroups = accessRightFilter.addAccessRights(criterion);

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
