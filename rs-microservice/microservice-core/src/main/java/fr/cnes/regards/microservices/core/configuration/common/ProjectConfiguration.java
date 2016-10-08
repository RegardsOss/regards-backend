/*
 * LICENSE_PLACEHOLDER
 */
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
    private DataSourceProperties datasource;

    /**
     * Project name
     */
    private String name;

    /**
     *
     * Getter
     *
     * @return project name
     * @since 1.0-SNAPSHOT
     */
    public String getName() {
        return name;
    }

    /**
     *
     * Setter
     *
     * @param pName
     *            project name
     * @since 1.0-SNAPSHOT
     */
    public void setName(String pName) {
        name = pName;
    }

    /**
     *
     * Getter
     *
     * @return project JPA datasource
     * @since 1.0-SNAPSHOT
     */
    public DataSourceProperties getDatasource() {
        return datasource;
    }

    /**
     *
     * Setter
     *
     * @param pDatasource
     *            project JPA datasource
     * @since 1.0-SNAPSHOT
     */
    public void setDatasource(DataSourceProperties pDatasource) {
        this.datasource = pDatasource;
    }

}
