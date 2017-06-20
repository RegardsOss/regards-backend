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
import fr.cnes.regards.modules.accessrights.service.account.passwordreset.IPasswordResetService;
import fr.cnes.regards.modules.accessrights.service.projectuser.IProjectUserService;
import fr.cnes.regards.modules.accessrights.service.projectuser.emailverification.IEmailVerificationTokenService;

/**
 * State class of the State Pattern implementing the available actions on a {@link Account} in status ACTIVE.
 *
 * @author Xavier-Alexandre Brochard
 * @since 1.1-SNAPSHOT
 */
@Component
@InstanceTransactional
public class ActiveState extends AbstractDeletableState {

    /**
     * @param pProjectUserService
     * @param pAccountRepository
     * @param pTenantResolver
     * @param pRuntimeTenantResolver
     * @param pPasswordResetTokenService
     * @param pEmailVerificationTokenService
     */
    public ActiveState(IProjectUserService pProjectUserService, IAccountRepository pAccountRepository,
            ITenantResolver pTenantResolver, IRuntimeTenantResolver pRuntimeTenantResolver,
            IPasswordResetService pPasswordResetTokenService,
            IEmailVerificationTokenService pEmailVerificationTokenService) {
        super(pProjectUserService, pAccountRepository, pTenantResolver, pRuntimeTenantResolver,
              pPasswordResetTokenService, pEmailVerificationTokenService);
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
