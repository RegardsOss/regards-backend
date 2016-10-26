/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.security.filter;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.InsufficientAuthenticationException;

import fr.cnes.regards.framework.security.domain.HttpConstants;
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
public class JWTAuthenticationFilterTest {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(JWTAuthenticationFilterTest.class);

    /**
     *
     * "Check security filter with Jwt access token
     *
     * @since 1.0-SNAPSHOT
     */
    @Requirement("REGARDS_DSL_SYS_SEC_100")
    @Purpose("Check security filter with Jwt access token")
    @Test
    public void jwtFilterTest() {

        final JWTAuthentication token = new JWTAuthentication("token");
        token.setRole("USER");

        final HttpServletRequest mockedRequest = Mockito.mock(HttpServletRequest.class);
        final HttpServletResponse mockedResponse = Mockito.mock(HttpServletResponse.class);

        final AuthenticationManager mockedManager = Mockito.mock(AuthenticationManager.class);

        final JWTAuthenticationFilter filter = new JWTAuthenticationFilter(mockedManager);

        try {
            filter.doFilter(mockedRequest, mockedResponse, new MockFilterChain());
            Assert.fail("Authentication should fail.");
        } catch (final InsufficientAuthenticationException e) {
            // Nothing to do
            LOG.info(e.getMessage());
        } catch (IOException | ServletException e) {
            LOG.error(e.getMessage(), e);
            Assert.fail(e.getMessage());
        }

        Mockito.when(mockedRequest.getHeader(HttpConstants.AUTHORIZATION)).thenReturn(token.getJwt());

        try {
            filter.doFilter(mockedRequest, mockedResponse, new MockFilterChain());
            Assert.fail("Authentication shoudd fail.");
        } catch (final InsufficientAuthenticationException e) {
            // Nothing to do
            LOG.info(e.getMessage());
        } catch (IOException | ServletException e) {
            LOG.error(e.getMessage(), e);
            Assert.fail(e.getMessage());
        }

        Mockito.when(mockedRequest.getHeader(HttpConstants.AUTHORIZATION))
                .thenReturn(String.format("%s: %s", HttpConstants.BEARER, token.getJwt()));

        try {
            filter.doFilter(mockedRequest, mockedResponse, new MockFilterChain());
        } catch (IOException | ServletException e) {
            LOG.error(e.getMessage(), e);
            Assert.fail(e.getMessage());
        }

    }

}
