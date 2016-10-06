/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.core.configuration.common;

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
    private Boolean enabled_;

    /**
     * Does the datasource has to be created in memory ?
     */
    private Boolean embedded_;

    /**
     * Directory path for embedded database persistence
     */
    private String embeddedPath_;

    /**
     * Common driver class name to create datasources
     */
    private String driverClassName_;

    /**
     * Common hibernate dialect to create datasources
     */
    private String dialect_;

    /**
     * Instance DAO Configuration
     */
    private InstanceConfiguration instance_;

    /**
     *
     * Getter
     *
     * @return driver class name
     * @since 1.0-SNAPSHOT
     */
    public String getDriverClassName() {
        return driverClassName_;
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
        driverClassName_ = pDriverClassName;
    }

    /**
     *
     * Getter
     *
     * @return hibernate dialect
     * @since 1.0-SNAPSHOT
     */
    public String getDialect() {
        return dialect_;
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
        dialect_ = pDialect;
    }

    /**
     *
     * Getter
     *
     * @return Boolean is the DAO enabled ?
     * @since 1.0-SNAPSHOT
     */
    public Boolean getEnabled() {
        return enabled_;
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
        enabled_ = pEnabled;
    }

    /**
     *
     * Getter
     *
     * @return Boolean is the DAO Embedded ?
     * @since 1.0-SNAPSHOT
     */
    public Boolean getEmbedded() {
        return embedded_;
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
        embedded_ = pEmbedded;
    }

    /**
     *
     * Getter
     *
     * @return Instance DAO configuration
     * @since 1.0-SNAPSHOT
     */
    public InstanceConfiguration getInstance() {
        return instance_;
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
        instance_ = pInstance;
    }

    /**
     *
     * Getter
     *
     * @return Embedded file database path
     * @since 1.0-SNAPSHOT
     */
    public String getEmbeddedPath() {
        return embeddedPath_;
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
        embeddedPath_ = pEmbeddedPath;
    }

}
