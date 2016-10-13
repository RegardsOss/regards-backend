/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.jpa.configuration;

import javax.validation.constraints.NotNull;

/**
 *
 * POJO for microservice common DAO configuration
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
public class CommonDaoConfiguration {

    /**
     * Does the DAO Component is activated ?
     */
    @NotNull
    private Boolean enabled;

    /**
     * Does the datasource has to be created in memory ?
     */
    private Boolean embedded;

    /**
     * Directory path for embedded database persistence
     */
    private String embeddedPath;

    /**
     * Common driver class name to create datasources
     */
    private String driverClassName;

    /**
     * Common hibernate dialect to create datasources
     */
    private String dialect;

    /**
     * Instance DAO Configuration
     */
    private InstanceConfiguration instance;

    /**
     *
     * Getter
     *
     * @return driver class name
     * @since 1.0-SNAPSHOT
     */
    public String getDriverClassName() {
        return driverClassName;
    }

    /**
     *
     * Setter
     *
     * @param pDriverClassName
     *            driver class name
     * @since 1.0-SNAPSHOT
     */
    public void setDriverClassName(String pDriverClassName) {
        driverClassName = pDriverClassName;
    }

    /**
     *
     * Getter
     *
     * @return hibernate dialect
     * @since 1.0-SNAPSHOT
     */
    public String getDialect() {
        return dialect;
    }

    /**
     *
     * Setter
     *
     * @param pDialect
     *            dialect
     * @since 1.0-SNAPSHOT
     */
    public void setDialect(String pDialect) {
        dialect = pDialect;
    }

    /**
     *
     * Getter
     *
     * @return Boolean is the DAO enabled ?
     * @since 1.0-SNAPSHOT
     */
    public Boolean getEnabled() {
        return enabled;
    }

    /**
     *
     * Setter
     *
     * @param pEnabled
     *            is the DAO enabled ?
     * @since 1.0-SNAPSHOT
     */
    public void setEnabled(Boolean pEnabled) {
        enabled = pEnabled;
    }

    /**
     *
     * Getter
     *
     * @return Boolean is the DAO Embedded ?
     * @since 1.0-SNAPSHOT
     */
    public Boolean getEmbedded() {
        return embedded;
    }

    /**
     *
     * Setter
     *
     * @param pEmbedded
     *            is the DAO Embedded ?
     * @since 1.0-SNAPSHOT
     */
    public void setEmbedded(Boolean pEmbedded) {
        embedded = pEmbedded;
    }

    /**
     *
     * Getter
     *
     * @return Instance DAO configuration
     * @since 1.0-SNAPSHOT
     */
    public InstanceConfiguration getInstance() {
        return instance;
    }

    /**
     *
     * Setter
     *
     * @param pInstance
     *            Instance DAO Configuration
     * @since 1.0-SNAPSHOT
     */
    public void setInstance(InstanceConfiguration pInstance) {
        instance = pInstance;
    }

    /**
     *
     * Getter
     *
     * @return Embedded file database path
     * @since 1.0-SNAPSHOT
     */
    public String getEmbeddedPath() {
        return embeddedPath;
    }

    /**
     *
     * Setter
     *
     * @param pEmbeddedPath
     *            Embedded file database path
     * @since 1.0-SNAPSHOT
     */
    public void setEmbeddedPath(String pEmbeddedPath) {
        embeddedPath = pEmbeddedPath;
    }

}
