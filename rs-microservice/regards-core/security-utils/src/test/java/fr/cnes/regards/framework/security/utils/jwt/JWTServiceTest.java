/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.security.utils.jwt;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import fr.cnes.regards.framework.security.utils.jwt.exception.JwtException;

/**
 * @author msordi
 *
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { JwtTestConfiguration.class })
public class JWTServiceTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(JWTServiceTest.class);

    private static final String TENANT = "tenant";

    private static final String EMAIL = "marc.sordi@c-s.fr";

    private static final String ROLE = "USER";

    /**
     * JWT service
     */
    @Autowired
    private JWTService jwtService;

    /**
     * Test JWT generation without group
     */
    @Test
    public void generateJWT() {

        // Generate token
        final String jwt = jwtService.generateToken(TENANT, EMAIL, ROLE);
        LOGGER.debug(jwt);

        // Parse token and retrieve user information
        try {
            final JWTAuthentication jwtAuth = jwtService.parseToken(new JWTAuthentication(jwt));

            Assert.assertEquals(TENANT, jwtAuth.getTenant());

            final UserDetails user = jwtAuth.getPrincipal();
            Assert.assertEquals(EMAIL, user.getName());
            Assert.assertEquals(ROLE, user.getRole());
        } catch (JwtException e) {
            final String message = "Error while generating JWT without group";
            LOGGER.debug(message, e);
            Assert.fail(message);
        }
    }
}
