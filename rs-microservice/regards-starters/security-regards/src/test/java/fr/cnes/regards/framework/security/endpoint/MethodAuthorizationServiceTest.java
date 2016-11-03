/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.security.endpoint;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.aopalliance.intercept.MethodInvocation;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDecisionVoter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.util.MethodInvocationUtils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.domain.ResourceMapping;
import fr.cnes.regards.framework.security.utils.endpoint.RoleAuthority;
import fr.cnes.regards.framework.security.utils.jwt.JWTAuthentication;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;

/**
 *
 * Class MethodAuthorizationServiceTest
 *
 * Test class for method authorization service.
 *
 * @author sbinda
 * @since 1.0-SNAPSHOT
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestConfiguration.class)
public class MethodAuthorizationServiceTest {

    /**
     * Role label for tests.
     */
    private static final String ROLE_LABEL = "USER";

    /**
     * MEthod authroization service
     */
    @Autowired
    private MethodAuthorizationService methodAuthService;

    /**
     *
     * Check that the resource voter accept or denied access to endpoints controller
     *
     * @throws NoSuchMethodException
     *             test internal error
     * @throws SecurityException
     *             test internal error
     * @since 1.0-SNAPSHOT
     */
    @Requirement("REGARDS_DSL_SYS_SEC_200")
    @Purpose("Verify access granted/denied to endpoints defined in authorized resources for a specific tenant")
    @Test
    public void testResourceAccessVoterVote() throws NoSuchMethodException, SecurityException {

        /**
         *
         * Class Controller
         *
         * Test controller
         *
         * @author sbinda
         * @since 1.0-SNAPSHOT
         */
        @RequestMapping
        class Controller {

            @ResourceAccess(name = "/endpoint1", description = "description")
            @RequestMapping(value = "/endpoint1", method = RequestMethod.GET)
            public Object endpoint() {
                return null;
            }
        }
        final MethodInvocation methodIvoncation = MethodInvocationUtils.create(new Controller(), "endpoint");

        final JWTAuthentication authenticationMock = Mockito.mock(JWTAuthentication.class);

        final Collection<RoleAuthority> authorities = new ArrayList<>();
        authorities.add(new RoleAuthority(ROLE_LABEL));
        Mockito.doReturn(authorities).when(authenticationMock).getAuthorities();
        Mockito.when(authenticationMock.getTenant()).thenReturn(TestConfiguration.TENANT_1);

        final ResourceAccessVoter voter = new ResourceAccessVoter(methodAuthService);
        int result = voter.vote(authenticationMock, methodIvoncation, null);
        Assert.assertEquals(result, AccessDecisionVoter.ACCESS_GRANTED);

        Mockito.when(authenticationMock.getTenant()).thenReturn("tenant-3");

        result = voter.vote(authenticationMock, methodIvoncation, null);
        Assert.assertEquals(result, AccessDecisionVoter.ACCESS_DENIED);

    }

    /**
     *
     * Check that the resource voter denied access to endpoints controller not auhtorized
     *
     * @throws NoSuchMethodException
     *             test internal error
     * @throws SecurityException
     *             test internal error
     * @since 1.0-SNAPSHOT
     */
    @Requirement("REGARDS_DSL_SYS_SEC_200")
    @Purpose("Verify access denied to endpoints not defined in authorized resources")
    @Test
    public void testResourceAccessVoterVoteDenied() throws NoSuchMethodException, SecurityException {

        /**
         *
         * Class Controller
         *
         * Test controller
         *
         * @author sbinda
         * @since 1.0-SNAPSHOT
         */
        @RequestMapping
        class Controller {

            @ResourceAccess(name = "/endpoint1/unauthorized", description = "description")
            @RequestMapping(value = "/endpoint/unauthorized", method = RequestMethod.GET)
            public Object endpoint() {
                return null;
            }
        }
        final MethodInvocation methodIvoncation = MethodInvocationUtils.create(new Controller(), "endpoint");

        final JWTAuthentication authenticationMock = Mockito.mock(JWTAuthentication.class);

        final Collection<RoleAuthority> authorities = new ArrayList<>();
        authorities.add(new RoleAuthority(ROLE_LABEL));
        Mockito.doReturn(authorities).when(authenticationMock).getAuthorities();
        Mockito.when(authenticationMock.getTenant()).thenReturn(TestConfiguration.TENANT_1);

        final ResourceAccessVoter voter = new ResourceAccessVoter(methodAuthService);
        final int result = voter.vote(authenticationMock, methodIvoncation, null);
        Assert.assertEquals(result, AccessDecisionVoter.ACCESS_DENIED);

    }

    /**
     *
     * testManagedAuthoritiesByTenantAndResources
     *
     * @since 1.0-SNAPSHOT
     */
    @Requirement("REGARDS_DSL_SYS_SEC_200")
    @Purpose("Verify internal management of resources endponts by tenants and resources")
    @Test
    public void testManagedAuthoritiesByTenantAndResources() {

        final String resourcePath = "new/path";
        final int expectedResult = 4;
        methodAuthService.init();
        methodAuthService.setAuthorities(TestConfiguration.TENANT_1, resourcePath, RequestMethod.GET, "TEST_ROLE",
                                         "TEST_ROLE_2");

        final Map<String, ArrayList<GrantedAuthority>> authorities = methodAuthService
                .getTenantAuthorities(TestConfiguration.TENANT_1);
        Assert.assertEquals(authorities.size(), expectedResult);
        final Optional<List<GrantedAuthority>> roles = methodAuthService
                .getAuthorities(TestConfiguration.TENANT_1, new ResourceMapping(resourcePath, RequestMethod.GET));
        Assert.assertEquals(roles.get().size(), 2);
        Assert.assertEquals(roles.get().get(0).getAuthority(), "ROLE_TEST_ROLE");

    }

}
