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
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import fr.cnes.regards.framework.jpa.instance.transactional.InstanceTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityAlreadyExistsException;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.modules.accessrights.dao.instance.IAccountRepository;
import fr.cnes.regards.modules.accessrights.dao.projects.IProjectUserRepository;
import fr.cnes.regards.modules.accessrights.domain.AccountStatus;
import fr.cnes.regards.modules.accessrights.domain.emailverification.EmailVerificationToken;
import fr.cnes.regards.modules.accessrights.domain.instance.Account;
import fr.cnes.regards.modules.accessrights.domain.instance.AccountSettings;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import fr.cnes.regards.modules.accessrights.domain.registration.AccessRequestDto;
import fr.cnes.regards.modules.accessrights.service.account.IAccountSettingsService;
import fr.cnes.regards.modules.accessrights.service.account.workflow.events.OnAcceptAccountEvent;
import fr.cnes.regards.modules.accessrights.service.account.workflow.state.AccountWorkflowManager;
import fr.cnes.regards.modules.accessrights.service.encryption.EncryptionUtils;
import fr.cnes.regards.modules.accessrights.service.projectuser.emailverification.IEmailVerificationTokenService;
import fr.cnes.regards.modules.accessrights.service.role.IRoleService;

/**
 * {@link IRegistrationService} implementation.
 *
 * @author Xavier-Alexandre Brochard
 */
@Service
@InstanceTransactional
public class RegistrationService implements IRegistrationService {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(RegistrationService.class);

    /**
     * CRUD repository handling {@link Account}s. Autowired by Spring.
     */
    private final IAccountRepository accountRepository;

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
     * CRUD repository handling {@link AccountSettingst}s. Autowired by Spring.
     */
    private final IAccountSettingsService accountSettingsService;

    /**
     * Account workflow manager. Autowired by Spring.
     */
    private final AccountWorkflowManager accountWorkflowManager;

    /**
     * Use this to publish Spring application events
     */
    private final ApplicationEventPublisher eventPublisher;

    /**
     * @param pAccountRepository
     * @param pProjectUserRepository
     * @param pRoleService
     * @param pTokenService
     * @param pAccountSettingsService
     * @param pAccountWorkflowManager
     * @param pEventPublisher
     */
    public RegistrationService(IAccountRepository pAccountRepository, IProjectUserRepository pProjectUserRepository,
            IRoleService pRoleService, IEmailVerificationTokenService pTokenService,
            IAccountSettingsService pAccountSettingsService, AccountWorkflowManager pAccountWorkflowManager,
            ApplicationEventPublisher pEventPublisher) {
        super();
        accountRepository = pAccountRepository;
        projectUserRepository = pProjectUserRepository;
        roleService = pRoleService;
        tokenService = pTokenService;
        accountSettingsService = pAccountSettingsService;
        accountWorkflowManager = pAccountWorkflowManager;
        eventPublisher = pEventPublisher;
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
     *
     * @param pDto
     * @return
     * @throws EntityException
     */
    private void requestAccountIfNecessary(final AccessRequestDto pDto) throws EntityException {
        // Check existence
        if (accountRepository.findOneByEmail(pDto.getEmail()).isPresent()) {
            LOG.info("Requesting access with an existing account. Ok, no account created");
            return;
        }

        // Create the new account
        final Account account = new Account(pDto.getEmail(), pDto.getFirstName(), pDto.getLastName(),
                EncryptionUtils.encryptPassword(pDto.getPassword()));

        // Check status
        Assert.isTrue(AccountStatus.PENDING.equals(account.getStatus()),
                      "Trying to create an Account with other status than PENDING.");

        // Auto-accept if configured so
        final AccountSettings settings = accountSettingsService.retrieve();
        if (AccountSettings.AUTO_ACCEPT_MODE.equals(settings.getMode())) {
            accountWorkflowManager.acceptAccount(account);
        }

        // Create
        accountRepository.save(account);
    }

    /**
     * Create the project user
     *
     * @param pDto
     * @throws EntityException
     */
    private void requestProjectUser(final AccessRequestDto pDto) throws EntityException {
        // Check that an associated account exists
        Optional<Account> optionalAccount = accountRepository.findOneByEmail(pDto.getEmail());
        if (!optionalAccount.isPresent()) {
            throw new EntityNotFoundException(pDto.getEmail(), Account.class);
        }

        // Check that no project user with same email exists
        if (projectUserRepository.findOneByEmail(pDto.getEmail()).isPresent()) {
            throw new EntityAlreadyExistsException("The email " + pDto.getEmail() + "is already in use.");
        }

        // Init with default role
        final Role role = roleService.getDefaultRole();

        // Create a new project user
        final ProjectUser projectUser = new ProjectUser(pDto.getEmail(), role, new ArrayList<>(), pDto.getMetadata());

        // Create
        projectUserRepository.save(projectUser);

        // Init the email verification token
        tokenService.create(projectUser, pDto.getOriginUrl(), pDto.getRequestLink());

        // Check the status
        if (AccountStatus.ACTIVE.equals(optionalAccount.get().getStatus())) {
            eventPublisher.publishEvent(new OnAcceptAccountEvent(pDto.getEmail()));
        }
    }

}
