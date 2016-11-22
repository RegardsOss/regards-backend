/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.service.account;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import fr.cnes.regards.framework.module.rest.exception.EntityAlreadyExistsException;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.framework.module.rest.exception.EntityTransitionForbiddenException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.accessrights.dao.instance.IAccountRepository;
import fr.cnes.regards.modules.accessrights.domain.AccessRequestDTO;
import fr.cnes.regards.modules.accessrights.domain.AccountStatus;
import fr.cnes.regards.modules.accessrights.domain.instance.Account;
import fr.cnes.regards.modules.accessrights.domain.instance.AccountSettings;

/**
 * Class managing the workflow of an account by applying the right transitions according to its status.<br>
 * Proxies the transition methods by instanciating the right state class (ActiveState, LockedState...).
 *
 * @author Xavier-Alexandre Brochard
 * @since 1.1-SNAPSHOT
 */
@Service
@Primary
public class AccountWorkflowManager implements IAccountTransitions {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(AbstractDeletableState.class);

    /**
     * Class providing the right state (i.e. implementation of the {@link IAccountTransitions}) according to the account
     * status
     */
    private final AccountStateProvider accountStateProvider;

    /**
     * CRUD repository handling {@link Account}s. Autowired by Spring.
     */
    private final IAccountRepository accountRepository;

    /**
     * Account Settings Repository. Autowired by Spring.
     */
    private final IAccountSettingsService accountSettingsService;

    /**
     * Constructor
     *
     * @param pAccountStateProvider
     *            the state factory
     * @param pAccountRepository
     *            the account repository * @param pAccountSettingsService the account settings repository
     */
    public AccountWorkflowManager(final AccountStateProvider pAccountStateProvider,
            final IAccountRepository pAccountRepository, final IAccountSettingsService pAccountSettingsService) {
        super();
        accountStateProvider = pAccountStateProvider;
        accountRepository = pAccountRepository;
        accountSettingsService = pAccountSettingsService;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * fr.cnes.regards.modules.accessrights.service.account.IAccountTransitions#requestAccount(fr.cnes.regards.modules.
     * accessrights.domain.AccessRequestDTO)
     */
    @Override
    public Account requestAccount(final AccessRequestDTO pDto) throws EntityAlreadyExistsException {
        // Check existence
        if (accountRepository.findOneByEmail(pDto.getEmail()).isPresent()) {
            throw new EntityAlreadyExistsException("The email " + pDto.getEmail() + "is already in use.");
        }
        // Create the new account
        final Account account = new Account(pDto.getEmail(), pDto.getFirstName(), pDto.getLastName(),
                pDto.getPassword());
        // Check status
        Assert.isTrue(AccountStatus.PENDING.equals(account.getStatus()),
                      "Trying to create an Account with other status than PENDING.");

        // Save
        accountRepository.save(account);

        // Auto-accept if configured so
        final AccountSettings settings = accountSettingsService.retrieve();
        if ("auto-accept".equals(settings.getMode())) {
            try {
                makeAdminDecision(account, true);
            } catch (final EntityTransitionForbiddenException e) {
                LOG.error("Error on auto-acceptance: ", e);
            }
        }

        return account;
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.accessrights.service.account.IAccountTransitions#makeAdminDecision(fr.cnes.regards.
     * modules. accessrights.domain.instance.Account)
     */
    @Override
    public void makeAdminDecision(final Account pAccount, final boolean pAccepted)
            throws EntityTransitionForbiddenException {
        accountStateProvider.getState(pAccount).makeAdminDecision(pAccount, pAccepted);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * fr.cnes.regards.modules.accessrights.service.account.IAccountTransitions#emailValidation(fr.cnes.regards.modules.
     * accessrights.domain.instance.Account)
     */
    @Override
    public void emailValidation(final Account pAccount) throws EntityTransitionForbiddenException {
        accountStateProvider.getState(pAccount).emailValidation(pAccount);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * fr.cnes.regards.modules.accessrights.service.account.IAccountTransitions#lockAccount(fr.cnes.regards.modules.
     * accessrights.domain.instance.Account)
     */
    @Override
    public void lockAccount(final Account pAccount) throws EntityTransitionForbiddenException {
        accountStateProvider.getState(pAccount).lockAccount(pAccount);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * fr.cnes.regards.modules.accessrights.service.account.IAccountTransitions#unlockAccount(fr.cnes.regards.modules.
     * accessrights.domain.instance.Account)
     */
    @Override
    public void unlockAccount(final Account pAccount, final String pUnlockCode)
            throws EntityOperationForbiddenException {
        accountStateProvider.getState(pAccount).unlockAccount(pAccount, pUnlockCode);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * fr.cnes.regards.modules.accessrights.service.account.IAccountTransitions#inactiveAccount(fr.cnes.regards.modules.
     * accessrights.domain.instance.Account)
     */
    @Override
    public void inactiveAccount(final Account pAccount) throws EntityTransitionForbiddenException {
        accountStateProvider.getState(pAccount).inactiveAccount(pAccount);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * fr.cnes.regards.modules.accessrights.service.account.IAccountTransitions#activeAccount(fr.cnes.regards.modules.
     * accessrights.domain.instance.Account)
     */
    @Override
    public void activeAccount(final Account pAccount) throws EntityTransitionForbiddenException {
        accountStateProvider.getState(pAccount).activeAccount(pAccount);
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.accessrights.service.account.IAccountTransitions#delete(fr.cnes.regards.modules.
     * accessrights. domain.instance.Account)
     */
    @Override
    public void delete(final Account pAccount) throws ModuleException {
        accountStateProvider.getState(pAccount).delete(pAccount);
    }

}
