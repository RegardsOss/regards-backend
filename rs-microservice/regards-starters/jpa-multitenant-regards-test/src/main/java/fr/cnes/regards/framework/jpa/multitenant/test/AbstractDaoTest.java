/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.jpa.multitenant.test;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;

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
     * JPA entity manager : use it to flush context to prevent false positive
     */
    @PersistenceContext
    protected EntityManager entityManager;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    protected void injectDefaultToken() {
        runtimeTenantResolver.forceTenant(DEFAULT_TENANT);
    }

    protected void injectToken(String pTenant) {
        runtimeTenantResolver.forceTenant(pTenant);
    }
}
