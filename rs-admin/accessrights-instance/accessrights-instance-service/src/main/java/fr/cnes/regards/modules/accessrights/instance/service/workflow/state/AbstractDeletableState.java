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
package fr.cnes.regards.modules.accessrights.instance.service.workflow.state;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import feign.FeignException;
import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.framework.module.rest.exception.EntityTransitionForbiddenException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.accessrights.client.IProjectUsersClient;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.instance.dao.IAccountRepository;
import fr.cnes.regards.modules.accessrights.instance.domain.Account;
import fr.cnes.regards.modules.accessrights.instance.service.accountunlock.IAccountUnlockTokenService;
import fr.cnes.regards.modules.accessrights.instance.service.passwordreset.IPasswordResetService;
import fr.cnes.regards.modules.project.service.ITenantService;

/**
 * Abstract state implementation to implement the delete action on an account.<br>
 * Various states share this common implementation.
 *
 * @author Xavier-Alexandre Brochard
 * @author Sébastien Binda
 */
abstract class AbstractDeletableState implements IAccountTransitions {

    /**
     * Logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractDeletableState.class);

    /**
     * Service managing {@link ProjectUser}s. Autowired by Spring.
     */
    protected final IProjectUsersClient projectUsersClient;

    /**
     * Account Repository
     */
    protected final IAccountRepository accountRepository;

    /**
     * Tenant resolver
     */
    protected final ITenantService tenantService;

    /**
     * Runtime tenant resolver
     */
    protected final IRuntimeTenantResolver runtimeTenantResolver;

    /**
     * Service to manage reset tokens for accounts.
     */
    protected final IPasswordResetService passwordResetTokenService;

    /**
     * Service to manage unlock tokens for accounts.
     */
    protected final IAccountUnlockTokenService accountUnlockTokenService;

    /**
     * @param projectUsersClient
     * @param accountRepository
     * @param tenantService
     * @param runtimeTenantResolver
     * @param passwordResetTokenService
     * @param accountUnlockTokenService
     */
    public AbstractDeletableState(IProjectUsersClient projectUsersClient, IAccountRepository accountRepository,
            ITenantService tenantService, IRuntimeTenantResolver runtimeTenantResolver,
            IPasswordResetService passwordResetTokenService, IAccountUnlockTokenService accountUnlockTokenService) {
        super();
        this.projectUsersClient = projectUsersClient;
        this.accountRepository = accountRepository;
        this.tenantService = tenantService;
        this.runtimeTenantResolver = runtimeTenantResolver;
        this.passwordResetTokenService = passwordResetTokenService;
        this.accountUnlockTokenService = accountUnlockTokenService;
    }

    @Override
    public void deleteAccount(final Account pAccount) throws EntityException {
        switch (pAccount.getStatus()) {
            case ACTIVE:
            case LOCKED:
            case PENDING:
            case INACTIVE:
                doDelete(pAccount);
                break;
            default:
                throw new EntityTransitionForbiddenException(pAccount.getId().toString(), ProjectUser.class,
                        pAccount.getStatus().toString(), Thread.currentThread().getStackTrace()[1].getMethodName());
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.accessrights.workflow.account.IAccountTransitions#canDelete(fr.cnes.regards.modules.
     * accessrights.domain.instance.Account)
     */
    @Override
    public boolean canDelete(final Account account) {
        try {
            for (String tenant : tenantService.getAllActiveTenants(IProjectUsersClient.TARGET_NAME)) {
                runtimeTenantResolver.forceTenant(tenant);
                try {
                    FeignSecurityManager.asSystem();
                    ResponseEntity<EntityModel<ProjectUser>> projectUserResponse = projectUsersClient
                            .retrieveProjectUserByEmail(account.getEmail());
                    if (projectUserResponse.getStatusCode() != HttpStatus.NOT_FOUND) {
                        return false;
                    }
                } catch (FeignException e) {
                    // in case admin project microservice has a problem, lets just log the issue and continue for the next tenant
                    LOGGER.warn("There was an issue while trying to determine if an account is deletable.", e);
                    // in case of issues, the tenant might be in maintenance or the microservice down, so lets assume there is a project user linked to this account
                    return false;
                } finally {
                    FeignSecurityManager.reset();

                }
            }
        } finally {
            runtimeTenantResolver.clearTenant();
        }
        return true;
    }

    /**
     * Delete an account
     *
     * @param pAccount
     *            the account
     * @throws EntityOperationForbiddenException
     *             when the account is linked to at least a project user
     */
    private void doDelete(final Account pAccount) throws EntityOperationForbiddenException {
        // Fail if not allowed to delete
        if (!canDelete(pAccount)) {
            final String message = String
                    .format("Cannot remove account %s because it is linked to at least one project.",
                            pAccount.getEmail());
            LOGGER.error(message);
            throw new EntityOperationForbiddenException(pAccount.getId().toString(), Account.class, message);
        }

        LOGGER.info("Deleting password reset tokens associated to account {} from instance.", pAccount.getEmail());
        passwordResetTokenService.deletePasswordResetTokenForAccount(pAccount);
        LOGGER.info("Deleting unlock tokens associated to account {} from instance.", pAccount.getEmail());
        accountUnlockTokenService.deleteAllByAccount(pAccount);
        LOGGER.info("Deleting account {} from instance.", pAccount.getEmail());
        accountRepository.deleteById(pAccount.getId());
    }

    /**
     * Delete ALL project users associated to given account
     * @param pAccount given account
     * @throws EntityNotFoundException
     */
    protected void deleteLinkedProjectUsers(final Account pAccount) throws EntityNotFoundException {
        String email = pAccount.getEmail();
        try {
            for (String tenant : tenantService.getAllActiveTenants(IProjectUsersClient.TARGET_NAME)) {
                runtimeTenantResolver.forceTenant(tenant);
                FeignSecurityManager.asSystem();
                //lets get the project user
                ResponseEntity<EntityModel<ProjectUser>> projectUserResponse = projectUsersClient
                        .retrieveProjectUserByEmail(email);
                if (projectUserResponse.getStatusCode() != HttpStatus.NOT_FOUND) {
                    ProjectUser projectUser = projectUserResponse.getBody().getContent();
                    projectUsersClient.removeProjectUser(projectUser.getId());
                }
            }
        } finally {
            // tenant being forced on each iteration, we only need to clear it once the loop has ended
            FeignSecurityManager.reset();
            runtimeTenantResolver.clearTenant();
        }
    }

}
