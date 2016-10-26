/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.hateoas;

import java.util.ArrayList;
import java.util.List;

import org.junit.Ignore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.web.bind.annotation.RequestMethod;

import fr.cnes.regards.framework.test.integration.AbstractRegardsIT;

/**
 *
 * Resource service test
 *
 * @author msordi
 *
 */
@Ignore
public class PojoControllerIT extends AbstractRegardsIT {

    /**
     * Logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(PojoControllerIT.class);

    /**
     * Get all pojos
     */
    public void testGetPojos() {

        // Manage security context
        final String role = "USER";
        final String endpoint = "/pojos";
        final String tenant = "PROJECT";

        final String jwt = jwtService.generateToken(tenant, "email", "MSI", role);
        authService.setAuthorities(tenant, endpoint, RequestMethod.GET, role);

        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(MockMvcResultMatchers.status().isOk());

        performGet(endpoint, jwt, expectations, "Cannot get all pojos");
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }
}
