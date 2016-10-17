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

import fr.cnes.regards.framework.security.utils.jwt.JWTAuthentication;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.framework.security.utils.jwt.UserDetails;
import fr.cnes.regards.framework.security.utils.jwt.exception.InvalidJwtException;
import fr.cnes.regards.framework.security.utils.jwt.exception.MissingClaimException;

/**
 * @author msordi
 *
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { JwtTestConfiguration.class })
public class JWTServiceTest {

    @Autowired
    private JWTService jwtService_;

    static final Logger LOG = LoggerFactory.getLogger(JWTServiceTest.class);

    @Test
    public void generateJWT() {
        String project = "PROJECT";
        String email = "marc.sordi@c-s.fr";
        String name = "Marc SORDI";
        String role = "USER";

        // Generate token

        String jwt = jwtService_.generateToken(project, email, name, role);
        LOG.debug("JWT = " + jwt);

        // Parse token and retrieve user information
        try {
            JWTAuthentication jwtAuth = jwtService_.parseToken(new JWTAuthentication(jwt));

            UserDetails user = jwtAuth.getPrincipal();
            Assert.assertEquals(email, user.getEmail());
            Assert.assertEquals(name, user.getName());
            Assert.assertEquals(project, user.getTenant());
        }
        catch (InvalidJwtException | MissingClaimException e) {
            String message = "JWT test error";
            LOG.debug(message, e);
            Assert.fail(message);
        }
    }

}
