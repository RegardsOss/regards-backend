/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.framework.jpa.multitenant.utils;

import java.beans.PropertyVetoException;
import java.io.IOException;
import java.sql.SQLException;

import javax.sql.DataSource;

import fr.cnes.regards.framework.jpa.multitenant.properties.MultitenantDaoProperties;
import fr.cnes.regards.framework.jpa.multitenant.properties.TenantConnection;
import fr.cnes.regards.framework.jpa.utils.DataSourceHelper;

/**
 * Help to init tenant data sources
 * @author Marc Sordi
 */
public final class TenantDataSourceHelper {


    private TenantDataSourceHelper() {
    }

    /**
     * Init a tenant data source
     * @param pDaoProperties {@link MultitenantDaoProperties}
     * @param pTenantConnection tenant connection to create
     * @param schemaIdentifier
     * @return a {@link DataSource}
     * @throws PropertyVetoException if parameter not supported
     * @throws SQLException          if connection fails
     */
    public static DataSource initDataSource(MultitenantDaoProperties pDaoProperties, TenantConnection pTenantConnection,
            String schemaIdentifier) throws PropertyVetoException, SQLException, IOException {
        DataSource dataSource;
        // Bypass configuration if embedded enabled
        if (pDaoProperties.getEmbedded()) {
            // Create an embedded data source
            dataSource = DataSourceHelper.createEmbeddedDataSource(pTenantConnection.getTenant(),
                                                                   pDaoProperties.getEmbeddedPath());
        } else {
            // Create a pooled data source
            dataSource = DataSourceHelper
                    .createHikariDataSource(pTenantConnection.getTenant(), pTenantConnection.getUrl(),
                                            pTenantConnection.getDriverClassName(), pTenantConnection.getUserName(),
                                            pTenantConnection.getPassword(), pDaoProperties.getMinPoolSize(),
                                            pDaoProperties.getMaxPoolSize(), pDaoProperties.getPreferredTestQuery(),
                                            schemaIdentifier);

            // Test connection for pooled datasource
            DataSourceHelper.testConnection(dataSource, true);
        }
        return dataSource;
    }
}
