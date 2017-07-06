/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.service.account.workflow.state;

import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.jpa.instance.transactional.InstanceTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityTransitionForbiddenException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.modules.accessrights.dao.instance.IAccountRepository;
import fr.cnes.regards.modules.accessrights.domain.AccountStatus;
import fr.cnes.regards.modules.accessrights.domain.instance.Account;
import fr.cnes.regards.modules.accessrights.service.account.accountunlock.IAccountUnlockTokenService;
import fr.cnes.regards.modules.accessrights.service.account.passwordreset.IPasswordResetService;
import fr.cnes.regards.modules.accessrights.service.projectuser.IProjectUserService;
import fr.cnes.regards.modules.accessrights.service.projectuser.emailverification.IEmailVerificationTokenService;

/**
 * State class of the State Pattern implementing the available actions on a {@link Account} in status INACTIVE.
 *
 * @author Xavier-Alexandre Brochard
 */
@Component
@InstanceTransactional
public class InactiveState extends AbstractDeletableState {

    /**
     * @param pProjectUserService
     * @param pAccountRepository
     * @param pTenantResolver
     * @param pRuntimeTenantResolver
     * @param pPasswordResetTokenService
     * @param pEmailVerificationTokenService
     * @param pAccountUnlockTokenService
     */
    public InactiveState(IProjectUserService pProjectUserService, IAccountRepository pAccountRepository,
            ITenantResolver pTenantResolver, IRuntimeTenantResolver pRuntimeTenantResolver,
            IPasswordResetService pPasswordResetTokenService,
            IEmailVerificationTokenService pEmailVerificationTokenService,
            IAccountUnlockTokenService pAccountUnlockTokenService) {
        super(pProjectUserService, pAccountRepository, pTenantResolver, pRuntimeTenantResolver,
              pPasswordResetTokenService, pEmailVerificationTokenService, pAccountUnlockTokenService);
    }

    @Override
    public void activeAccount(final Account pAccount) throws EntityTransitionForbiddenException {
        pAccount.setStatus(AccountStatus.ACTIVE);
        getAccountRepository().save(pAccount);
    }

}
