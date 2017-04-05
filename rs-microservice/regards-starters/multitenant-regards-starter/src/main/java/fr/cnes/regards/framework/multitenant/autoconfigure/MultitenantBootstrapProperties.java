/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.multitenant.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Allows to configured a set of tenant to initialize at bootstrap during bean construction.
 *
 * @author Marc Sordi
 *
 */
@ConfigurationProperties(prefix = "regards")
public class MultitenantBootstrapProperties {

    /**
     * List of static tenant to manage at bootstrap
     */
    private String[] bootstrapTenants;

    public String[] getBootstrapTenants() {
        return bootstrapTenants;
    }

    public void setBootstrapTenants(String[] pBootstrapTenants) {
        bootstrapTenants = pBootstrapTenants;
    }
}
