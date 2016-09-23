/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.core.configuration.common;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

public class InstanceConfiguration {

    private Boolean enabled_;

    @NestedConfigurationProperty
    private DataSourceProperties datasource_;

    public void setDatasource(DataSourceProperties datasource) {
        this.datasource_ = datasource;
    }

    public DataSourceProperties getDatasource() {
        return this.datasource_;
    }

    public Boolean getEnabled() {
        return enabled_;
    }

    public void setEnabled(Boolean pEnabled) {
        enabled_ = pEnabled;
    }

}
