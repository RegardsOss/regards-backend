/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.workflow.account;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fr.cnes.regards.modules.accessrights.domain.AccountStatus;
import fr.cnes.regards.modules.accessrights.domain.instance.Account;

/**
 * Provider class returning the right {@link IAccountTransitions} for the passed {@link Account} according to its
 * <code>state</code> field.
 *
 * @author Xavier-Alexandre Brochard
 */
@Component
public class AccountStateProvider {

    /**
     * Pending state
     */
    @Autowired
    private PendingState pendingState;

    /**
     * Accepted state
     */
    @Autowired
    private AcceptedState acceptedState;

    /**
     * Active state
     */
    @Autowired
    private ActiveState activeState;

    /**
     * Inactive state
     */
    @Autowired
    private InactiveState inactiveState;

    /**
     * Locked state
     */
    @Autowired
    private LockedState lockedState;

    /**
     * Get the right account state based on the passed status
     *
     * @param pStatus
     *            The account status
     * @return the account state object
     */
    public IAccountTransitions getState(final AccountStatus pStatus) {
        final IAccountTransitions state;
        switch (pStatus) {
            case PENDING:
                state = pendingState;
                break;
            case ACCEPTED:
                state = acceptedState;
                break;
            case ACTIVE:
                state = activeState;
                break;
            case INACTIVE:
                state = inactiveState;
                break;
            case LOCKED:
                state = lockedState;
                break;
            default:
                state = pendingState;
                break;
        }
        return state;
    }

    /**
     * Get the right account state based on the passed account's status
     *
     * @param pAccount
     *            The account
     * @return the account state object
     */
    public IAccountTransitions getState(final Account pAccount) {
        return getState(pAccount.getStatus());
    }

}
