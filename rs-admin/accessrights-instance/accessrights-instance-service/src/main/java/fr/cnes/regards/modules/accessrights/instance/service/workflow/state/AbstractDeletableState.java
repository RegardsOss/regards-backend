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
package fr.cnes.regards.modules.accessrights.instance.service.workflow.state;

import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.EntityModel;
import org.springframework.util.CollectionUtils;

/**
 * Abstract state implementation to implement the delete action on an account.<br>
 * Various states share this common implementation.
 *
 * @author Xavier-Alexandre Brochard
 * @author S??bastien Binda
 */
abstract class AbstractDeletableState implements IAccountTransitions {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractDeletableState.class);

    protected final IProjectUsersClient projectUsersClient;
    protected final IAccountRepository accountRepository;
    protected final ITenantService tenantService;
    protected final IRuntimeTenantResolver runtimeTenantResolver;
    protected final IPasswordResetService passwordResetTokenService;
    protected final IAccountUnlockTokenService accountUnlockTokenService;

    protected AbstractDeletableState(IProjectUsersClient projectUsersClient, IAccountRepository accountRepository, ITenantService tenantService,
            IRuntimeTenantResolver runtimeTenantResolver, IPasswordResetService passwordResetTokenService, IAccountUnlockTokenService accountUnlockTokenService
    ) {
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
                throw new EntityTransitionForbiddenException(
                        pAccount.getId().toString(),
                        ProjectUser.class,
                        pAccount.getStatus().toString(),
                        Thread.currentThread().getStackTrace()[1].getMethodName());
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.accessrights.workflow.account.IAccountTransitions#canDelete(fr.cnes.regards.modules.
     * accessrights.domain.instance.Account)
     */
    @Override
    public boolean canDelete(Account account) {
        return CollectionUtils.isEmpty(account.getProjects());
    }

    /**
     * Delete an account
     *
     * @param pAccount the account
     * @throws EntityOperationForbiddenException when the account is linked to at least a project user
     */
    private void doDelete(final Account pAccount) throws EntityOperationForbiddenException {
        if (!canDelete(pAccount)) {
            String message = String.format("Cannot remove account %s because it is linked to at least one project.", pAccount.getEmail());
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
     *
     * @param account given account
     */
    protected void deleteLinkedProjectUsers(Account account) {
        try {
            FeignSecurityManager.asSystem();
            account.getProjects().forEach(project -> {
                runtimeTenantResolver.forceTenant(project.getName());
                EntityModel<ProjectUser> projectUserBody = projectUsersClient.retrieveProjectUserByEmail(account.getEmail()).getBody();
                if (projectUserBody != null) {
                    ProjectUser projectUser = projectUserBody.getContent();
                    if (projectUser != null) {
                        projectUsersClient.removeProjectUser(projectUser.getId());
                    }
                }
                account.getProjects().remove(project);
            });
        } finally {
            FeignSecurityManager.reset();
            runtimeTenantResolver.clearTenant();
        }
    }

}
