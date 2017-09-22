package fr.cnes.regards.framework.test.integration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.transaction.BeforeTransaction;

import fr.cnes.regards.framework.jpa.utils.RegardsTransactional;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@RegardsTransactional
public abstract class AbstractRegarsServiceTransactionalIT  extends AbstractRegardsServiceIT{

    @Autowired
    protected IRuntimeTenantResolver runtimeTenantResolver;

    @BeforeTransaction
    public void beforeTransaction() {
        runtimeTenantResolver.forceTenant(DEFAULT_TENANT);
    }

}
