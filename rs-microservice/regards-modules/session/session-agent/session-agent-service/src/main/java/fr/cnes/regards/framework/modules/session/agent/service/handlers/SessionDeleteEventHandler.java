package fr.cnes.regards.framework.modules.session.agent.service.handlers;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.modules.session.commons.domain.events.SessionDeleteEvent;
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

    private static final Logger LOGGER = LoggerFactory.getLogger(SessionDeleteEventHandler.class);

    @Autowired
    private ISubscriber subscriber;

    @Autowired
    private ISessionDeleteService sessionDeleteService;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        subscriber.subscribeTo(SessionDeleteEvent.class, this);
    }

    @Override
    public void handle(String tenant, SessionDeleteEvent message) {
        String source = message.getSource();
        String session = message.getSession();
        long start = System.currentTimeMillis();

        LOGGER.debug("Handling deleting of session {} from source {} for tenant {}", source, session, tenant);
        sessionDeleteService.deleteSession(message.getSource(), message.getSession());
        LOGGER.debug("Deleting of session {} from source {} for tenant {} handled in {}ms", source, session, tenant,
                     start - System.currentTimeMillis());

    }
}
