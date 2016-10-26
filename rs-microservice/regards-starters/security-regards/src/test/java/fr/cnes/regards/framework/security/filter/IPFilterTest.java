/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.security.filter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;

import fr.cnes.regards.framework.security.domain.ResourceMapping;
import fr.cnes.regards.framework.security.endpoint.IAuthoritiesProvider;
import fr.cnes.regards.framework.security.utils.jwt.JWTAuthentication;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;

/**
 *
 * Class IPFilterTest
 *
 * IP Filter tests
 *
 * @author sbinda
 * @since 1.0-SNAPSHOT
 */
public class IPFilterTest {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(IPFilterTest.class);

    /**
     * Test authorized address
     */
    private static final String AUTHORIZED_ADRESS = "127.0.0.1";

    /**
     * Test authorized address
     */
    private static final String AUTHORIZED_ADRESS_PATTERN = "127.0.0.*";

    /**
     * Test user Role
     */
    private static final String ROLE_NAME = "USER";

    /**
     *
     * Check security filter with ip adress for endpoints accesses
     *
     * @since 1.0-SNAPSHOT
     */
    @Requirement("REGARDS_DSL_SYS_SEC_200")
    @Purpose("Check security filter with ip adress for endpoints accesses")
    @Test
    public void ipFilterTest() {

        final JWTAuthentication token = new JWTAuthentication("token");
        token.setRole(ROLE_NAME);
        SecurityContextHolder.getContext().setAuthentication(token);

        final IpFilter filter = new IpFilter(new IAuthoritiesProvider() {

            @Override
            public List<String> getRoleAuthorizedAddress(final String pRole) {
                final List<String> results = new ArrayList<>();
                results.add(AUTHORIZED_ADRESS);
                return results;
            }

            @Override
            public List<ResourceMapping> getResourcesAccessConfiguration() {
                return null;
            }
        });

        final HttpServletRequest mockedRequest = Mockito.mock(HttpServletRequest.class);
        final HttpServletResponse mockedResponse = Mockito.mock(HttpServletResponse.class);

        Mockito.when(mockedRequest.getRemoteAddr()).thenReturn(AUTHORIZED_ADRESS);

        try {
            filter.doFilter(mockedRequest, mockedResponse, new MockFilterChain());
        } catch (IOException | ServletException e) {
            Assert.fail(e.getMessage());
        }

        Mockito.when(mockedRequest.getRemoteAddr()).thenReturn("127.0.0.2");

        try {
            filter.doFilter(mockedRequest, mockedResponse, new MockFilterChain());
            Assert.fail("There should be an error. Address not authorized");
        } catch (final InsufficientAuthenticationException e) {
            LOG.info(e.getMessage());
        } catch (IOException | ServletException e) {
            Assert.fail(e.getMessage());
        }

    }

    /**
     *
     * Check security filter with ip adress for endpoints accesses
     *
     * @since 1.0-SNAPSHOT
     */
    @Requirement("REGARDS_DSL_SYS_SEC_200")
    @Purpose("Check security filter with subdomain ip adress for endpoints accesses")
    @Test
    public void subdomainIpFilterTest() {

        final JWTAuthentication token = new JWTAuthentication("token");
        token.setRole(ROLE_NAME);
        SecurityContextHolder.getContext().setAuthentication(token);

        final IpFilter filter = new IpFilter(new IAuthoritiesProvider() {

            @Override
            public List<String> getRoleAuthorizedAddress(final String pRole) {
                final List<String> results = new ArrayList<>();
                results.add(AUTHORIZED_ADRESS_PATTERN);
                return results;
            }

            @Override
            public List<ResourceMapping> getResourcesAccessConfiguration() {
                return null;
            }
        });

        final HttpServletRequest mockedRequest = Mockito.mock(HttpServletRequest.class);
        final HttpServletResponse mockedResponse = Mockito.mock(HttpServletResponse.class);

        Mockito.when(mockedRequest.getRemoteAddr()).thenReturn(AUTHORIZED_ADRESS);

        try {
            filter.doFilter(mockedRequest, mockedResponse, new MockFilterChain());
        } catch (IOException | ServletException e) {
            Assert.fail(e.getMessage());
        }

        Mockito.when(mockedRequest.getRemoteAddr()).thenReturn("127.0.1.1");

        try {
            filter.doFilter(mockedRequest, mockedResponse, new MockFilterChain());
            Assert.fail("There should be an error. Address not authorized");
        } catch (final InsufficientAuthenticationException e) {
            LOG.info(e.getMessage());
        } catch (IOException | ServletException e) {
            Assert.fail(e.getMessage());
        }

    }

}
