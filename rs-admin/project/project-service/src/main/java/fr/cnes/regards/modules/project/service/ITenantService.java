/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.project.service;

import java.util.Set;

import fr.cnes.regards.modules.project.dao.IProjectRepository;

/**
 * This interface is used to retrieve all tenants from {@link IProjectRepository}.<br/>
 * The advantage of this service is that it only depends on the single repository and no other bean that may cause a
 * circular dependency.<br/>
 * Thus, a circular dependency may occurs at starting point when a starter or module try to retrieve all tenants to
 * initialize itself.
 *
 * @author Marc Sordi
 *
 */
public interface ITenantService {

    /**
     *
     * @return all tenant managed by the current instance. Tenants are equivalents to projects.
     */
    Set<String> getAllTenants();
}
