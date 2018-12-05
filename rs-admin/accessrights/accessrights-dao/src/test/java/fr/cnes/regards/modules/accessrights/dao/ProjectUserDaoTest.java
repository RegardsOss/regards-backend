/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.accessrights.dao;

import java.time.OffsetDateTime;
import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.transaction.BeforeTransaction;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.modules.accessrights.dao.projects.IProjectUserRepository;
import fr.cnes.regards.modules.accessrights.dao.projects.IRoleRepository;
import fr.cnes.regards.modules.accessrights.dao.projects.ProjectUserSpecification;
import fr.cnes.regards.modules.accessrights.domain.UserStatus;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import fr.cnes.regards.modules.accessrights.domain.projects.RoleFactory;

/**
 *
 * Test class for {@link ProjectUser} DAO module
 *
 * @author Xavier-Alexandre Brochard

 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { AccessRightsDaoTestConfiguration.class })
@MultitenantTransactional
public class ProjectUserDaoTest {

    /**
     * JPA Repository
     */
    @Autowired
    private IProjectUserRepository projectUserRepository;

    /**
     * Runtime tenant resolver
     */
    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    /**
     * Role projectUserRepository.
     */
    @Autowired
    private IRoleRepository roleRepository;

    @BeforeTransaction
    public void beforeTransaction() {
        runtimeTenantResolver.forceTenant("test1");
    }

    /**
     * Check that the system updates automatically the field lastUpdate before any db persistence.
     */
    @Test
    @Purpose("Check that the system updates automatically the field lastUpdate before any db persistence.")
    public final void setLastUpdateListener() {

        final RoleFactory factory = new RoleFactory();
        final Role role = roleRepository.save(factory.createPublic());
        final ProjectUser user = new ProjectUser("email@test.com", role, new ArrayList<>(), new ArrayList<>());

        // Init with a past date (2 days ago)
        final OffsetDateTime initial = OffsetDateTime.now().minusDays(2);
        user.setLastUpdate(initial);
        Assert.assertEquals(user.getLastUpdate(), initial);

        // Call tested method
        final ProjectUser saved = projectUserRepository.save(user);

        // Check the value was updated
        Assert.assertNotEquals(saved.getLastUpdate(), initial);
        Assert.assertTrue(saved.getLastUpdate().isAfter(initial));
    }

    @Test
    public void testStartsWithSpec() {
        Assert.assertEquals("repository should be empty", 0, projectUserRepository.findAll().size());
        RoleFactory factory = new RoleFactory();
        Role role = roleRepository.save(factory.createPublic());
        ProjectUser user = new ProjectUser("user@test.com", role, new ArrayList<>(), new ArrayList<>());
        ProjectUser user2 = new ProjectUser("user2@test.com", role, new ArrayList<>(), new ArrayList<>());
        ProjectUser user3 = new ProjectUser("user3@test.com", role, new ArrayList<>(), new ArrayList<>());
        ProjectUser user4 = new ProjectUser("user4@test.com", role, new ArrayList<>(), new ArrayList<>());

        projectUserRepository.save(user);
        projectUserRepository.save(user2);
        projectUserRepository.save(user3);
        projectUserRepository.save(user4);

        Page<ProjectUser> result = projectUserRepository.findAll(ProjectUserSpecification.search(null, "user"), new PageRequest(0, 4));
        Assert.assertEquals("search of users which email starts with \"user\" should return 4 user", 4, result.getContent().size());

        result = projectUserRepository.findAll(ProjectUserSpecification.search(null, "user4"), new PageRequest(0, 4));
        Assert.assertEquals("search of users which email starts with \"user4\" should return 1 user", 1, result.getContent().size());

        result = projectUserRepository.findAll(ProjectUserSpecification.search(null, null), new PageRequest(0, 4));
        Assert.assertEquals("search of users which email starts with NOTHING should return ALL user", projectUserRepository.findAll().size(), result.getContent().size());

        result = projectUserRepository.findAll(ProjectUserSpecification.search(UserStatus.WAITING_ACCOUNT_ACTIVE.toString(), null), new PageRequest(0, 4));
        Assert.assertEquals("search of users which email starts with NOTHING should return ALL user", projectUserRepository.findAll().size(), result.getContent().size());
    }

}
