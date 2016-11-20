/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.jpa.multitenant.event.handler;

import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.jpa.multitenant.event.NewTenantEvent;
import fr.cnes.regards.framework.jpa.multitenant.properties.TenantConnection;
import fr.cnes.regards.framework.jpa.multitenant.resolver.DataSourceBasedMultiTenantConnectionProviderImpl;

/**
 *
 * Class NewTenantHandler
 *
 * Action when a NewTenantEvt is received
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
public class NewTenantHandler implements IHandler<NewTenantEvent> {

    /**
     * JPA Multitenant connection provider
     */
    private final DataSourceBasedMultiTenantConnectionProviderImpl multitenantConnectionProvider;

    /**
     * Current microservice name
     */
    private final String currentMicroservice;

    public NewTenantHandler(final DataSourceBasedMultiTenantConnectionProviderImpl pMultitenantConnectionProvider,
            final String pCurrentMicroservice) {
        super();
        multitenantConnectionProvider = pMultitenantConnectionProvider;
        currentMicroservice = pCurrentMicroservice;
    }

    /**
     *
     * Create a new DataSource and add it to the JPA Multitenant connection provider
     * 
     * @see fr.cnes.regards.framework.amqp.domain.IHandler#handle(fr.cnes.regards.framework.amqp.domain.TenantWrapper)
     * @since 1.0-SNAPSHOT
     */
    @Override
    public void handle(final TenantWrapper<NewTenantEvent> pNewTenant) {
        // Add a new datasource to the current pool of datasource if the current microservice is the target of the new
        // tenant connection
        if ((pNewTenant.getContent() != null)
                && currentMicroservice.equals(pNewTenant.getContent().getMicroserviceName())) {
            final TenantConnection tenantConn = pNewTenant.getContent().getTenant();
            multitenantConnectionProvider.addDataSource(tenantConn.getUrl(), tenantConn.getUserName(),
                                                        tenantConn.getPassword(), tenantConn.getDriverClassName(),
                                                        tenantConn.getName());
        }
    }

}
