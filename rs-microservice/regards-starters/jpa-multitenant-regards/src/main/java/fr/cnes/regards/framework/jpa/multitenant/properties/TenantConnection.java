/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
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
     * Tenant connection state
     */
    private TenantConnectionState state;

    /**
     * If {@link TenantConnectionState#ERROR}, explains the error cause
     */
    private String errorCause;

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

    public TenantConnectionState getState() {
        return state;
    }

    public void setState(TenantConnectionState state) {
        this.state = state;
    }

    public String getErrorCause() {
        return errorCause;
    }

    public void setErrorCause(String errorCause) {
        this.errorCause = errorCause;
    }

}
