/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.workflow.account;

import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.modules.accessrights.dao.instance.IAccountRepository;
import fr.cnes.regards.modules.accessrights.domain.AccountStatus;
import fr.cnes.regards.modules.accessrights.domain.instance.Account;

/**
 * State class of the State Pattern implementing the available actions on a {@link Account} in status ACCEPTED.
 *
 * @author Xavier-Alexandre Brochard
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
    public void emailValidation(final Account pAccount, final String pCode) throws EntityOperationForbiddenException {
        if (!pAccount.getCode().equals(pCode)) {
            throw new EntityOperationForbiddenException(pAccount.getId().toString(), Account.class,
                    "The validation code is incorrect");
        }
        pAccount.setStatus(AccountStatus.ACTIVE);
        accountRepository.save(pAccount);
    }

}
