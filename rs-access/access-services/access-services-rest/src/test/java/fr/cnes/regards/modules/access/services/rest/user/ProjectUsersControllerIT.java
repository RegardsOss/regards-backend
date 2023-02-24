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
package fr.cnes.regards.modules.access.services.rest.user;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.framework.test.integration.RequestBuilderCustomizer;
import fr.cnes.regards.modules.access.services.domain.user.ProjectUserCreateDto;
import fr.cnes.regards.modules.accessrights.domain.projects.SearchProjectUserParameters;
import org.junit.Test;
import org.springframework.test.context.TestPropertySource;

import static fr.cnes.regards.modules.access.services.rest.user.mock.ProjectUsersClientMock.*;
import static fr.cnes.regards.modules.access.services.rest.user.mock.StorageRestClientMock.*;

/**
 * Integration tests for ProjectUsers REST Controller.
 */
@MultitenantTransactional
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=access" },
                    locations = { "classpath:application-test.properties" })
public class ProjectUsersControllerIT extends AbstractRegardsTransactionalIT {

    @Override
    protected String getDefaultRole() {
        return DefaultRole.PROJECT_ADMIN.toString();
    }

    @Test
    public void getAllUsers() {
        RequestBuilderCustomizer customizer = customizer().expectStatusOk();
        expectPagingFromClientMock(customizer);
        expectPagedUserFromClientMock(customizer);

        performDefaultPost(ProjectUsersController.TYPE_MAPPING + ProjectUsersController.SEARCH_USERS_PATH,
                           new SearchProjectUserParameters(),
                           customizer,
                           "Failed to retrieve users list");
    }

    @Test
    public void getUserById() {
        String apiUserId = ProjectUsersController.TYPE_MAPPING + "/{user_id}";

        RequestBuilderCustomizer customizer = customizer().expectStatusOk();
        expectSingleUserFromClientMock(customizer);

        performDefaultGet(apiUserId, customizer, "Failed to retrieve user by id", PROJECT_USER_STUB_ID);
    }

    @Test
    public void getPendingAccessesUsers() {
        RequestBuilderCustomizer customizer = customizer().expectStatusOk();
        expectPagingFromClientMock(customizer);
        expectPagedUserFromClientMock(customizer);

        performDefaultGet(ProjectUsersController.TYPE_MAPPING + ProjectUsersController.PENDINGACCESSES,
                          customizer,
                          "Failed to retrieve list of users with pending access");
    }

    @Test
    public void getCurrentUser() {
        String apiUserEmail = ProjectUsersController.TYPE_MAPPING + "/myuser";

        RequestBuilderCustomizer customizer = customizer().expectStatusOk();
        expectSingleUserFromClientMock(customizer);

        performDefaultGet(apiUserEmail, customizer, "Failed to retrieve current");
    }

    @Test
    public void getUserByEmail() {
        String apiUserEmail = ProjectUsersController.TYPE_MAPPING + "/email/{user_email}";

        RequestBuilderCustomizer customizer = customizer().expectStatusOk();
        expectSingleUserFromClientMock(customizer);

        performDefaultGet(apiUserEmail, customizer, "Failed to retrieve user by email", PROJECT_USER_STUB_EMAIL);
    }

    @Test
    public void isAdmin() {
        String apiUserEmail = ProjectUsersController.TYPE_MAPPING + "/email/{user_email}/admin";

        RequestBuilderCustomizer customizer = customizer().expectStatusOk().expectValue("$", false);

        performDefaultGet(apiUserEmail, customizer, "Failed to tell if user is admin", PROJECT_USER_STUB_EMAIL);
    }

    @Test
    public void updateUser() {
        String api = ProjectUsersController.TYPE_MAPPING + ProjectUsersController.USER_ID_RELATIVE_PATH;

        RequestBuilderCustomizer customizer = customizer().expectStatusOk();

        performDefaultPut(api, PROJECT_USER_STUB, customizer, "Failed to update user", PROJECT_USER_STUB_ID);
    }

    @Test
    public void updateCurrentUser() {
        String api = ProjectUsersController.TYPE_MAPPING + "/myuser";

        RequestBuilderCustomizer customizer = customizer().expectStatusOk();

        performDefaultPut(api, PROJECT_USER_STUB, customizer, "Failed to update current user");
    }

    @Test
    public void createUser() {
        String api = ProjectUsersController.TYPE_MAPPING;

        RequestBuilderCustomizer customizer = customizer().expectStatusOk();

        performDefaultPost(api,
                           new ProjectUserCreateDto().setRoleName(ROLE_STUB_NAME).setEmail(PROJECT_USER_STUB_EMAIL),
                           customizer,
                           "Failed to create user");
    }

    @Test
    public void deleteUser() {
        String api = ProjectUsersController.TYPE_MAPPING + ProjectUsersController.USER_ID_RELATIVE_PATH;

        RequestBuilderCustomizer customizer = customizer().expectStatusOk();

        performDefaultDelete(api, customizer, "Failed to delete user", PROJECT_USER_STUB_ID);
    }

    @Test
    public void retrieveRoleProjectUserList() {
        String api = ProjectUsersController.TYPE_MAPPING + ProjectUsersController.ROLES_ROLE_ID;

        RequestBuilderCustomizer customizer = customizer().expectStatusOk();
        expectPagingFromClientMock(customizer);
        expectPagedUserFromClientMock(customizer);

        performDefaultGet(api, customizer, "Failed to users with role", ROLE_STUB_ID);
    }

    @Test
    public void retrieveRoleProjectUsersList() {
        String api = ProjectUsersController.TYPE_MAPPING + "/roles?role_name=" + ROLE_STUB_NAME;

        RequestBuilderCustomizer customizer = customizer().expectStatusOk();
        expectPagingFromClientMock(customizer);
        expectPagedUserFromClientMock(customizer);

        performDefaultGet(api, customizer, "Failed to users with role");
    }

    protected RequestBuilderCustomizer expectSingleUserFromClientMock(RequestBuilderCustomizer customizer) {
        return customizer.expectValue("$.content.email", PROJECT_USER_STUB_EMAIL)
                         .expectValue("$.content.maxQuota", USER_QUOTA_LIMITS_STUB_MAX_QUOTA)
                         .expectValue("$.content.rateLimit", USER_QUOTA_LIMITS_STUB_RATE_LIMIT)
                         .expectValue("$.content.currentQuota", CURRENT_USER_QUOTA_STUB)
                         .expectValue("$.content.currentRate", CURRENT_USER_RATE_STUB);
    }

    protected RequestBuilderCustomizer expectPagedUserFromClientMock(RequestBuilderCustomizer customizer) {
        return customizer.expectValue("$.content.[0].content.email", PROJECT_USER_STUB_EMAIL)
                         .expectValue("$.content.[0].content.maxQuota", USER_QUOTA_LIMITS_STUB_MAX_QUOTA)
                         .expectValue("$.content.[0].content.rateLimit", USER_QUOTA_LIMITS_STUB_RATE_LIMIT)
                         .expectValue("$.content.[0].content.currentQuota", CURRENT_USER_QUOTA_STUB)
                         .expectValue("$.content.[0].content.currentRate", CURRENT_USER_RATE_STUB);
    }

    private RequestBuilderCustomizer expectPagingFromClientMock(RequestBuilderCustomizer customizer) {
        return customizer.expectValue("$.metadata.totalElements", TOTAL_ELEMENTS_STUB)
                         .expectValue("$.metadata.totalPages", TOTAL_PAGES_STUB)
                         .expectValue("$.metadata.number", PAGE_NUMBER_STUB);
    }
}
