/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.core.dao.jpa;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 *
 * Properties read from the Spring boot properties used for the database multitenancy configuration
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
@ConfigurationProperties("spring.multitenancy")
public class MultitenancyProperties {

    @NestedConfigurationProperty
    private DataSourceProperties datasource;

    @NestedConfigurationProperty
    private String tenant;

    public DataSourceProperties getDatasource() {
        return datasource;
    }

    public void setDatasource(DataSourceProperties datasource) {
        this.datasource = datasource;
    }

    public void setTenant(String pTenant) {
        this.tenant = pTenant;
    }

    public String getTenant() {
        return this.tenant;
    }

}