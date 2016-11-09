/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.multitenant.autoconfigure.tenant;

import java.util.Set;
import java.util.TreeSet;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;

/**
 * Resolve tenant base on configuration properties
 *
 * @author msordi
 *
 */
public class LocalTenantResolver implements ITenantResolver {

    /**
     * List of configurated tenants
     */
    @Value("${regards.tenants:#{null}}")
    private String[] localTenants;

    /**
     * List of unique tenants
     */
    private Set<String> tenants;

    @PostConstruct
    public void init() {
        tenants = new TreeSet<>();
        if (localTenants != null) {
            for (String tenant : localTenants) {
                tenants.add(tenant);
            }
        }
    }

    @Override
    public Set<String> getAllTenants() {
        return tenants;
    }

}
