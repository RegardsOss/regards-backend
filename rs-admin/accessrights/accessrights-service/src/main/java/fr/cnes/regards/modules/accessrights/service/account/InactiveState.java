/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.service.account;

import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.multitenant.autoconfigure.tenant.ITenantResolver;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.modules.accessrights.dao.instance.IAccountRepository;
import fr.cnes.regards.modules.accessrights.domain.AccountStatus;
import fr.cnes.regards.modules.accessrights.domain.instance.Account;
import fr.cnes.regards.modules.accessrights.service.projectuser.IProjectUserService;

/**
 * State class of the State Pattern implementing the available actions on a {@link Account} in status INACTIVE.
 *
 * @author Xavier-Alexandre Brochard
 */
@Component
public class InactiveState extends AbstractDeletableState {

    /**
     * Constructor
     *
     * @param pProjectUserService
     *            the project user service
     * @param pAccountRepository
     *            the account repository
     * @param pJwtService
     *            the jwt service
     * @param pTenantResolver
     *            the tenant resolver
     */
    public InactiveState(final IProjectUserService pProjectUserService, final IAccountRepository pAccountRepository,
            final JWTService pJwtService, final ITenantResolver pTenantResolver) {
        super(pProjectUserService, pAccountRepository, pJwtService, pTenantResolver);
    }

    @Override
    public void activeAccount(final Account pAccount) throws IllegalActionForAccountStatusException {
        pAccount.setStatus(AccountStatus.ACTIVE);
        getAccountRepository().save(pAccount);
    }

}
