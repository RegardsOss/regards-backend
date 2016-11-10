/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.jpa.multitenant.test;

import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import fr.cnes.regards.framework.security.utils.jwt.JWTService;

/**
 * Default configuration test
 *
 * @author Marc Sordi
 *
 */
@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { DefaultTestConfiguration.class })
public abstract class AbstractDaoTest {

    /**
     * Default tenant configured in dao.properties
     */
    private static final String DEFAULT_TENANT = "PROJECT";

    /**
     * Default role
     */
    private static final String DEFAULT_ROLE = "ROLE_USER";

    /**
     * JWT service
     */
    @Autowired
    private JWTService jwtService;

    protected void injectDefaultToken() {
        jwtService.injectMockToken(DEFAULT_TENANT, DEFAULT_ROLE);
    }

    protected void injectToken(String pTenant) {
        jwtService.injectMockToken(pTenant, DEFAULT_ROLE);
    }

}
