/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.security.filter;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.InsufficientAuthenticationException;

import fr.cnes.regards.framework.security.utils.HttpConstants;
import fr.cnes.regards.framework.security.utils.jwt.JWTAuthentication;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;
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
public class JWTAuthenticationFilterTest {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(JWTAuthenticationFilterTest.class);

    private final JWTService jwtService = new JWTService();

    @Before
    public void init() {
        jwtService.setSecret("123456789");
    }

    /**
     *
     * Check security filter with no Jwt access token
     *
     * @since 1.0-SNAPSHOT
     */
    @Requirement("REGARDS_DSL_SYS_SEC_100")
    @Purpose("Check security filter with no Jwt access token")
    @Test
    public void jwtFilterAccessDeniedWithoutToken() {

        final JWTAuthentication token = new JWTAuthentication("token");
        token.setRole("USER");

        final HttpServletRequest mockedRequest = Mockito.mock(HttpServletRequest.class);
        final HttpServletResponse mockedResponse = new MockHttpServletResponse();

        final AuthenticationManager mockedManager = Mockito.mock(AuthenticationManager.class);

        final JWTAuthenticationFilter filter = new JWTAuthenticationFilter(mockedManager, jwtService);

        try {
            filter.doFilter(mockedRequest, mockedResponse, new MockFilterChain());
            if (mockedResponse.getStatus() == HttpStatus.OK.value()) {
                Assert.fail("Authentication should fail.");
            }
        } catch (final InsufficientAuthenticationException e) {
            // Nothing to do
            LOG.info(e.getMessage());
        } catch (IOException | ServletException e) {
            LOG.error(e.getMessage(), e);
            Assert.fail(e.getMessage());
        }
    }

    /**
     *
     * Check security filter with no Jwt access token
     *
     * @since 1.0-SNAPSHOT
     */
    @Requirement("REGARDS_DSL_SYS_SEC_100")
    @Purpose("Check security filter with no Jwt access token for public access (scope as query parameter)")
    @Test
    public void jwtFilterPublicAccess() {

        final HttpServletRequest mockedRequest = Mockito.mock(HttpServletRequest.class);
        final HttpServletResponse mockedResponse = new MockHttpServletResponse();

        Mockito.when(mockedRequest.getParameter(HttpConstants.SCOPE)).thenReturn("project-test");

        final AuthenticationManager mockedManager = Mockito.mock(AuthenticationManager.class);

        final JWTAuthenticationFilter filter = new JWTAuthenticationFilter(mockedManager, jwtService);

        try {
            filter.doFilter(mockedRequest, mockedResponse, new MockFilterChain());
            if (mockedResponse.getStatus() != HttpStatus.OK.value()) {
                Assert.fail("Authentication should be granted.");
            }
        } catch (final InsufficientAuthenticationException e) {
            // Nothing to do
            LOG.info(e.getMessage());
        } catch (IOException | ServletException e) {
            LOG.error(e.getMessage(), e);
            Assert.fail(e.getMessage());
        }
    }

    /**
     *
     * Check security filter with no Jwt access token
     *
     * @since 1.0-SNAPSHOT
     */
    @Requirement("REGARDS_DSL_SYS_SEC_100")
    @Purpose("Check security filter with no Jwt access token for public access (scope in header)")
    @Test
    public void jwtFilterPublicAccessWithHeader() {

        final HttpServletRequest mockedRequest = Mockito.mock(HttpServletRequest.class);
        final HttpServletResponse mockedResponse = new MockHttpServletResponse();

        Mockito.when(mockedRequest.getHeader(HttpConstants.SCOPE)).thenReturn("project-test");

        final AuthenticationManager mockedManager = Mockito.mock(AuthenticationManager.class);

        final JWTAuthenticationFilter filter = new JWTAuthenticationFilter(mockedManager, jwtService);

        try {
            filter.doFilter(mockedRequest, mockedResponse, new MockFilterChain());
            if (mockedResponse.getStatus() != HttpStatus.OK.value()) {
                Assert.fail("Authentication should be granted.");
            }
        } catch (final InsufficientAuthenticationException e) {
            // Nothing to do
            LOG.info(e.getMessage());
        } catch (IOException | ServletException e) {
            LOG.error(e.getMessage(), e);
            Assert.fail(e.getMessage());
        }
    }

    /**
     *
     * "Check security filter with Jwt access token
     *
     * @since 1.0-SNAPSHOT
     */
    @Requirement("REGARDS_DSL_SYS_SEC_100")
    @Purpose("Check security filter with invlid request authorization header")
    @Test
    public void jwtFilterAccessDeniedTest() {

        final JWTAuthentication token = new JWTAuthentication("token");
        token.setRole("USER");

        final HttpServletRequest mockedRequest = Mockito.mock(HttpServletRequest.class);
        final HttpServletResponse mockedResponse = new MockHttpServletResponse();

        final AuthenticationManager mockedManager = Mockito.mock(AuthenticationManager.class);

        final JWTAuthenticationFilter filter = new JWTAuthenticationFilter(mockedManager, jwtService);

        // Header whithout Bearer: prefix.
        Mockito.when(mockedRequest.getHeader(HttpConstants.AUTHORIZATION)).thenReturn(token.getJwt());

        try {
            filter.doFilter(mockedRequest, mockedResponse, new MockFilterChain());
            if (mockedResponse.getStatus() == HttpStatus.OK.value()) {
                Assert.fail("Authentication should fail.");
            }
        } catch (final InsufficientAuthenticationException e) {
            // Nothing to do
            LOG.info(e.getMessage());
        } catch (IOException | ServletException e) {
            LOG.error(e.getMessage(), e);
            Assert.fail(e.getMessage());
        }
    }

    /**
     *
     * Check security filter with valid Jwt access token
     *
     * @since 1.0-SNAPSHOT
     */
    @Requirement("REGARDS_DSL_SYS_SEC_100")
    @Purpose("Check security filter with valid Jwt access token")
    @Test
    public void jwtFilterAccessGrantedTest() {

        final JWTAuthentication token = new JWTAuthentication("token");
        token.setRole("USER");

        final HttpServletRequest mockedRequest = Mockito.mock(HttpServletRequest.class);
        final HttpServletResponse mockedResponse = new MockHttpServletResponse();

        final AuthenticationManager mockedManager = Mockito.mock(AuthenticationManager.class);

        final JWTAuthenticationFilter filter = new JWTAuthenticationFilter(mockedManager, jwtService);

        Mockito.when(mockedRequest.getHeader(HttpConstants.AUTHORIZATION))
                .thenReturn(String.format("%s: %s", HttpConstants.BEARER, token.getJwt()));

        try {
            filter.doFilter(mockedRequest, mockedResponse, new MockFilterChain());
            if (mockedResponse.getStatus() != HttpStatus.OK.value()) {
                Assert.fail("Authentication should be granted");
            }
        } catch (IOException | ServletException e) {
            LOG.error(e.getMessage(), e);
            Assert.fail(e.getMessage());
        }

    }

}
