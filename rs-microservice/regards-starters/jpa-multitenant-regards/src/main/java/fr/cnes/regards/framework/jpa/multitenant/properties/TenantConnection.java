/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.jpa.multitenant.properties;

import javax.validation.constraints.NotNull;

/**
 *
 * POJO for microservice project configuration
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
public class TenantConnection {

    /**
     * Tenant name
     */
    @NotNull
    private String tenant;

    /**
     * Tenant datasource url
     */
    private String url;

    /**
     * Tenant datasource username
     */
    private String userName;

    /**
     * Tenant datasource password
     */
    private String password;

    /**
     * Tenant datasource driverClassName
     */
    private String driverClassName = "org.postgresql.Driver";

    /**
     *
     * Constructor
     *
     * @since 1.0-SNAPSHOT
     */
    public TenantConnection() {
        super();
    }

    /**
     *
     * Constructor
     *
     * @param pName
     *            tenant name
     * @param pUrl
     *            tenant datasource url
     * @param pUserName
     *            tenant datasource username
     * @param pPassword
     *            tenant datasource password
     * @param pDriverClassName
     *            tenant datasource driver class name
     * @since 1.0-SNAPSHOT
     */
    public TenantConnection(final String pName, final String pUrl, final String pUserName, final String pPassword,
            final String pDriverClassName) {
        super();
        tenant = pName;
        url = pUrl;
        userName = pUserName;
        password = pPassword;
        driverClassName = pDriverClassName;
    }

    public String getTenant() {
        return tenant;
    }

    public void setTenant(final String pName) {
        tenant = pName;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(final String pUrl) {
        url = pUrl;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(final String pUserName) {
        userName = pUserName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(final String pPassword) {
        password = pPassword;
    }

    public String getDriverClassName() {
        return driverClassName;
    }

    public void setDriverClassName(final String pDriverClassName) {
        driverClassName = pDriverClassName;
    }

}
