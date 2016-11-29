/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.workflow.account;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.modules.accessrights.dao.instance.IAccountRepository;
import fr.cnes.regards.modules.accessrights.domain.AccountStatus;
import fr.cnes.regards.modules.accessrights.domain.instance.Account;
import fr.cnes.regards.modules.accessrights.registration.OnAcceptAccountEvent;

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
     * Use this to publish the event triggering the verification email on registration
     */
    @Autowired
    private ApplicationEventPublisher eventPublisher;

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
    public void acceptAccount(final Account pAccount, final String pValidationUrl) throws EntityException {
        pAccount.setStatus(AccountStatus.ACCEPTED);
        accountRepository.save(pAccount);

        eventPublisher.publishEvent(new OnAcceptAccountEvent(pAccount, pValidationUrl));
    }

}
