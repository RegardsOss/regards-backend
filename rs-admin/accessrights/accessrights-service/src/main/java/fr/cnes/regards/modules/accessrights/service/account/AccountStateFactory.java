/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.service.account;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fr.cnes.regards.modules.accessrights.domain.AccountStatus;
import fr.cnes.regards.modules.accessrights.domain.instance.Account;

/**
 * Factory class returning the right {@link IAccountTransitions} for the passed {@link Account} according to its
 * <code>state</code> field.
 *
 * @author Xavier-Alexandre Brochard
 */
@Component
public class AccountStateFactory {

    /**
     * Pending state
     */
    @Autowired
    private PendingState pendingState;

    /**
     * Active state
     */
    @Autowired
    private ActiveState activeState;

    /**
     * Accepted state
     */
    @Autowired
    private AcceptedState acceptedState;

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
     * Creates the right account state based on the passed status
     *
     * @param pStatus
     *            The account status
     * @return the account state object
     */
    public IAccountTransitions createState(final AccountStatus pStatus) {
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
     * Creates the right account state based on the passed account's status
     *
     * @param pAccount
     *            The account
     * @return the account state object
     */
    public IAccountTransitions createState(final Account pAccount) {
        return createState(pAccount.getStatus());
    }

}
