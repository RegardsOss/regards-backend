/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.jpa.multitenant.test;

import org.springframework.test.context.transaction.BeforeTransaction;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;

/**
 * Manage security for transactional test
 *
 * @author Marc Sordi
 *
 */
@MultitenantTransactional
public abstract class AbstractDaoTransactionalTest extends AbstractDaoTest {

    @BeforeTransaction
    public void beforeTransaction() {
        injectDefaultToken();
    }
}
