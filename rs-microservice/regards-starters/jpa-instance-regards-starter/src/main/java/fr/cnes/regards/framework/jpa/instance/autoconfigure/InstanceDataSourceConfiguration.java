/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.jpa.instance.autoconfigure;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import fr.cnes.regards.framework.jpa.instance.properties.InstanceDaoProperties;
import fr.cnes.regards.framework.jpa.utils.DataSourceHelper;

/**
 *
 * Class InstanceDataSourceConfiguration
 *
 * JPA Properties for instance datasource
 *
 * @author CS
 * @since 1.0-SNAPSHOT
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
     *
     * Default data source for persistence unit instance.
     *
     * @return datasource
     * @since 1.0-SNAPSHOT
     */
    @Bean
    @Primary
    public DataSource instanceDataSource() {

        DataSource datasource;
        if (daoProperties.getEmbedded()) {
            datasource = DataSourceHelper.createEmbeddedDataSource("instance", daoProperties.getEmbeddedPath());

        } else {
            datasource = DataSourceHelper.createDataSource(daoProperties.getDatasource().getUrl(),
                                                           daoProperties.getDatasource().getDriverClassName(),
                                                           daoProperties.getDatasource().getUsername(),
                                                           daoProperties.getDatasource().getPassword());
        }
        return datasource;
    }

}
