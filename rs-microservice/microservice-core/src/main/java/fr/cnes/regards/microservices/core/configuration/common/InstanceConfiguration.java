/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.core.configuration.common;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 *
 * Class InstanceConfiguration
 *
 * DAO Instance database configuration
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
public class InstanceConfiguration {

    /**
     * Is Instance database enabled ?
     */
    private Boolean enabled_;

    /**
     * Instance JPA Datasource
     */
    @NestedConfigurationProperty
    private DataSourceProperties datasource_;

    /**
     *
     * Setter
     *
     * @param datasource
     *            instance JPA datasource
     * @since 1.0-SNAPSHOT
     */
    public void setDatasource(DataSourceProperties datasource) {
        this.datasource_ = datasource;
    }

    /**
     *
     * Getter
     *
     * @return instance JPA Datasource
     * @since 1.0-SNAPSHOT
     */
    public DataSourceProperties getDatasource() {
        return this.datasource_;
    }

    /**
     *
     * Getter
     *
     * @return Is Instance database enabled ?
     * @since 1.0 SNAPSHOT
     */
    public Boolean getEnabled() {
        return enabled_;
    }

    /**
     *
     * Setter
     *
     * @param pEnabled
     *            Is Instance database enabled ?
     * @since 1.0 SNAPSHOT
     */
    public void setEnabled(Boolean pEnabled) {
        enabled_ = pEnabled;
    }

}
