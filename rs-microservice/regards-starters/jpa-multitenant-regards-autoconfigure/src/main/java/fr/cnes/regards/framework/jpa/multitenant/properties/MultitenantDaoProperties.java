/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.jpa.multitenant.properties;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 *
 * POJO for microservice configuration
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
@ConfigurationProperties("regards.jpa.multitenant")
public class MultitenantDaoProperties {

    /**
     * Does the dao is enabled ?
     */
    private Boolean enabled = Boolean.TRUE;

    /**
     * Projects configurations
     */
    private List<TenantConnection> tenants = new ArrayList<>();

    /**
     * Does the Multitenant dao is embedded ?
     */
    private Boolean embedded = Boolean.FALSE;

    /**
     * Path for embedded databases
     */
    private String embeddedPath;

    /**
     * Global hibernate dialect for all tenants
     */
    private String dialect;

    public List<TenantConnection> getTenants() {
        return tenants;
    }

    public void setTenants(final List<TenantConnection> pPTenants) {
        tenants = pPTenants;
    }

    public Boolean getEmbedded() {
        return embedded;
    }

    public void setEmbedded(final Boolean pEmbedded) {
        embedded = pEmbedded;
    }

    public String getDialect() {
        return dialect;
    }

    public void setDialect(final String pDriverClassName) {
        dialect = pDriverClassName;
    }

    public String getEmbeddedPath() {
        return embeddedPath;
    }

    public void setEmbeddedPath(final String pEmbeddedPath) {
        embeddedPath = pEmbeddedPath;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(final Boolean pEnabled) {
        enabled = pEnabled;
    }

}
