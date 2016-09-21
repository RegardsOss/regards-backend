package fr.cnes.regards.microservices.core.configuration.common;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

public class ProjectConfiguration {

    @NestedConfigurationProperty
    private DataSourceProperties datasource;

    private String name;

    public String getName() {
        return name;
    }

    public void setName(String pName) {
        name = pName;
    }

    public DataSourceProperties getDatasource() {
        return datasource;
    }

    public void setDatasource(DataSourceProperties datasource) {
        this.datasource = datasource;
    }

}
