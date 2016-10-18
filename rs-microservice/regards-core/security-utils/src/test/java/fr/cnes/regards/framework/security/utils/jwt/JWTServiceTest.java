/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.security.utils.jwt;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import fr.cnes.regards.framework.security.utils.jwt.exception.InvalidJwtException;
import fr.cnes.regards.framework.security.utils.jwt.exception.MissingClaimException;

/**
 * @author msordi
 *
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { JwtTestConfiguration.class })
public class JWTServiceTest {

    /**
     * Class logger
     */
    static final Logger LOG = LoggerFactory.getLogger(JWTServiceTest.class);

    /**
     * JWT Secret
     */
    @Value("jwt.secret")
    private String secret;

    /**
     * Test JWT generation
     */
    @Test
    public void generateJWT() {
        final String project = "PROJECT";
        final String email = "marc.sordi@c-s.fr";
        final String name = "Marc SORDI";
        final String role = "USER";

        // Init JWT service
        final JWTService jwtService = new JWTService(secret);

        // Generate token
        final String jwt = jwtService.generateToken(project, email, name, role);
        LOG.debug("JWT = " + jwt);

        // Parse token and retrieve user information
        try {
            final JWTAuthentication jwtAuth = jwtService.parseToken(new JWTAuthentication(jwt));

            Assert.assertEquals(project, jwtAuth.getProject());

            final UserDetails user = jwtAuth.getPrincipal();
            Assert.assertEquals(email, user.getEmail());
            Assert.assertEquals(name, user.getName());
        } catch (InvalidJwtException | MissingClaimException e) {
            final String message = "JWT test error";
            LOG.debug(message, e);
            Assert.fail(message);
        }
    }

}
