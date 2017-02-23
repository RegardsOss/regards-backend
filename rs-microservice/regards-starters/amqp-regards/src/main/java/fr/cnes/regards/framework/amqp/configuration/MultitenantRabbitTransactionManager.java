/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.amqp.configuration;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.transaction.RabbitTransactionManager;
import org.springframework.transaction.TransactionDefinition;

import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;

/**
 *
 * Override {@link RabbitTransactionManager} to manage virtual host in transaction
 *
 * @author Marc Sordi
 *
 */
@SuppressWarnings("serial")
public class MultitenantRabbitTransactionManager extends RabbitTransactionManager {

    /**
     * Resolve current tenant at runtime. The resolver must be thread safe.
     */
    private final IRuntimeTenantResolver runtimeTenantResolver;

    /**
     * Virtual host admin
     */
    private final IRabbitVirtualHostAdmin rabbitVirtualHostAdmin;

    public MultitenantRabbitTransactionManager(ConnectionFactory pConnectionFactory,
            IRuntimeTenantResolver pRuntimeTenantResolver, IRabbitVirtualHostAdmin pRabbitVirtualHostAdmin) {
        super(pConnectionFactory);
        this.runtimeTenantResolver = pRuntimeTenantResolver;
        this.rabbitVirtualHostAdmin = pRabbitVirtualHostAdmin;
    }

    @Override
    protected void doBegin(Object pTransaction, TransactionDefinition pDefinition) {
        try {
            rabbitVirtualHostAdmin.bind(runtimeTenantResolver.getTenant());
            super.doBegin(pTransaction, pDefinition);
        } finally {
            rabbitVirtualHostAdmin.unbind();
        }
    }
}
