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
package fr.cnes.regards.modules.accessrights.rest;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.accessrights.dao.projects.IAccessSettingsRepository;
import fr.cnes.regards.modules.accessrights.dao.projects.IProjectUserRepository;
import fr.cnes.regards.modules.accessrights.dao.projects.IRoleRepository;
import fr.cnes.regards.modules.accessrights.domain.UserStatus;
import fr.cnes.regards.modules.accessrights.domain.projects.AccessSettings;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import fr.cnes.regards.modules.accessrights.domain.registration.AccessRequestDto;
import fr.cnes.regards.modules.accessrights.instance.client.IAccountSettingsClient;
import fr.cnes.regards.modules.accessrights.instance.client.IAccountsClient;
import fr.cnes.regards.modules.accessrights.instance.domain.Account;
import fr.cnes.regards.modules.accessrights.instance.domain.AccountNPassword;
import fr.cnes.regards.modules.accessrights.instance.domain.AccountSettings;

/**
 * Integration tests for the accesses functionalities.
 *
 * @author Xavier-Alexandre Brochard
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
@MultitenantTransactional
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=account" })
public class RegistrationControllerIT extends AbstractRegardsTransactionalIT {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(RegistrationControllerIT.class);

    /**
     * Dummy email
     */
    private static final String EMAIL = "RegistrationControllerIT@test.com";

    /**
     * Dummy first name
     */
    private static final String FIRST_NAME = "Firstname";

    /**
     * Dummy last name
     */
    private static final String LAST_NAME = "Lastname";

    /**
     * Dummy password
     */
    private static final String PASSWORD = "password";

    /**
     * Dummy origin url
     */
    private static final String ORIGIN_URL = "originUrl";

    /**
     * Dummy request link
     */
    private static final String REQUEST_LINK = "requestLink";

    /**
     * A project user.<br>
     * We ensure before each test to have only this exactly project user in db for convenience.
     */
    private ProjectUser projectUser;

    @Autowired
    private IProjectUserRepository projectUserRepository;

    @Autowired
    private IAccessSettingsRepository accessSettingsRepository;

    /**
     * Root access request endpoint
     */
    private String apiAccesses;

    /**
     * Endpoint to access list of users in status access pending
     */
    private String apiAccessesPending;

    /**
     * Endpoint for a specific access request
     */
    private String apiAccessId;

    /**
     * Endpoint for accepting access request
     */
    private String apiAccessAccept;

    /**
     * Endpoint for denying access request
     */
    private String apiAccessDeny;

    /**
     * Endpoint for access request settings
     */
    private String apiAccessSettings;

    /**
     * The error message TODO: Remove this? We should not shadow the error message thrown by the caught exception.
     */
    private String errorMessage;

    private Role publicRole;

    @Autowired
    private IRoleRepository roleRepository;

    @Autowired
    private IAccountsClient accountsClient;

    @Autowired
    private IAccountSettingsClient accountSettingsClient;


    /**
     * Do some setup before each test
     */
    @Before
    public void setUp() {
        apiAccesses = RegistrationController.REQUEST_MAPPING_ROOT;
        apiAccessId = apiAccesses + "/{access_id}";
        apiAccessAccept = apiAccessId + "/accept";
        apiAccessDeny = apiAccessId + "/deny";
        apiAccessSettings = apiAccesses + "/settings";
        apiAccessesPending = ProjectUsersController.TYPE_MAPPING + "/pendingaccesses";
        errorMessage = "Cannot reach model attributes";
        publicRole = roleRepository.findOneByName(DefaultRole.PUBLIC.toString()).get();
    }

    /**
     * Check that the system allows to retrieve all access requests for a project.
     */
    @MultitenantTransactional
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_310")
    @Purpose("Check that the system allows to retrieve all access requests for a project.")
    public void getAllAccesses() {
        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(MockMvcResultMatchers.status().isOk());
        performDefaultGet(apiAccessesPending, expectations, errorMessage);
    }

    /**
     * Check that the system allows the user to request a registration.
     */
    @MultitenantTransactional
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_510")
    @Purpose("Check that the system allows the user to request a registration.")
    public void requestAccess() {
        final AccessRequestDto newAccessRequest = new AccessRequestDto(EMAIL, FIRST_NAME, LAST_NAME, null,
                new ArrayList<>(), PASSWORD, ORIGIN_URL, REQUEST_LINK);

        //lets mock the feign clients
        Account account = new Account(newAccessRequest.getEmail(), newAccessRequest.getFirstName(), newAccessRequest.getLastName(), newAccessRequest.getPassword());
        AccountSettings accountSettings = new AccountSettings();

        Mockito.when(accountsClient.retrieveAccounByEmail(newAccessRequest.getEmail()))
                .thenReturn(new ResponseEntity<>(HttpStatus.NOT_FOUND),
                            new ResponseEntity<>(new Resource<>(account), HttpStatus.OK));
        AccountNPassword accountNPassword = new AccountNPassword(account, account.getPassword());
        Mockito.when(accountsClient.createAccount(accountNPassword))
                .thenReturn(new ResponseEntity<>(new Resource<>(account), HttpStatus.CREATED));
        Mockito.when(accountSettingsClient.retrieveAccountSettings())
                .thenReturn(new ResponseEntity<>(new Resource<>(accountSettings), HttpStatus.OK));

        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(MockMvcResultMatchers.status().isCreated());
        performDefaultPost(apiAccesses, newAccessRequest, expectations, errorMessage);

        expectations.clear();
        expectations.add(MockMvcResultMatchers.status().isConflict());
        performDefaultPost(apiAccesses, newAccessRequest, expectations, errorMessage);
    }

    /**
     * Check that the system allows to reactivate access to an access denied project user.
     */
    @MultitenantTransactional
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_520")
    @Purpose("Check that the system allows to reactivate access to an access denied project user.")
    public void acceptAccessRequest() {
        // Prepare the test conditions
        projectUser = projectUserRepository
                .save(new ProjectUser(EMAIL, publicRole, new ArrayList<>(), new ArrayList<>()));
        projectUser.setStatus(UserStatus.ACCESS_DENIED);
        projectUserRepository.save(projectUser);

        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(MockMvcResultMatchers.status().isOk());
        performDefaultPut(apiAccessAccept, null, expectations, errorMessage, projectUser.getId());

        // Now the project user is ACCESS_GRANTED, so trying to re-accept will fail
        expectations.clear();
        expectations.add(MockMvcResultMatchers.status().isForbidden());
        performDefaultPut(apiAccessAccept, null, expectations, errorMessage, projectUser.getId());

        // something that does not exist
        expectations.clear();
        expectations.add(MockMvcResultMatchers.status().isNotFound());
        performDefaultPut(apiAccessAccept, null, expectations, errorMessage, Long.MAX_VALUE);
    }

    /**
     * Check that the system allows to refuse an access request.
     */
    @MultitenantTransactional
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_520")
    @Purpose("Check that the system allows to refuse an access request.")
    public void denyAccessRequest() {
        // Prepare the test conditions
        projectUser = projectUserRepository
                .save(new ProjectUser(EMAIL, publicRole, new ArrayList<>(), new ArrayList<>()));
        projectUser.setStatus(UserStatus.WAITING_ACCESS);
        projectUserRepository.save(projectUser);

        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(MockMvcResultMatchers.status().isOk());
        performDefaultPut(apiAccessDeny, null, expectations, errorMessage, projectUser.getId());

        // Now the project user is ACCESS_DENIED, so trying to re-deny it will fail
        expectations.clear();
        expectations.add(MockMvcResultMatchers.status().isForbidden());
        performDefaultPut(apiAccessDeny, null, expectations, errorMessage, projectUser.getId());

        expectations.clear();
        expectations.add(MockMvcResultMatchers.status().isNotFound());
        performDefaultPut(apiAccessDeny, null, expectations, errorMessage, Long.MAX_VALUE);
    }

    /**
     * Check that the system allows to delete a registration request.
     */
    @MultitenantTransactional
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_520")
    @Purpose("Check that the system allows to delete a registration request.")
    public void deleteAccessRequest() {
        // Prepare the test
        projectUser = projectUserRepository
                .save(new ProjectUser(EMAIL, publicRole, new ArrayList<>(), new ArrayList<>()));

        // Case not found
        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(MockMvcResultMatchers.status().isNotFound());
        performDefaultDelete(apiAccessId, expectations, errorMessage, 12345678L);

        expectations.clear();
        expectations.add(MockMvcResultMatchers.status().isOk());
        performDefaultDelete(apiAccessId, expectations, errorMessage, projectUser.getId());

        expectations.clear();
        expectations.add(MockMvcResultMatchers.status().isNotFound());
        performDefaultDelete(apiAccessId, expectations, errorMessage, projectUser.getId());
    }

    /**
     * Check that the system allows to retrieve the access settings.
     */
    @MultitenantTransactional
    @Test
    @Purpose("Check that the system allows to retrieve the access settings.")
    public void getAccessSettings() {
        // Populate
        accessSettingsRepository.save(new AccessSettings());

        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(MockMvcResultMatchers.status().isOk());
        performDefaultGet(apiAccessSettings, expectations, errorMessage);
    }

    /**
     * Check that the system fails when trying to update a non existing access settings.
     */
    @MultitenantTransactional
    @Test
    @Purpose("Check that the system fails when trying to update a non existing access settings.")
    public void updateAccessSettingsEntityNotFound() {
        final AccessSettings settings = new AccessSettings();
        settings.setId(999L);

        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(MockMvcResultMatchers.status().isNotFound());
        performDefaultPut(apiAccessSettings, settings, expectations, "TODO Error message");
    }

    /**
     * Check that the system allows to update access settings in regular case.
     */
    @MultitenantTransactional
    @Test
    @Purpose("Check that the system allows to update access settings in regular case.")
    public void updateAccessSettings() {

        // First save settings
        final AccessSettings settings = new AccessSettings();
        accessSettingsRepository.save(settings);

        // Then update them
        settings.setMode("manual");

        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(MockMvcResultMatchers.status().isOk());
        performDefaultPut(apiAccessSettings, settings, expectations, "TODO Error message");
    }

    /**
     * Check that the system allows to activate an inactive project user.
     */
    @MultitenantTransactional
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_520")
    @Purpose("Check that the system allows to activate an inactive project user.")
    public void activeAccess() {
        // Prepare the test conditions
        projectUser = projectUserRepository
                .save(new ProjectUser(EMAIL, publicRole, new ArrayList<>(), new ArrayList<>()));
        projectUser.setStatus(UserStatus.ACCESS_INACTIVE);
        projectUserRepository.save(projectUser);

        //lets mock the feign clients
        Account account = new Account(projectUser.getEmail(), "projectUser.getFirstName()", "projectUser.getLastName()", "projectUser.getPassword()");

        Mockito.when(accountsClient.retrieveAccounByEmail(projectUser.getEmail()))
                .thenReturn(new ResponseEntity<>(new Resource<>(account), HttpStatus.OK));

        // Endpoint
        String endpoint = RegistrationController.REQUEST_MAPPING_ROOT
                + RegistrationController.ACTIVE_ACCESS_RELATIVE_PATH;

        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(MockMvcResultMatchers.status().isOk());
        performDefaultPut(endpoint, null, expectations, errorMessage, projectUser.getId());
    }

    /**
     * Check that the system allows to deactivate an active project user.
     */
    @MultitenantTransactional
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_520")
    @Purpose("Check that the system allows to deactivate an active project user.")
    public void inactiveAccess() {
        // Prepare the test conditions
        projectUser = projectUserRepository
                .save(new ProjectUser(EMAIL, publicRole, new ArrayList<>(), new ArrayList<>()));
        projectUser.setStatus(UserStatus.ACCESS_GRANTED);
        projectUserRepository.save(projectUser);

        // Endpoint
        String endpoint = RegistrationController.REQUEST_MAPPING_ROOT
                + RegistrationController.INACTIVE_ACCESS_RELATIVE_PATH;

        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(MockMvcResultMatchers.status().isOk());
        performDefaultPut(endpoint, null, expectations, errorMessage, projectUser.getId());
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }
}
