/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.workflow.account;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.modules.accessrights.dao.instance.IAccountRepository;
import fr.cnes.regards.modules.accessrights.domain.AccountStatus;
import fr.cnes.regards.modules.accessrights.domain.instance.Account;
import fr.cnes.regards.modules.accessrights.domain.registration.VerificationToken;

/**
 * State class of the State Pattern implementing the available actions on a {@link Account} in status ACCEPTED.
 *
 * @author Xavier-Alexandre Brochard
 * @author Christophe Mertz
 */
@Component
public class AcceptedState implements IAccountTransitions {

    /**
     * Account repository
     */
    private final IAccountRepository accountRepository;

    /**
     * @param pAccountRepository
     *            the account repository
     */
    public AcceptedState(final IAccountRepository pAccountRepository) {
        super();
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
