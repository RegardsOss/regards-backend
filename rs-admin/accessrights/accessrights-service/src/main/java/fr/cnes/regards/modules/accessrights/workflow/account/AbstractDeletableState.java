/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.workflow.account;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.framework.module.rest.exception.EntityTransitionForbiddenException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.modules.accessrights.dao.instance.IAccountRepository;
import fr.cnes.regards.modules.accessrights.domain.instance.Account;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.passwordreset.IPasswordResetService;
import fr.cnes.regards.modules.accessrights.registration.IVerificationTokenService;
import fr.cnes.regards.modules.accessrights.service.projectuser.IProjectUserService;

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
    private final IProjectUserService projectUserService;

    /**
     * Account Repository
     */
    private final IAccountRepository accountRepository;

    /**
     * Tenant resolver
     */
    private final ITenantResolver tenantResolver;

    /**
     * Runtime tenant resolver
     */
    private final IRuntimeTenantResolver runtimeTenantResolver;

    /**
     * Service to manage reset tokens for accounts.
     */
    private final IPasswordResetService passwordResetTokenService;

    /**
     * Service to manage verification tokens for accounts.
     */
    private final IVerificationTokenService verificationTokenService;

    /**
     * Constructor
     *
     * @param pProjectUserService
     *            the project user service
     * @param pAccountRepository
     *            the account repository
     * @param pTenantResolver
     *            the tenant resolver
     */
    public AbstractDeletableState(final IProjectUserService pProjectUserService,
            final IAccountRepository pAccountRepository, final ITenantResolver pTenantResolver,
            final IRuntimeTenantResolver pRuntimeTenantResolver, final IPasswordResetService pPasswordResetTokenService,
            final IVerificationTokenService pVerificationTokenService) {
        super();
        projectUserService = pProjectUserService;
        accountRepository = pAccountRepository;
        tenantResolver = pTenantResolver;
        runtimeTenantResolver = pRuntimeTenantResolver;
        passwordResetTokenService = pPasswordResetTokenService;
        verificationTokenService = pVerificationTokenService;
    }

    @Override
    public void deleteAccount(final Account pAccount) throws ModuleException {
        switch (pAccount.getStatus()) {
            case ACTIVE:
            case LOCKED:
            case ACCEPTED:
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
    public boolean canDelete(final Account pAccount) {

        try {
            for (String tenant : tenantResolver.getAllActiveTenants()) {
                runtimeTenantResolver.forceTenant(tenant);
                if (projectUserService.existUser(pAccount.getEmail())) {
                    return false;
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
            final String message = String.format(
                                                 "Cannot remove account %s because it is linked to at least one project.",
                                                 pAccount.getEmail());
            LOGGER.error(message);
            throw new EntityOperationForbiddenException(pAccount.getId().toString(), Account.class, message);
        }

        LOGGER.info("Deleting tokens associated to account {} from instance.", pAccount.getEmail());
        passwordResetTokenService.deletePasswordResetTokenForAccount(pAccount);
        verificationTokenService.deletePasswordResetTokenForAccount(pAccount);
        LOGGER.info("Deleting account {} from instance.", pAccount.getEmail());
        accountRepository.delete(pAccount.getId());
    }

    /**
     * @return the projectUserService
     */
    protected IProjectUserService getProjectUserService() {
        return projectUserService;
    }

    /**
     * @return the accountRepository
     */
    protected IAccountRepository getAccountRepository() {
        return accountRepository;
    }

    /**
     * @return the tenantResolver
     */
    protected ITenantResolver getTenantResolver() {
        return tenantResolver;
    }

}
