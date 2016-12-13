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

    /**
     * Class logger
     */
    static final Logger LOG = LoggerFactory.getLogger(JWTServiceTest.class);

    /**
     * JWT service
     */
    @Autowired
    private JWTService jwtService;

    /**
     * Test JWT generation
     */
    @Test
    public void generateJWT() {
        final String project = "PROJECT";
        final String email = "marc.sordi@c-s.fr";
        final String role = "USER";

        // Generate token
        final String jwt = jwtService.generateToken(project, email, role);
        LOG.debug("JWT = " + jwt);

        // Parse token and retrieve user information
        try {
            final JWTAuthentication jwtAuth = jwtService.parseToken(new JWTAuthentication(jwt));

            Assert.assertEquals(project, jwtAuth.getProject());

            final UserDetails user = jwtAuth.getPrincipal();
            Assert.assertEquals(email, user.getName());
            Assert.assertEquals(project, user.getTenant());
        } catch (JwtException e) {
            final String message = "JWT test error";
            LOG.debug(message, e);
            Assert.fail(message);
        }
    }

}
