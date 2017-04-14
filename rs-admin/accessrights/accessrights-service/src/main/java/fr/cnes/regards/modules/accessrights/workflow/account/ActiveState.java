/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.workflow.account;

import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.module.rest.exception.EntityTransitionForbiddenException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.modules.accessrights.dao.instance.IAccountRepository;
import fr.cnes.regards.modules.accessrights.domain.AccountStatus;
import fr.cnes.regards.modules.accessrights.domain.instance.Account;
import fr.cnes.regards.modules.accessrights.passwordreset.IPasswordResetService;
import fr.cnes.regards.modules.accessrights.service.projectuser.IProjectUserService;

/**
 * State class of the State Pattern implementing the available actions on a {@link Account} in status ACTIVE.
 *
 * @author Xavier-Alexandre Brochard
 * @since 1.1-SNAPSHOT
 */
@Component
public class ActiveState extends AbstractDeletableState {

    /**
     * Constructor
     *
     * @param pProjectUserService
     *            the project user service
     * @param pAccountRepository
     *            the account repository
     * @param pTenantResolver
     *            the tenant resolver
     * @param pRuntimeTenantResolver
     *            runtime tenant resolver
     */
    public ActiveState(final IProjectUserService pProjectUserService, final IAccountRepository pAccountRepository,
            final ITenantResolver pTenantResolver, final IRuntimeTenantResolver pRuntimeTenantResolver,
            final IPasswordResetService pPasswordResetTokenService) {
        super(pProjectUserService, pAccountRepository, pTenantResolver, pRuntimeTenantResolver,
              pPasswordResetTokenService);
    }

    @Override
    public void lockAccount(final Account pAccount) throws EntityTransitionForbiddenException {
        pAccount.setStatus(AccountStatus.LOCKED);
        getAccountRepository().save(pAccount);
    }

    @Override
    public void inactiveAccount(final Account pAccount) throws EntityTransitionForbiddenException {
        pAccount.setStatus(AccountStatus.INACTIVE);
        getAccountRepository().save(pAccount);
    }

}
