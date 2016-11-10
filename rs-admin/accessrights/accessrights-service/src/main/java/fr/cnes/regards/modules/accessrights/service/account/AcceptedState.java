/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.service.account;

import org.springframework.stereotype.Component;

import fr.cnes.regards.modules.accessrights.domain.instance.Account;

/**
 * State class of the State Pattern implementing the available actions on a {@link Account} in status ACCEPTED.
 *
 * @author Xavier-Alexandre Brochard
 */
@Component
public class AcceptedState implements IAccountState {

    @Override
    public void emailValidation(final Account pAccount) throws IllegalActionForAccountStatusException {
        // TODO Auto-generated method stub
    }

}
