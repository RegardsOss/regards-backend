/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.security.filter;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.InsufficientAuthenticationException;

import fr.cnes.regards.framework.security.utils.jwt.JWTAuthentication;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.framework.security.utils.jwt.exception.JwtException;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;

/**
 *
 * Class JWTAuthenticationProviderTest
 *
 * Test class for JWT Filter
 *
 * @author sbinda
 * @since 1.0-SNAPSHOT
 */
public class JWTAuthenticationProviderTest {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(JWTAuthenticationProviderTest.class);

    /**
     *
     * "Check security filter with Jwt access token
     *
     * @since 1.0-SNAPSHOT
     */
    @Requirement("REGARDS_DSL_SYS_SEC_100")
    @Purpose("Check security filter with Jwt access token")
    @Test
    public void test() {

        final JWTAuthentication jwtAuthentication = new JWTAuthentication("token");

        final JWTService mockedJWTService = Mockito.mock(JWTService.class);

        final JWTAuthenticationProvider provider = new JWTAuthenticationProvider(mockedJWTService);

        try {
            Mockito.when(mockedJWTService.parseToken(jwtAuthentication)).thenReturn(jwtAuthentication);
        } catch (final JwtException e) {
            LOG.error(e.getMessage(), e);
            Assert.fail(e.getMessage());
        }

        try {
            provider.authenticate(jwtAuthentication);
        } catch (final InsufficientAuthenticationException e) {
            LOG.error(e.getMessage(), e);
            Assert.fail(e.getMessage());
        }

        try {
            Mockito.when(mockedJWTService.parseToken(jwtAuthentication)).thenThrow(new JwtException("JWT parse error"));
        } catch (final JwtException e) {
            LOG.error(e.getMessage(), e);
            Assert.fail(e.getMessage());
        }

        try {
            provider.authenticate(jwtAuthentication);
            Assert.fail("There should be an error");
        } catch (final InsufficientAuthenticationException e) {
            LOG.info(e.getMessage());
        }
    }
}
