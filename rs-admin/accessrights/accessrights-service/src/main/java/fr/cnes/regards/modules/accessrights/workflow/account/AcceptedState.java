/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.workflow.account;

import java.time.LocalDateTime;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.modules.accessrights.dao.instance.IAccountRepository;
import fr.cnes.regards.modules.accessrights.domain.AccountStatus;
import fr.cnes.regards.modules.accessrights.domain.instance.Account;
import fr.cnes.regards.modules.accessrights.domain.registration.VerificationToken;
import fr.cnes.regards.modules.accessrights.workflow.account.event.OnAccountEmailValidationEvent;

/**
 * State class of the State Pattern implementing the available actions on a {@link Account} in status ACCEPTED.
 *
 * @author Xavier-Alexandre Brochard
 * @author Christophe Mertz
 */
@Component
@Transactional
public class AcceptedState implements IAccountTransitions {

    /**
     * Account repository
     */
    private final IAccountRepository accountRepository;

    /**
     * Use this to publish Spring application events
     */
    private final ApplicationEventPublisher eventPublisher;

    /**
     * @param pAccountRepository Account repository. Autowired by Spring. Must not be null.
     * @param pEventPublisher Use this to publish Spring application events. Auowired by Spring. Must not be null.
     */
    public AcceptedState(IAccountRepository pAccountRepository, ApplicationEventPublisher pEventPublisher) {
        super();
        Assert.notNull(pAccountRepository);
        Assert.notNull(pEventPublisher);
        accountRepository = pAccountRepository;
        eventPublisher = pEventPublisher;
    }

    @Override
    public void validateAccount(final VerificationToken pVerificationToken) throws EntityOperationForbiddenException {
        final Account account = pVerificationToken.getAccount();

        if (pVerificationToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new EntityOperationForbiddenException(account.getEmail(), Account.class,
                    "Verification token has expired");
        }

        account.setStatus(AccountStatus.ACTIVE);
        eventPublisher.publishEvent(new OnAccountEmailValidationEvent(account.getEmail()));
        accountRepository.save(account);
    }

}
