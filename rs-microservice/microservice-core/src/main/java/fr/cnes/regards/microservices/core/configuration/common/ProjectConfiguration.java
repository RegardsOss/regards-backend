package fr.cnes.regards.microservices.core.configuration.common;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

public class ProjectConfiguration {

    @NestedConfigurationProperty
    private DataSourceProperties datasource_;

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
