package fr.cnes.regards.microservices.core.configuration.common;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 *
 * POJO for microservice project configuration
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
public class ProjectConfiguration {

    /**
     * Project datasource
     */
    @NestedConfigurationProperty
    private DataSourceProperties datasource_;

    /**
     * Project name
     */
    private String name_;

    public String getName() {
        return name_;
    }

    public void setName(String pName) {
        name_ = pName;
    }

    public DataSourceProperties getDatasource() {
        return datasource_;
    }

    public void setDatasource(DataSourceProperties datasource) {
        this.datasource_ = datasource;
    }

}
