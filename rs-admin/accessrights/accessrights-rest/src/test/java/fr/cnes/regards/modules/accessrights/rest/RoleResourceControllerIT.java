/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.rest;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.web.bind.annotation.RequestMethod;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.modules.accessrights.dao.projects.IResourcesAccessRepository;
import fr.cnes.regards.modules.accessrights.dao.projects.IRoleRepository;
import fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;

/**
 * @author Marc Sordi
 *
 */
@MultitenantTransactional
public class RoleResourceControllerIT extends AbstractRegardsTransactionalIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(RoleResourceControllerIT.class);

    @Autowired
    private IRoleRepository roleRepository;

    @Autowired
    private IResourcesAccessRepository resourcesAccessRepository;

    /**
     * Get all role resources
     */
    @Test
    public void getRoleResourcesTest() {
    }

    /**
     * Add new resource access to a role
     */
    @Test
    public void addRoleResourceTest() {
    }

    /**
     * Delete resource access from a role
     */
    @Test
    @Purpose("Check that the system allows to remove a resourceAccess permission to a given role.")
    public void deleteRoleResourceTest() {

        // Create a new resource
        ResourcesAccess resource = new ResourcesAccess(null, "microservice", "/to/delete", "delController",
                RequestMethod.GET, DefaultRole.ADMIN);
        resourcesAccessRepository.save(resource);

        // Add to admin group
        Role adminRole = roleRepository.findOneByName(DefaultRole.ADMIN.toString()).get();
        adminRole.addPermission(resource);
        roleRepository.save(adminRole);

        // Remove resource access
        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(MockMvcResultMatchers.status().isNoContent());
        performDefaultDelete(RoleResourceController.TYPE_MAPPING + RoleResourceController.SINGLE_RESOURCE_MAPPING,
                             expectations, "Error retrieving resourcesAccess for user.", adminRole.getName(),
                             resource.getId());
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }
}
