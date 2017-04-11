/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.jpa.multitenant.utils;

import java.beans.PropertyVetoException;

import javax.sql.DataSource;

import fr.cnes.regards.framework.jpa.multitenant.properties.MultitenantDaoProperties;
import fr.cnes.regards.framework.jpa.multitenant.properties.TenantConnection;
import fr.cnes.regards.framework.jpa.utils.DataSourceHelper;

/**
 *
 * Help to init tenant data sources
 *
 * @author Marc Sordi
 *
 */
public final class TenantDataSourceHelper {

    private TenantDataSourceHelper() {
    }

    /**
     * Init a tenant data source
     *
     * @param pDaoProperties
     *            {@link MultitenantDaoProperties}
     * @param pTenantConnection
     *            tenant connection to create
     * @return a {@link DataSource}
     * @throws PropertyVetoException
     *             if paramater not supported
     */
    public static DataSource initDataSource(MultitenantDaoProperties pDaoProperties, TenantConnection pTenantConnection)
            throws PropertyVetoException {
        DataSource dataSource;
        // Bypass configuration if embedded enabled
        if (pDaoProperties.getEmbedded()) {
            // Create an embedded data source
            dataSource = DataSourceHelper.createEmbeddedDataSource(pTenantConnection.getTenant(),
                                                                   pDaoProperties.getEmbeddedPath());
        } else {
            // Create a pooled data source
            dataSource = DataSourceHelper
                    .createPooledDataSource(pTenantConnection.getTenant(), pTenantConnection.getUrl(),
                                            pTenantConnection.getDriverClassName(), pTenantConnection.getUserName(),
                                            pTenantConnection.getPassword(), pDaoProperties.getMinPoolSize(),
                                            pDaoProperties.getMaxPoolSize(), pDaoProperties.getPreferredTestQuery());
        }
        return dataSource;
    }
}
