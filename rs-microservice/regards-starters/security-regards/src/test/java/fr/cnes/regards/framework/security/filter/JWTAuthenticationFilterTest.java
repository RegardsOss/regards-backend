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
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.web.servlet.DispatcherServlet;

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

        final JWTAuthentication token = new JWTAuthentication(
                jwtService.generateToken("PROJECT", "test@test.test", "USER"));

        final HttpServletRequest mockedRequest = Mockito.mock(HttpServletRequest.class);
        final HttpServletResponse mockedResponse = new MockHttpServletResponse();

        final AuthenticationManager mockedManager = Mockito.mock(AuthenticationManager.class);

        final JWTAuthenticationFilter filter = new JWTAuthenticationFilter(mockedManager);

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

        final MockHttpServletRequest mockedRequest = new MockHttpServletRequest();
        mockedRequest.addParameter(HttpConstants.SCOPE, "project-test");

        final HttpServletResponse mockedResponse = new MockHttpServletResponse();

        PublicAuthenticationFilter publicFilter = new PublicAuthenticationFilter(jwtService);
        final AuthenticationManager mockedManager = Mockito.mock(AuthenticationManager.class);
        final JWTAuthenticationFilter filter = new JWTAuthenticationFilter(mockedManager);

        DispatcherServlet servlet = Mockito.mock(DispatcherServlet.class);
        MockFilterChain mockedFilterChain = new MockFilterChain(servlet, publicFilter, filter);

        try {
            mockedFilterChain.doFilter(mockedRequest, mockedResponse);
            // filter.doFilter(mockedFilterChain.getRequest(), mockedFilterChain.getResponse(), mockedFilterChain);
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

        final MockHttpServletRequest mockedRequest = new MockHttpServletRequest();
        mockedRequest.addHeader(HttpConstants.SCOPE, "project-test");

        final HttpServletResponse mockedResponse = new MockHttpServletResponse();

        PublicAuthenticationFilter publicFilter = new PublicAuthenticationFilter(jwtService);
        final AuthenticationManager mockedManager = Mockito.mock(AuthenticationManager.class);
        final JWTAuthenticationFilter filter = new JWTAuthenticationFilter(mockedManager);

        DispatcherServlet servlet = Mockito.mock(DispatcherServlet.class);
        MockFilterChain mockedFilterChain = new MockFilterChain(servlet, publicFilter, filter);

        try {
            mockedFilterChain.doFilter(mockedRequest, mockedResponse);
            // filter.doFilter(mockedFilterChain.getRequest(), mockedFilterChain.getResponse(), mockedFilterChain);
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

        final JWTAuthentication token = new JWTAuthentication(
                jwtService.generateToken("PROJECT", "test@test.test", "USER"));

        final HttpServletRequest mockedRequest = Mockito.mock(HttpServletRequest.class);
        final HttpServletResponse mockedResponse = new MockHttpServletResponse();

        final AuthenticationManager mockedManager = Mockito.mock(AuthenticationManager.class);

        final JWTAuthenticationFilter filter = new JWTAuthenticationFilter(mockedManager);

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

        final JWTAuthentication token = new JWTAuthentication(
                jwtService.generateToken("PROJECT", "test@test.test", "USER"));

        final HttpServletRequest mockedRequest = Mockito.mock(HttpServletRequest.class);
        final HttpServletResponse mockedResponse = new MockHttpServletResponse();

        final AuthenticationManager mockedManager = Mockito.mock(AuthenticationManager.class);

        final JWTAuthenticationFilter filter = new JWTAuthenticationFilter(mockedManager);

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
