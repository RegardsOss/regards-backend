package fr.cnes.regards.microservices.core.configuration.common;

import javax.validation.constraints.NotNull;

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
     * Common driver class name to create datasources
     */
    private String driverClassName_;

    /**
     * Common hibernate dialect to create datasources
     */
    private String dialect_;

    public String getDriverClassName() {
        return driverClassName_;
    }

    public void setDriverClassName(String pDriverClassName) {
        driverClassName_ = pDriverClassName;
    }

    public String getDialect() {
        return dialect_;
    }

    public void setDialect(String pDialect) {
        dialect_ = pDialect;
    }

    public Boolean getEnabled() {
        return enabled_;
    }

    public void setEnabled(Boolean pEnabled) {
        enabled_ = pEnabled;
    }

    public Boolean getEmbedded() {
        return embedded_;
    }

    public void setEmbedded(Boolean pEmbedded) {
        embedded_ = pEmbedded;
    }

}
