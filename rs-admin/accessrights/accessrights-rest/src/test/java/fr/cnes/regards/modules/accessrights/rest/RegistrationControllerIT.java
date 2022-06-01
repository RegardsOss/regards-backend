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
package fr.cnes.regards.modules.accessrights.rest;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.accessrights.dao.projects.IProjectUserRepository;
import fr.cnes.regards.modules.accessrights.dao.projects.IRoleRepository;
import fr.cnes.regards.modules.accessrights.domain.UserStatus;
import fr.cnes.regards.modules.accessrights.domain.UserVisibility;
import fr.cnes.regards.modules.accessrights.domain.projects.MetaData;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import fr.cnes.regards.modules.accessrights.domain.registration.AccessRequestDto;
import fr.cnes.regards.modules.accessrights.instance.client.IAccountsClient;
import fr.cnes.regards.modules.accessrights.instance.domain.Account;
import fr.cnes.regards.modules.accessrights.instance.domain.AccountNPassword;
import fr.cnes.regards.modules.accessrights.service.projectuser.QuotaHelperService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import java.util.*;

/**
 * Integration tests for the accesses functionalities.
 *
 * @author Xavier-Alexandre Brochard
 * @author Sébastien Binda
 */
@MultitenantTransactional
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=account" })
public class RegistrationControllerIT extends AbstractRegardsTransactionalIT {

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
     * The error message TODO: Remove this? We should not shadow the error message thrown by the caught exception.
     */
    private static final String ERROR_MESSAGE = "Cannot reach model attributes";

    private static final String ORIGIN = "origin";

    private static final Set<String> ACCESS_GROUPS = Collections.emptySet();

    /**
     * A project user.<br>
     * We ensure before each test to have only this exactly project user in db for convenience.
     */
    private ProjectUser projectUser;

    @Autowired
    private IProjectUserRepository projectUserRepository;

    @MockBean
    private QuotaHelperService quotaHelperService;

    /**
     * Root access request endpoint
     */
    private String apiAccesses;

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

    private Role publicRole;

    @Autowired
    private IRoleRepository roleRepository;

    @Autowired
    private IAccountsClient accountsClient;

    /**
     * Do some setup before each test
     */
    @Before
    public void setUp() {
        apiAccesses = RegistrationController.REQUEST_MAPPING_ROOT;
        apiAccessId = apiAccesses + "/{access_id}";
        apiAccessAccept = apiAccessId + "/accept";
        apiAccessDeny = apiAccessId + "/deny";
        publicRole = roleRepository.findOneByName(DefaultRole.PUBLIC.toString()).get();
    }

    /**
     * Check that the system allows the user to request a registration.
     */
    @SuppressWarnings("unchecked")
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_510")
    @Purpose("Check that the system allows the user to request a registration.")
    public void requestAccessSuccess() {
        MetaData metadata = new MetaData("plop", "test", UserVisibility.READABLE);
        List<MetaData> metas = new ArrayList<>();
        metas.add(metadata);
        AccessRequestDto newAccessRequest = new AccessRequestDto(EMAIL,
                                                                 FIRST_NAME,
                                                                 LAST_NAME,
                                                                 null,
                                                                 metas,
                                                                 PASSWORD,
                                                                 ORIGIN_URL,
                                                                 REQUEST_LINK,
                                                                 ORIGIN,
                                                                 ACCESS_GROUPS,
                                                                 0L);
        requestAccess(newAccessRequest);

    }

    private void requestAccess(AccessRequestDto newAccessRequest) {
        //lets mock the feign clients
        Account account = new Account(newAccessRequest.getEmail(),
                                      newAccessRequest.getFirstName(),
                                      newAccessRequest.getLastName(),
                                      newAccessRequest.getPassword());

        Mockito.when(accountsClient.retrieveAccounByEmail(newAccessRequest.getEmail()))
               .thenReturn(new ResponseEntity<>(HttpStatus.NOT_FOUND),
                           new ResponseEntity<>(EntityModel.of(account), HttpStatus.OK));
        AccountNPassword accountNPassword = new AccountNPassword(account, account.getPassword());
        Mockito.when(accountsClient.createAccount(accountNPassword))
               .thenReturn(new ResponseEntity<>(EntityModel.of(account), HttpStatus.CREATED));

        performDefaultPost(apiAccesses, newAccessRequest, customizer().expectStatusCreated(), ERROR_MESSAGE);
    }

    @Test
    public void requestAccessConflict() {
        AccessRequestDto newAccessRequest = new AccessRequestDto(EMAIL,
                                                                 FIRST_NAME,
                                                                 LAST_NAME,
                                                                 null,
                                                                 new ArrayList<>(),
                                                                 PASSWORD,
                                                                 ORIGIN_URL,
                                                                 REQUEST_LINK,
                                                                 ORIGIN,
                                                                 ACCESS_GROUPS,
                                                                 0L);
        requestAccess(newAccessRequest);
        performDefaultPost(apiAccesses, newAccessRequest, customizer().expectStatusConflict(), ERROR_MESSAGE);
    }

    /**
     * Check that the system allows to reactivate access to an access denied project user.
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_520")
    @Purpose("Check that the system allows to reactivate access to an access denied project user.")
    public void acceptAccessRequest() {
        // Prepare the test conditions
        projectUser = projectUserRepository.save(new ProjectUser(EMAIL,
                                                                 publicRole,
                                                                 new ArrayList<>(),
                                                                 new HashSet<>()));
        projectUser.setStatus(UserStatus.ACCESS_DENIED);
        projectUserRepository.save(projectUser);

        performDefaultPut(apiAccessAccept, null, customizer().expectStatusOk(), ERROR_MESSAGE, projectUser.getId());

        // Now the project user is ACCESS_GRANTED, so trying to re-accept will fail
        performDefaultPut(apiAccessAccept,
                          null,
                          customizer().expectStatusForbidden(),
                          ERROR_MESSAGE,
                          projectUser.getId());

        // something that does not exist
        performDefaultPut(apiAccessAccept, null, customizer().expectStatusNotFound(), ERROR_MESSAGE, Long.MAX_VALUE);
    }

    /**
     * Check that the system allows to refuse an access request.
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_520")
    @Purpose("Check that the system allows to refuse an access request.")
    public void denyAccessRequest() {
        // Prepare the test conditions
        projectUser = projectUserRepository.save(new ProjectUser(EMAIL,
                                                                 publicRole,
                                                                 new ArrayList<>(),
                                                                 new HashSet<>()));
        projectUser.setStatus(UserStatus.WAITING_ACCESS);
        projectUserRepository.save(projectUser);

        //lets mock the feign clients
        Account account = new Account(EMAIL, FIRST_NAME, LAST_NAME, PASSWORD);

        Mockito.when(accountsClient.retrieveAccounByEmail(account.getEmail()))
               .thenReturn(new ResponseEntity<>(EntityModel.of(account), HttpStatus.OK));

        performDefaultPut(apiAccessDeny, null, customizer().expectStatusOk(), ERROR_MESSAGE, projectUser.getId());
    }

    @Test
    public void denyAccessRequestTwice() {
        denyAccessRequest();
        // Now the project user is ACCESS_DENIED, so trying to re-deny it will fail
        performDefaultPut(apiAccessDeny,
                          null,
                          customizer().expectStatusForbidden(),
                          ERROR_MESSAGE,
                          projectUser.getId());
    }

    @Test
    public void denyAccessRequestUnknown() {

        performDefaultPut(apiAccessDeny, null, customizer().expectStatusNotFound(), ERROR_MESSAGE, Long.MAX_VALUE);
    }

    /**
     * Check that the system allows to delete a registration request.
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_520")
    @Purpose("Check that the system allows to delete a registration request.")
    public void deleteAccessRequest() {
        // Prepare the test
        projectUser = projectUserRepository.save(new ProjectUser(EMAIL,
                                                                 publicRole,
                                                                 new ArrayList<>(),
                                                                 new HashSet<>()));

        // Case not found
        performDefaultDelete(apiAccessId, customizer().expectStatusNotFound(), ERROR_MESSAGE, 12345678L);

        performDefaultDelete(apiAccessId, customizer().expectStatusOk(), ERROR_MESSAGE, projectUser.getId());
    }

    /**
     * Check that the system allows to activate an inactive project user.
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_520")
    @Purpose("Check that the system allows to activate an inactive project user.")
    public void activeAccess() {
        // Prepare the test conditions
        projectUser = projectUserRepository.save(new ProjectUser(EMAIL,
                                                                 publicRole,
                                                                 new ArrayList<>(),
                                                                 new HashSet<>()));
        projectUser.setStatus(UserStatus.ACCESS_INACTIVE);
        projectUserRepository.save(projectUser);

        //lets mock the feign clients
        Account account = new Account(projectUser.getEmail(),
                                      "projectUser.getFirstName()",
                                      "projectUser.getLastName()",
                                      "projectUser.getPassword()");

        Mockito.when(accountsClient.retrieveAccounByEmail(projectUser.getEmail()))
               .thenReturn(new ResponseEntity<>(EntityModel.of(account), HttpStatus.OK));

        // Endpoint
        String endpoint = RegistrationController.REQUEST_MAPPING_ROOT
                          + RegistrationController.ACTIVE_ACCESS_RELATIVE_PATH;

        performDefaultPut(endpoint, null, customizer().expectStatusOk(), ERROR_MESSAGE, projectUser.getId());
    }

    /**
     * Check that the system allows to deactivate an active project user.
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_520")
    @Purpose("Check that the system allows to deactivate an active project user.")
    public void inactiveAccess() {
        // Prepare the test conditions
        projectUser = projectUserRepository.save(new ProjectUser(EMAIL,
                                                                 publicRole,
                                                                 new ArrayList<>(),
                                                                 new HashSet<>()));
        projectUser.setStatus(UserStatus.ACCESS_GRANTED);
        projectUserRepository.save(projectUser);

        // Endpoint
        String endpoint = RegistrationController.REQUEST_MAPPING_ROOT
                          + RegistrationController.INACTIVE_ACCESS_RELATIVE_PATH;

        performDefaultPut(endpoint, null, customizer().expectStatusOk(), ERROR_MESSAGE, projectUser.getId());
    }

}
