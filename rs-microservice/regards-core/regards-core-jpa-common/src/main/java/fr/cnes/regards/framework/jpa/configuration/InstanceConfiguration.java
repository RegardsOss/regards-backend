/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.jpa.configuration;

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
    private Boolean enabled;

    /**
     * Instance JPA Datasource
     */
    @NestedConfigurationProperty
    private DataSourceProperties datasource;

    /**
     *
     * Setter
     *
     * @param pDatasource
     *            instance JPA datasource
     * @since 1.0-SNAPSHOT
     */
    public void setDatasource(DataSourceProperties pDatasource) {
        this.datasource = pDatasource;
    }

    /**
     *
     * Getter
     *
     * @return instance JPA Datasource
     * @since 1.0-SNAPSHOT
     */
    public DataSourceProperties getDatasource() {
        return this.datasource;
    }

    /**
     *
     * Getter
     *
     * @return Is Instance database enabled ?
     * @since 1.0 SNAPSHOT
     */
    public Boolean getEnabled() {
        return enabled;
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
        enabled = pEnabled;
    }

}
