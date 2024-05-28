/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.jpa.multitenant.resolver;

import fr.cnes.regards.framework.jpa.multitenant.exception.InvalidDataSourceTenant;
import fr.cnes.regards.framework.jpa.multitenant.properties.MultitenantDaoProperties;
import org.hibernate.engine.jdbc.connections.spi.AbstractDataSourceBasedMultiTenantConnectionProviderImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.util.Map;

/**
 * Multitenancy Database Connection Provider. By default only one connection is available. The one defined in the
 * DataSourceConfig class.
 *
 * @author CS
 */

public class DataSourceBasedMultiTenantConnectionProviderImpl
    extends AbstractDataSourceBasedMultiTenantConnectionProviderImpl {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(DataSourceBasedMultiTenantConnectionProviderImpl.class);

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
    protected DataSource selectDataSource(final String tenantIdentifier) {
        DataSource tenantDataSource = dataSources.get(tenantIdentifier);
        if (tenantDataSource == null) {
            throw new InvalidDataSourceTenant(String.format("""
                                                                No data source found for tenant %s.
                                                                Actual defined datasources : %s.
                                                                If wanted tenant is 'default' and at least another one has been defined, a transactional method may have been executed before multitenancy is set.
                                                                """, tenantIdentifier, dataSources.keySet()));
        }
        return tenantDataSource;
    }
}
