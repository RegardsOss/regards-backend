/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.service.instance;

import org.springframework.stereotype.Component;

import fr.cnes.regards.modules.accessrights.dao.instance.IAccountRepository;
import fr.cnes.regards.modules.accessrights.domain.AccountStatus;
import fr.cnes.regards.modules.accessrights.domain.instance.Account;
import fr.cnes.regards.modules.accessrights.domain.instance.AccountSettings;

/**
 * State class of the State Pattern implementing the available actions on a {@link Account} in status PENDING.
 *
 * @author Xavier-Alexandre Brochard
 * @since 1.1-SNAPSHOT
 */
@Component
class PendingState implements IAccountState {

    /**
     * Account Repository. Autowired by Spring.
     */
    private final IAccountRepository accountRepository;

    /**
     * Account Settings Repository. Autowired by Spring.
     */
    private final IAccountSettingsService accountSettingsService;

    /**
     * Creates a new PENDING state
     *
     * @param pAccountRepository
     *            the account repository
     * @param pAccountSettingsService
     *            the account settings repository
     */
    public PendingState(final IAccountRepository pAccountRepository,
            final IAccountSettingsService pAccountSettingsService) {
        super();
        accountRepository = pAccountRepository;
        accountSettingsService = pAccountSettingsService;
    }

    @Override
    public void makeAdminDecision(final Account pAccount) {
        final AccountSettings settings = accountSettingsService.retrieve();

        final boolean accepted = true;

        if (accepted) {
            pAccount.setStatus(AccountStatus.ACTIVE);
            accountRepository.save(pAccount);
        } else {
            accountRepository.delete(pAccount);
        }
    }

}
