package fr.cnes.regards.framework.modules.session.commons.service.delete;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.modules.session.commons.domain.events.SessionDeleteEvent;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * @author Iliana Ghazali
 **/
@Component
public class SessionDeleteEventHandler
        implements ApplicationListener<ApplicationReadyEvent>, IHandler<SessionDeleteEvent> {

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private ISubscriber subscriber;

    @Autowired
    private ISessionDeleteService sessionDeleteService;

    private static final Logger LOGGER = LoggerFactory.getLogger(SessionDeleteEventHandler.class);

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        subscriber.subscribeTo(SessionDeleteEvent.class, this);
    }

    @Override
    public void handle(String tenant, SessionDeleteEvent message) {
        runtimeTenantResolver.forceTenant(tenant);
        try {
            String source = message.getSource();
            String session = message.getSession();
            long start = System.currentTimeMillis();

            LOGGER.trace("Handling deleting of session {} from source {} for tenant {}", source, session, tenant);
            sessionDeleteService.deleteSession(message.getSource(), message.getSession());
            LOGGER.trace("Deleting of session {} from source {} for tenant {} handled in {}ms", source, session, tenant,
                         start - System.currentTimeMillis());
        } finally {
            runtimeTenantResolver.clearTenant();

        }
    }
}
