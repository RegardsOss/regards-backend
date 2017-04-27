/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.jpa.multitenant.resolver;

import java.util.Map;

import javax.sql.DataSource;

import org.hibernate.engine.jdbc.connections.spi.AbstractDataSourceBasedMultiTenantConnectionProviderImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.framework.jpa.multitenant.exception.InvalidDataSourceTenant;
import fr.cnes.regards.framework.jpa.multitenant.properties.MultitenantDaoProperties;

/**
 *
 * Multitenancy Database Connection Provider. By default only one connection is available. The one defined in the
 * DataSourceConfig class.
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
@SuppressWarnings("serial")
public class DataSourceBasedMultiTenantConnectionProviderImpl
        extends AbstractDataSourceBasedMultiTenantConnectionProviderImpl {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory
            .getLogger(DataSourceBasedMultiTenantConnectionProviderImpl.class);

    /**
     * Pool of datasources available for this connection provider
     */
    private transient Map<String, DataSource> dataSources;

    public DataSourceBasedMultiTenantConnectionProviderImpl(final Map<String, DataSource> pDataSources) {
        super();
        dataSources = pDataSources;
    }

    public DataSourceBasedMultiTenantConnectionProviderImpl(final MultitenantDaoProperties pDaoProperties) {
        super();
    }

    @Override
    protected DataSource selectAnyDataSource() {
        return dataSources.values().iterator().next();
    }

    @Override
    protected DataSource selectDataSource(final String pTenantIdentifier) {
        DataSource tenantDataSource = dataSources.get(pTenantIdentifier);
        if (tenantDataSource == null) {
            String message = String.format("No data source found for tenant %s.", pTenantIdentifier);
            LOGGER.error(message);
            throw new InvalidDataSourceTenant(message);
        }
        return dataSources.get(pTenantIdentifier);
    }
}
