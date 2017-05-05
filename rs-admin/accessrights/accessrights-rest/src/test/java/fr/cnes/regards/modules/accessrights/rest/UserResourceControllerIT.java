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

import com.google.common.collect.Lists;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.accessrights.dao.projects.IProjectUserRepository;
import fr.cnes.regards.modules.accessrights.dao.projects.IResourcesAccessRepository;
import fr.cnes.regards.modules.accessrights.dao.projects.IRoleRepository;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;

/**
 * @author Marc Sordi
 *
 */
@MultitenantTransactional
public class UserResourceControllerIT extends AbstractRegardsTransactionalIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserResourceControllerIT.class);

    @Autowired
    private IRoleRepository roleRepository;

    @Autowired
    private IProjectUserRepository projectUserRepository;

    @Autowired
    private IResourcesAccessRepository resourcesAccessRepository;

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_230")
    @Purpose("Check that the system allows to retrieve a user's permissions.")
    public void getUserPermissions() {

        // Create user
        Role adminRole = roleRepository.findOneByName(DefaultRole.ADMIN.toString()).get();
        ProjectUser user = projectUserRepository
                .save(new ProjectUser(DEFAULT_USER_EMAIL, adminRole, new ArrayList<>(), new ArrayList<>()));

        // Create a new resource
        ResourcesAccess resource = new ResourcesAccess(null, "microservice", "/to/user", "controller",
                RequestMethod.GET, DefaultRole.ADMIN);
        resourcesAccessRepository.save(resource);

        // Add access to user
        user.setPermissions(Lists.newArrayList(resource));

        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(MockMvcResultMatchers.status().isOk());
        performDefaultGet(UserResourceController.TYPE_MAPPING, expectations,
                          "Error retrieving resourcesAccess for user.", user.getEmail());

        expectations.clear();
        expectations.add(MockMvcResultMatchers.status().isNotFound());
        performDefaultGet(UserResourceController.TYPE_MAPPING, expectations,
                          "The user does not exists. There should be an error 404", "wrongEmail");
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }
}
