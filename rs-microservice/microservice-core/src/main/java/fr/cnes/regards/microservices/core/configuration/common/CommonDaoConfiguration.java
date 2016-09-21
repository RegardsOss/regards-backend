package fr.cnes.regards.microservices.core.configuration.common;

public class CommonDaoConfiguration {

    /**
     * Does the DAO Component is activated ?
     */
    private Boolean enabled;

    /**
     * Does the datasource has to be created in memory ?
     */
    private Boolean embedded;

    /**
     * Common driver class name to create datasources
     */
    private String driverClassName;

    /**
     * Common hibernate dialect to create datasources
     */
    private String dialect;

    public String getDriverClassName() {
        return driverClassName;
    }

    public void setDriverClassName(String pDriverClassName) {
        driverClassName = pDriverClassName;
    }

    public String getDialect() {
        return dialect;
    }

    public void setDialect(String pDialect) {
        dialect = pDialect;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean pEnabled) {
        enabled = pEnabled;
    }

    public Boolean getEmbedded() {
        return embedded;
    }

    public void setEmbedded(Boolean pEmbedded) {
        embedded = pEmbedded;
    }

}
