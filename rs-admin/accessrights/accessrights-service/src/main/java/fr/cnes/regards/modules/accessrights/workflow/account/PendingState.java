/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.workflow.account;

import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.modules.accessrights.dao.instance.IAccountRepository;
import fr.cnes.regards.modules.accessrights.domain.AccountStatus;
import fr.cnes.regards.modules.accessrights.domain.instance.Account;
import fr.cnes.regards.modules.accessrights.registration.IRegistrationService;

/**
 * State class of the State Pattern implementing the available actions on a {@link Account} in status PENDING.
 *
 * @author Xavier-Alexandre Brochard
 * @since 1.1-SNAPSHOT
 */
@Component
public class PendingState implements IAccountTransitions {

    /**
     * Account Repository. Autowired by Spring.
     */
    private final IAccountRepository accountRepository;

    /**
     * The registration service
     */
    private IRegistrationService registrationService;

    /**
     * Creates a new PENDING state
     *
     * @param pAccountRepository
     *            the account repository
     */
    public PendingState(final IAccountRepository pAccountRepository) {
        super();
        accountRepository = pAccountRepository;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * fr.cnes.regards.modules.accessrights.workflow.account.IAccountTransitions#acceptAccount(fr.cnes.regards.modules.
     * accessrights.domain.instance.Account)
     */
    @Override
    public void acceptAccount(final Account pAccount) throws EntityException {
        pAccount.setStatus(AccountStatus.ACCEPTED);
        accountRepository.save(pAccount);

        registrationService.sendValidationEmail(pAccount);
    }

}
