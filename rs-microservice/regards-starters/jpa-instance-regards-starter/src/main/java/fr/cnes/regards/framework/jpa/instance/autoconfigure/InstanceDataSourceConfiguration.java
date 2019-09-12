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
package fr.cnes.regards.framework.jpa.instance.autoconfigure;

import java.beans.PropertyVetoException;
import java.io.IOException;

import javax.sql.DataSource;

import org.hibernate.cfg.Environment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import fr.cnes.regards.framework.jpa.instance.properties.InstanceDaoProperties;
import fr.cnes.regards.framework.jpa.utils.DataSourceHelper;

/**
 * Class InstanceDataSourceConfiguration
 *
 * JPA Properties for instance datasource
 * @author CS
 */
@Configuration
@EnableConfigurationProperties(InstanceDaoProperties.class)
@ConditionalOnProperty(prefix = "regards.jpa", name = "instance.enabled", matchIfMissing = true)
public class InstanceDataSourceConfiguration {

    /**
     * Microservice global configuration
     */
    @Autowired
    private InstanceDaoProperties daoProperties;

    /**
     * JPA Configuration
     */
    @Autowired
    private JpaProperties jpaProperties;

    /**
     * Default data source for persistence unit instance.
     */
    @Bean
    @Primary
    public DataSource instanceDataSource() throws PropertyVetoException, IOException {

        String tenant = "instance";
        DataSource datasource;
        if (daoProperties.getEmbedded()) {
            datasource = DataSourceHelper.createEmbeddedDataSource(tenant, daoProperties.getEmbeddedPath());

        } else {
            // Retrieve schema name
            String schemaIdentifier = jpaProperties.getProperties().get(Environment.DEFAULT_SCHEMA);
            // this datasource does not need to be encrypted because it doesn't live in any database,
            // just into the configuration file which is not encrypted but accesses are restricted.
            datasource = DataSourceHelper
                    .createHikariDataSource(tenant, daoProperties.getDatasource().getUrl(),
                                            daoProperties.getDatasource().getDriverClassName(),
                                            daoProperties.getDatasource().getUsername(),
                                            daoProperties.getDatasource().getPassword(), daoProperties.getMinPoolSize(),
                                            daoProperties.getMaxPoolSize(), daoProperties.getPreferredTestQuery(), schemaIdentifier);
        }
        return datasource;
    }

}
