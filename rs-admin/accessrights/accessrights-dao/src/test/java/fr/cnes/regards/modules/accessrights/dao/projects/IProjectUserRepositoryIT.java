/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.accessrights.dao.projects;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.accessrights.dao.AccessRightsDaoTestConfiguration;
import fr.cnes.regards.modules.accessrights.domain.UserStatus;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import fr.cnes.regards.modules.accessrights.domain.projects.RoleFactory;
import fr.cnes.regards.modules.accessrights.domain.projects.SearchProjectUserParameters;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.transaction.BeforeTransaction;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

import static fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser.REGARDS_ORIGIN;

/**
 * @author Stephane Cortine
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { AccessRightsDaoTestConfiguration.class })
@MultitenantTransactional
public class IProjectUserRepositoryIT {

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

    @Before
    public void init() {
        Assert.assertEquals("repository should be empty", 0, projectUserRepository.findAll().size());

        RoleFactory factory = new RoleFactory();
        Role role1 = roleRepository.save(factory.createPublic());
        Role role2 = roleRepository.save(factory.createInstanceAdmin());

        ProjectUser user1 = new ProjectUser("user@test.com", role1, new ArrayList<>(), new HashSet<>());
        user1.setFirstName("FirstName1");
        user1.setLastName("LastName1");
        user1.setLastConnection(OffsetDateTime.now().minusDays(1));
        user1.setAccessGroups(Sets.newHashSet("Public"));

        ProjectUser user2 = new ProjectUser("user2@test.com", role1, new ArrayList<>(), new HashSet<>());
        user2.setFirstName("FirstName2");
        user2.setLastName("LastName2");
        user2.setOrigin("Origin2");
        user2.setLastConnection(OffsetDateTime.now().minusDays(2));
        user2.setAccessGroups(Sets.newHashSet("Private", "Regards"));

        ProjectUser user3 = new ProjectUser("user3@test.com", role1, new ArrayList<>(), new HashSet<>());
        user3.setFirstName("FirstName3");
        user3.setLastName("LastName3");
        user3.setOrigin("Origin2");
        user3.setLastConnection(OffsetDateTime.now().minusDays(3));
        user3.setAccessGroups(Sets.newHashSet("Regards"));

        ProjectUser user4 = new ProjectUser("user4@test.com", role1, new ArrayList<>(), new HashSet<>());
        user4.setFirstName("FirstName4");
        user4.setLastName("LastName4");
        user4.setOrigin("Origin4");
        user4.setLastConnection(OffsetDateTime.now().minusDays(4));

        ProjectUser user5 = new ProjectUser("regards@test.com", role2, new ArrayList<>(), new HashSet<>());
        user5.setFirstName("FirstRegards");
        user5.setLastName("LastRegards");
        user5.setOrigin("OriginRegards");
        user5.setLastConnection(OffsetDateTime.now().minusDays(5));
        user5.setAccessGroups(Sets.newHashSet("ALL"));

        projectUserRepository.save(user1);
        projectUserRepository.save(user2);
        projectUserRepository.save(user3);
        projectUserRepository.save(user4);
        projectUserRepository.save(user5);
    }

    @After
    public void reset() {
        roleRepository.deleteAll();
        projectUserRepository.deleteAll();
    }

    @Test
    public void test_findAll_with_firstname() {
        // Given
        SearchProjectUserParameters filters = new SearchProjectUserParameters().withFirstName("FirstName1");
        // When
        Page<ProjectUser> projectUserResult = projectUserRepository.findAll(new ProjectUserSpecificationsBuilderNew().withParameters(
            filters).build(), PageRequest.of(0, 5));
        //Then
        Assert.assertEquals("search of users which firstname contains \"FirstName1\" should return 1 users",
                            1,
                            projectUserResult.getContent().size());

        // Given
        filters = new SearchProjectUserParameters().withFirstName("FirstName");
        // When
        projectUserResult = projectUserRepository.findAll(new ProjectUserSpecificationsBuilderNew().withParameters(
            filters).build(), PageRequest.of(0, 5));
        // Then
        Assert.assertEquals("search of users which firstname contains \"FirstName\" should return 4 users",
                            4,
                            projectUserResult.getContent().size());
    }

    @Test
    public void test_findAll_with_lastname() {
        // Given
        SearchProjectUserParameters filters = new SearchProjectUserParameters().withLastName("LastName1");
        // When
        Page<ProjectUser> projectUserResult = projectUserRepository.findAll(new ProjectUserSpecificationsBuilderNew().withParameters(
            filters).build(), PageRequest.of(0, 5));
        //Then
        Assert.assertEquals("search of users which lastname contains \"LastName1\" should return 1 users",
                            1,
                            projectUserResult.getContent().size());

        // Given
        filters = new SearchProjectUserParameters().withLastName("LastName");
        // When
        projectUserResult = projectUserRepository.findAll(new ProjectUserSpecificationsBuilderNew().withParameters(
            filters).build(), PageRequest.of(0, 5));
        // Then
        Assert.assertEquals("search of users which lastname contains \"LastName\" should return 4 users",
                            4,
                            projectUserResult.getContent().size());
    }

    @Test
    public void test_findAll_with_email() {
        // Given
        SearchProjectUserParameters filters = new SearchProjectUserParameters().withEmail("user4");
        // When
        Page<ProjectUser> projectUserResult = projectUserRepository.findAll(new ProjectUserSpecificationsBuilderNew().withParameters(
            filters).build(), PageRequest.of(0, 5));
        //Then
        Assert.assertEquals("search of users which email contains \"user4\" should return 1 users",
                            1,
                            projectUserResult.getContent().size());

        // Given
        filters = new SearchProjectUserParameters().withEmail("user");
        // When
        projectUserResult = projectUserRepository.findAll(new ProjectUserSpecificationsBuilderNew().withParameters(
            filters).build(), PageRequest.of(0, 5));
        // Then
        Assert.assertEquals("search of users which email contains \"user\" should return 4 users",
                            4,
                            projectUserResult.getContent().size());
    }

    @Test
    public void test_findAll_without_criterias() {
        // Given

        // When
        Page<ProjectUser> projectUserResult = projectUserRepository.findAll(new ProjectUserSpecificationsBuilderNew().build(),
                                                                            PageRequest.of(0, 5));
        //Then
        Assert.assertEquals("search of users without filters should return ALL users",
                            projectUserRepository.findAll().size(),
                            projectUserResult.getContent().size());
    }

    @Test
    public void test_findAll_with_status() {
        // Given
        SearchProjectUserParameters filters = new SearchProjectUserParameters().withStatusIncluded(Arrays.asList(
            UserStatus.WAITING_ACCOUNT_ACTIVE));
        // When
        Page<ProjectUser> projectUserResult = projectUserRepository.findAll(new ProjectUserSpecificationsBuilderNew().withParameters(
            filters).build(), PageRequest.of(0, 5));
        //Then
        Assert.assertEquals("search of users with status active should return ALL users",
                            projectUserRepository.findAll().size(),
                            projectUserResult.getContent().size());
    }

    @Test
    public void test_findAll_with_origin() {
        // Given
        SearchProjectUserParameters filters = new SearchProjectUserParameters().withOriginsIncluded(Arrays.asList(
            REGARDS_ORIGIN));
        // When
        Page<ProjectUser> projectUserResult = projectUserRepository.findAll(new ProjectUserSpecificationsBuilderNew().withParameters(
            filters).build(), PageRequest.of(0, 5));
        //Then
        Assert.assertEquals("search of users which origin contains \"Regards\" should return 1 users",
                            1,
                            projectUserResult.getContent().size());

        // Given
        filters = new SearchProjectUserParameters().withOriginsIncluded(Arrays.asList(REGARDS_ORIGIN, "Origin4"));
        // When
        projectUserResult = projectUserRepository.findAll(new ProjectUserSpecificationsBuilderNew().withParameters(
            filters).build(), PageRequest.of(0, 5));
        // Then
        Assert.assertEquals("search of users which origin contains \"Regards\" and \"origin4\" should return 2 users",
                            2,
                            projectUserResult.getContent().size());
    }

    @Test
    public void test_findAll_with_role() {
        // Given
        SearchProjectUserParameters filters = new SearchProjectUserParameters().withRolesIncluded(Arrays.asList(
            DefaultRole.PUBLIC.toString()));
        // When
        Page<ProjectUser> projectUserResult = projectUserRepository.findAll(new ProjectUserSpecificationsBuilderNew().withParameters(
            filters).build(), PageRequest.of(0, 5));
        //Then
        Assert.assertEquals("search of users which role contains \"PUBLIC\" should return 4 users",
                            4,
                            projectUserResult.getContent().size());

        // Given
        filters = new SearchProjectUserParameters().withRolesIncluded(Arrays.asList(DefaultRole.ADMIN.toString()));
        // When
        projectUserResult = projectUserRepository.findAll(new ProjectUserSpecificationsBuilderNew().withParameters(
            filters).build(), PageRequest.of(0, 5));
        //Then
        Assert.assertEquals("search of users which role contains \"INSTANCE_ADMIN\" should return 0 users",
                            0,
                            projectUserResult.getContent().size());

        // Given
        filters = new SearchProjectUserParameters().withRolesIncluded(Arrays.asList(DefaultRole.PUBLIC.toString(),
                                                                                    DefaultRole.INSTANCE_ADMIN.toString()));
        // When
        projectUserResult = projectUserRepository.findAll(new ProjectUserSpecificationsBuilderNew().withParameters(
            filters).build(), PageRequest.of(0, 5));
        //Then
        Assert.assertEquals("search of users which role contains \"PUBLIC\" or \"INSTANCE_ADMIN\" should return 4 users",
                            5,
                            projectUserResult.getContent().size());
    }

    @Test
    public void test_findAll_with_created() {
        // Given
        OffsetDateTime begin = OffsetDateTime.now().minusDays(1);
        OffsetDateTime end = OffsetDateTime.now().plusDays(1);
        SearchProjectUserParameters filters = new SearchProjectUserParameters().withCreationDateBefore(end)
                                                                               .withCreationDateAfter(begin);
        // When
        Page<ProjectUser> projectUserResult = projectUserRepository.findAll(new ProjectUserSpecificationsBuilderNew().withParameters(
            filters).build(), PageRequest.of(0, 5));
        //Then
        Assert.assertEquals(5, projectUserResult.getContent().size());

        // Given
        filters = new SearchProjectUserParameters().withCreationDateBefore(begin).withCreationDateAfter(end);
        // When
        projectUserResult = projectUserRepository.findAll(new ProjectUserSpecificationsBuilderNew().withParameters(
            filters).build(), PageRequest.of(0, 5));
        //Then
        Assert.assertEquals(0, projectUserResult.getContent().size());
    }

    @Test
    public void test_findAll_with_lastConnection() {
        // Given
        OffsetDateTime begin = OffsetDateTime.now().minusDays(2);
        OffsetDateTime end = OffsetDateTime.now();
        SearchProjectUserParameters filters = new SearchProjectUserParameters().withLastConnectionBefore(end)
                                                                               .withLastConnectionAfter(begin);
        // When
        Page<ProjectUser> projectUserResult = projectUserRepository.findAll(new ProjectUserSpecificationsBuilderNew().withParameters(
            filters).build(), PageRequest.of(0, 5));
        //Then
        Assert.assertEquals(1, projectUserResult.getContent().size());

        // Given
        filters = new SearchProjectUserParameters().withLastConnectionBefore(begin).withLastConnectionAfter(end);
        // When
        projectUserResult = projectUserRepository.findAll(new ProjectUserSpecificationsBuilderNew().withParameters(
            filters).build(), PageRequest.of(0, 5));
        //Then
        Assert.assertEquals(0, projectUserResult.getContent().size());
    }

    @Test
    public void test_findAll_with_accessGroup() {
        // Given
        SearchProjectUserParameters filters = new SearchProjectUserParameters().withAccessGroupsIncluded(Arrays.asList(
            "Public"));
        // When
        Page<ProjectUser> projectUserResult = projectUserRepository.findAll(new ProjectUserSpecificationsBuilderNew().withParameters(
            filters).build(), PageRequest.of(0, 5));
        //Then
        Assert.assertEquals(1, projectUserResult.getContent().size());

        // Given
        filters = new SearchProjectUserParameters().withAccessGroupsIncluded(Arrays.asList("Private", "Regards"));
        // When
        projectUserResult = projectUserRepository.findAll(new ProjectUserSpecificationsBuilderNew().withParameters(
            filters).build(), PageRequest.of(0, 5));
        //Then
        Assert.assertEquals(2, projectUserResult.getContent().size());
    }

    @Test
    public void test_findAll_all_criterias() {
        // Given
        OffsetDateTime begin = OffsetDateTime.now().minusDays(6);
        OffsetDateTime end = OffsetDateTime.now().plusDays(1);
        SearchProjectUserParameters filters = new SearchProjectUserParameters().withFirstName("First")
                                                                               .withLastName("Last")
                                                                               .withEmail("regards")
                                                                               .withRolesIncluded(Arrays.asList(
                                                                                   DefaultRole.INSTANCE_ADMIN.toString()))
                                                                               .withOriginsIncluded(Arrays.asList(
                                                                                   "OriginRegards"))
                                                                               .withAccessGroupsIncluded(Arrays.asList(
                                                                                   "ALL"))
                                                                               .withLastConnectionBefore(end)
                                                                               .withLastConnectionAfter(begin)
                                                                               .withCreationDateBefore(end)
                                                                               .withCreationDateAfter(begin);
        // When
        Page<ProjectUser> projectUserResult = projectUserRepository.findAll(new ProjectUserSpecificationsBuilderNew().withParameters(
            filters).build(), PageRequest.of(0, 5));
        //Then
        Assert.assertEquals(1, projectUserResult.getContent().size());
    }

}
