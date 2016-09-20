/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.core.dao.hibernate;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

import org.hibernate.engine.jdbc.connections.spi.AbstractDataSourceBasedMultiTenantConnectionProviderImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.stereotype.Component;

import fr.cnes.regards.microservices.core.dao.jpa.MultitenancyProperties;

/**
 *
 * Multitenancy Database Connection Provider. By default only one connection is available. The one defined in the
 * DataSourceConfig class. To add a new connection use the addDataSource method
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
@Component
public class DataSourceBasedMultiTenantConnectionProviderImpl
        extends AbstractDataSourceBasedMultiTenantConnectionProviderImpl {

    private static final long serialVersionUID = 8168907057647334460L;

    private Map<String, DataSource> dataSources;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private MultitenancyProperties multitenancyProperties;

    @PostConstruct
    public void load() {
        dataSources = new HashMap<>();
        // Adding default condigured datasurce
        dataSources.put(multitenancyProperties.getTenant(), dataSource);
    }

    @Override
    protected DataSource selectAnyDataSource() {
        return dataSources.get(multitenancyProperties.getTenant());
    }

    @Override
    protected DataSource selectDataSource(String tenantIdentifier) {
        return dataSources.get(tenantIdentifier);
    }

    public DataSource getDataSource(String pTenant) {
        if (pTenant != null) {
            return selectDataSource(pTenant);
        }
        else {
            return selectAnyDataSource();
        }
    }

    public void addDataSource(String pUrl, String pUser, String pPassword, String pTenant) {

        DataSourceBuilder factory = DataSourceBuilder.create(multitenancyProperties.getDatasource().getClassLoader())
                .driverClassName(multitenancyProperties.getDatasource().getDriverClassName()).username(pUser)
                .password(pPassword).url(pUrl);
        DataSource datasource = factory.build();

        dataSources.put(pTenant, datasource);
    }

}
