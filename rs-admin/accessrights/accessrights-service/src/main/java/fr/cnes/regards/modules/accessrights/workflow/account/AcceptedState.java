/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.workflow.account;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.modules.accessrights.dao.instance.IAccountRepository;
import fr.cnes.regards.modules.accessrights.domain.AccountStatus;
import fr.cnes.regards.modules.accessrights.domain.instance.Account;
import fr.cnes.regards.modules.accessrights.domain.registration.VerificationToken;
import fr.cnes.regards.modules.accessrights.passwordreset.IPasswordResetService;
import fr.cnes.regards.modules.accessrights.registration.IVerificationTokenService;
import fr.cnes.regards.modules.accessrights.service.projectuser.IProjectUserService;

/**
 * State class of the State Pattern implementing the available actions on a {@link Account} in status ACCEPTED.
 *
 * @author Xavier-Alexandre Brochard
 * @author Christophe Mertz
 */
@Component
public class AcceptedState extends AbstractDeletableState {

    /**
     * Account repository
     */
    private final IAccountRepository accountRepository;

    /**
     * @param pAccountRepository
     *            the account repository
     */
    public AcceptedState(final IProjectUserService pProjectUserService, final IAccountRepository pAccountRepository,
            final ITenantResolver pTenantResolver, final IRuntimeTenantResolver pRuntimeTenantResolver,
            final IPasswordResetService pPasswordResetTokenService,
            final IVerificationTokenService pVerificationTokenService) {
        super(pProjectUserService, pAccountRepository, pTenantResolver, pRuntimeTenantResolver,
              pPasswordResetTokenService, pVerificationTokenService);
        accountRepository = pAccountRepository;
    }

    @Override
    public void validateAccount(final VerificationToken pVerificationToken) throws EntityOperationForbiddenException {
        final Account account = pVerificationToken.getAccount();

        if (pVerificationToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new EntityOperationForbiddenException(account.getEmail(), Account.class,
                    "Verification token has expired");
        }

        account.setStatus(AccountStatus.ACTIVE);
        accountRepository.save(account);
    }

}
