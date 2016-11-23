/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.test.integration;

import org.springframework.test.context.transaction.BeforeTransaction;

/**
 * Add default transactional initialization
 *
 * @author Marc Sordi
 *
 */
public abstract class AbstractRegardsTransactionalIT extends AbstractRegardsIT {

    @BeforeTransaction
    protected void beforeTransaction() {
        injectToken(DEFAULT_TENANT, DEFAULT_ROLE);
    }

    /**
     * Inject token in the security context.<br>
     * Override this method to manage your tenant and role in transaction
     *
     * @param pTenant
     *            tenant
     * @param pRole
     *            role
     */
    protected void injectToken(String pTenant, String pRole) {
        jwtService.injectMockToken(pTenant, pRole);
    }

}
