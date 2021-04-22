package fr.cnes.regards.modules.accessrights.instance.domain;

import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.Target;
import fr.cnes.regards.framework.amqp.event.WorkerMode;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@Event(mode = WorkerMode.BROADCAST, target = Target.ONE_PER_MICROSERVICE_TYPE)
public class AccountAcceptedEvent implements ISubscribable {

    private String accountEmail;

    public AccountAcceptedEvent() {}

    public AccountAcceptedEvent(Account account) {
        this.accountEmail = account.getEmail();
    }

    public String getAccountEmail() {
        return accountEmail;
    }

    public void setAccountEmail(String accountEmail) {
        this.accountEmail = accountEmail;
    }
}
