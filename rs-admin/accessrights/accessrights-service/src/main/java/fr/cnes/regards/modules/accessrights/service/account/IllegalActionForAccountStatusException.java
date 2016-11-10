/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.service.account;

import fr.cnes.regards.framework.module.rest.exception.OperationForbiddenException;
import fr.cnes.regards.modules.accessrights.domain.instance.Account;

/**
 * Exception thrown when an illegal action is called on an {@link Account}.<br>
 * The legal actions are defined in the state diagram of the conception document.
 *
 * @see SGDS-CP-12200-0010-CS p. 71
 *
 * @author Xavier-Alexandre Brochard
 * @since 1.1-SNAPSHOT
 */
public class IllegalActionForAccountStatusException extends OperationForbiddenException {

    /**
     * Serial version
     *
     * @since 1.1-SNAPSHOT
     */
    private static final long serialVersionUID = -7255117056559968468L;

    /**
     * Creates a new exception with passed params
     *
     * @param pAccount
     *            the account on which was illegally called the action
     * @param pMethodName
     *            the illegally called method's name
     * @since 1.1-SNAPSHOT
     */
    public IllegalActionForAccountStatusException(final Account pAccount, final String pMethodName) {
        super("todo id", Account.class, "Method " + pMethodName + " was called, whereas the account status is "
                + pAccount.getStatus().toString());
    }

}