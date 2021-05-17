/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.jpa.utils.RegardsTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityAlreadyExistsException;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.modules.accessrights.dao.projects.IProjectUserRepository;
import fr.cnes.regards.modules.accessrights.domain.UserStatus;
import fr.cnes.regards.modules.accessrights.domain.emailverification.EmailVerificationToken;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import fr.cnes.regards.modules.accessrights.domain.registration.AccessRequestDto;
import fr.cnes.regards.modules.accessrights.instance.client.IAccountsClient;
import fr.cnes.regards.modules.accessrights.instance.domain.Account;
import fr.cnes.regards.modules.accessrights.instance.domain.AccountNPassword;
import fr.cnes.regards.modules.accessrights.instance.domain.AccountSettings;
import fr.cnes.regards.modules.accessrights.instance.domain.AccountStatus;
import fr.cnes.regards.modules.accessrights.service.projectuser.IProjectUserService;
import fr.cnes.regards.modules.accessrights.service.projectuser.emailverification.IEmailVerificationTokenService;
import fr.cnes.regards.modules.accessrights.service.projectuser.workflow.listeners.WaitForQualificationListener;
import fr.cnes.regards.modules.accessrights.service.role.IRoleService;

/**
 * {@link IRegistrationService} implementation.
 * @author Xavier-Alexandre Brochard
 * @author Sébastien Binda
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

    private final WaitForQualificationListener listener;

    public RegistrationService(IProjectUserRepository pProjectUserRepository, IRoleService pRoleService,
            IEmailVerificationTokenService pTokenService, IAccountsClient accountsClient, WaitForQualificationListener listener) {
        super();
        projectUserRepository = pProjectUserRepository;
        roleService = pRoleService;
        tokenService = pTokenService;
        this.accountsClient = accountsClient;
        this.listener = listener;
    }

    @Override
    public ProjectUser requestAccess(final AccessRequestDto accountDto, Boolean isExternalAccess)
            throws EntityException {
        // Create the account if needed
        requestAccountIfNecessary(accountDto, isExternalAccess);

        // Create the project user
        return requestProjectUser(accountDto, isExternalAccess);
    }

    /**
     * Create the account if necessary
     * @param accountDto {@link AccessRequestDto} account to create if missing
     * @param isExternalAccess {@link Boolean} if true, the account is an external account (authentication not handled by regards system).
     */
    private void requestAccountIfNecessary(final AccessRequestDto accountDto, Boolean isExternalAccess)
            throws EntityException {
        // Check existence
        try {
            FeignSecurityManager.asSystem();
            ResponseEntity<EntityModel<Account>> accountResponse = accountsClient
                    .retrieveAccounByEmail(accountDto.getEmail());
            if (accountResponse.getStatusCode() != HttpStatus.NOT_FOUND) {
                LOG.info("Requesting access with an existing account. Ok, no account created");
            } else {
                // Check that all information are provided to create account
                boolean isNotValid = accountDto.getEmail() == null;
                isNotValid |= accountDto.getFirstName() == null;
                isNotValid |= accountDto.getLastName() == null;
                isNotValid |= (accountDto.getPassword() == null) && !isExternalAccess;
                if (isNotValid) {
                    LOG.error("Account does not exist for user {} and there is not enough information to create a new one.", accountDto.getEmail());
                    throw new EntityNotFoundException(accountDto.getEmail(), Account.class);
                }
                Account account = new Account(accountDto.getEmail(),
                                              accountDto.getFirstName(),
                                              accountDto.getLastName(),
                                              accountDto.getPassword());
                account.setExternal(isExternalAccess);
                AccountNPassword accountNPassword = new AccountNPassword(account, account.getPassword());
                accountsClient.createAccount(accountNPassword);
            }
        } finally {
            FeignSecurityManager.reset();
        }
    }

    /**
     * Create the project user
     * @param accountDto {@link AccessRequestDto} account to create if missing
     * @param isExternal {@link Boolean} if true, the account is an external account (authentication not handled by regards system).
     */
    private ProjectUser requestProjectUser(final AccessRequestDto accountDto, Boolean isExternal)
            throws EntityException {
        try {
            FeignSecurityManager.asSystem();
            // Check that an associated account exists
            ResponseEntity<EntityModel<Account>> accountResponse = accountsClient
                    .retrieveAccounByEmail(accountDto.getEmail());
            if (accountResponse.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new EntityNotFoundException(accountDto.getEmail(), Account.class);
            }
            Account account = accountResponse.getBody().getContent();

            // Check that no project user with same email exists
            if (projectUserRepository.findOneByEmail(accountDto.getEmail()).isPresent()) {
                throw new EntityAlreadyExistsException("The email " + accountDto.getEmail() + "is already in use.");
            }

            // Init with default role
            final Role role = roleService.getDefaultRole();

            // Create a new project user
            ProjectUser projectUser = new ProjectUser(accountDto.getEmail(), role, new ArrayList<>(),
                    accountDto.getMetadata());
            // Create
            if (isExternal) {
                // External authenticated accounts doesn't need to validate email.
                projectUser.setStatus(UserStatus.ACCESS_GRANTED);
                projectUser = projectUserRepository.save(projectUser);
            } else {
                projectUser = projectUserRepository.save(projectUser);
                // Init the email verification token
                tokenService.create(projectUser, accountDto.getOriginUrl(), accountDto.getRequestLink());

                // Check the status
                if (AccountStatus.ACTIVE.equals(account.getStatus())) {
                    LOG.info(
                            "Account is already active for new user {}. Sending AccountAcceptedEvent to handle ProjectUser status.",
                            account.getEmail());
                    listener.onAccountActivation(account.getEmail());
                }
            }
            return projectUser;
        } finally {
            FeignSecurityManager.reset();
        }
    }

}
