/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.accessrights.service.registration;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.jpa.utils.RegardsTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityAlreadyExistsException;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.modules.accessrights.dao.projects.IProjectUserRepository;
import fr.cnes.regards.modules.accessrights.domain.emailverification.EmailVerificationToken;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import fr.cnes.regards.modules.accessrights.domain.registration.AccessRequestDto;
import fr.cnes.regards.modules.accessrights.instance.client.IAccountSettingsClient;
import fr.cnes.regards.modules.accessrights.instance.client.IAccountsClient;
import fr.cnes.regards.modules.accessrights.instance.domain.Account;
import fr.cnes.regards.modules.accessrights.instance.domain.AccountNPassword;
import fr.cnes.regards.modules.accessrights.instance.domain.AccountSettings;
import fr.cnes.regards.modules.accessrights.instance.domain.AccountStatus;
import fr.cnes.regards.modules.accessrights.service.encryption.EncryptionUtils;
import fr.cnes.regards.modules.accessrights.service.projectuser.emailverification.IEmailVerificationTokenService;
import fr.cnes.regards.modules.accessrights.service.projectuser.workflow.state.ProjectUserWorkflowManager;
import fr.cnes.regards.modules.accessrights.service.role.IRoleService;

/**
 * {@link IRegistrationService} implementation.
 * @author Xavier-Alexandre Brochard
 */
@Service
@RegardsTransactional
public class RegistrationService implements IRegistrationService {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(RegistrationService.class);

    /**
     * {@link IAccountsClient} instance
     */
    private final IAccountsClient accountsClient;

    /**
     * {@link IAccountSettingsClient} instance
     */
    private final IAccountSettingsClient accountSettingsClient;

    /**
     * CRUD repository handling {@link ProjectUser}s. Autowired by Spring.
     */
    private final IProjectUserRepository projectUserRepository;

    /**
     * CRUD repository handling {@link Role}s. Autowired by Spring.
     */
    private final IRoleService roleService;

    /**
     * CRUD service handling {@link EmailVerificationToken}s. Autowired by Spring.
     */
    private final IEmailVerificationTokenService tokenService;

    /**
     * Account workflow manager
     */
    private final ProjectUserWorkflowManager projectUserWorkflowManager;

    public RegistrationService(IProjectUserRepository pProjectUserRepository, IRoleService pRoleService,
            IEmailVerificationTokenService pTokenService, ProjectUserWorkflowManager projectUserWorkflowManager,
            IAccountSettingsClient accountSettingsClient, IAccountsClient accountsClient) {
        super();
        projectUserRepository = pProjectUserRepository;
        roleService = pRoleService;
        tokenService = pTokenService;
        this.accountsClient = accountsClient;
        this.accountSettingsClient = accountSettingsClient;
        this.projectUserWorkflowManager = projectUserWorkflowManager;
    }

    @Override
    public void requestAccess(final AccessRequestDto pDto) throws EntityException {
        // Create the account if needed
        requestAccountIfNecessary(pDto);

        // Create the project user
        requestProjectUser(pDto);
    }

    /**
     * Create the account if necessary
     */
    private void requestAccountIfNecessary(final AccessRequestDto pDto) throws EntityException {
        // Check existence
        try {
            FeignSecurityManager.asSystem();
            ResponseEntity<Resource<Account>> accountResponse = accountsClient.retrieveAccounByEmail(pDto.getEmail());
            if (accountResponse.getStatusCode() != HttpStatus.NOT_FOUND) {
                LOG.info("Requesting access with an existing account. Ok, no account created");
                return;
            }

            // Create the new account
            Account account = new Account(pDto.getEmail(), pDto.getFirstName(), pDto.getLastName(), pDto.getPassword());

            // Check status
            Assert.isTrue(AccountStatus.PENDING.equals(account.getStatus()),
                          "Trying to create an Account with other status than PENDING.");

            AccountNPassword accountNPassword = new AccountNPassword(account, account.getPassword());
            // Create
            account = accountsClient.createAccount(accountNPassword).getBody().getContent();

            // Auto-accept if configured so
            ResponseEntity<Resource<AccountSettings>> accountSettingsResponse = accountSettingsClient
                    .retrieveAccountSettings();
            if (accountSettingsResponse.getStatusCode().is2xxSuccessful() && accountSettingsResponse.getBody()
                    .getContent().getMode().equals(AccountSettings.AUTO_ACCEPT_MODE)) {
                // in case the microservice does not answer properly to us, lets decide its manual
                accountsClient.acceptAccount(account.getEmail());
            }
        } finally {
            FeignSecurityManager.reset();
        }
    }

    /**
     * Create the project user
     */
    private void requestProjectUser(final AccessRequestDto pDto) throws EntityException {
        try {
            FeignSecurityManager.asSystem();
            // Check that an associated account exists
            ResponseEntity<Resource<Account>> accountResponse = accountsClient.retrieveAccounByEmail(pDto.getEmail());
            if (accountResponse.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new EntityNotFoundException(pDto.getEmail(), Account.class);
            }
            Account account = accountResponse.getBody().getContent();

            // Check that no project user with same email exists
            if (projectUserRepository.findOneByEmail(pDto.getEmail()).isPresent()) {
                throw new EntityAlreadyExistsException("The email " + pDto.getEmail() + "is already in use.");
            }

            // Init with default role
            final Role role = roleService.getDefaultRole();

            // Create a new project user
            ProjectUser projectUser = new ProjectUser(pDto.getEmail(), role, new ArrayList<>(), pDto.getMetadata());

            // Create
            projectUser = projectUserRepository.save(projectUser);

            // Init the email verification token
            tokenService.create(projectUser, pDto.getOriginUrl(), pDto.getRequestLink());

            // Check the status
            if (AccountStatus.ACTIVE.equals(account.getStatus())) {
                projectUserWorkflowManager.grantAccess(projectUser);
            }
        } finally {
            FeignSecurityManager.reset();
        }
    }

}
