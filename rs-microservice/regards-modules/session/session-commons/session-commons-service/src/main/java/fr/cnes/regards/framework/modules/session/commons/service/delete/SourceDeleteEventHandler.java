package fr.cnes.regards.framework.modules.session.commons.service.delete;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.modules.session.commons.domain.events.SourceDeleteEvent;
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
public class SourceDeleteEventHandler
        implements ApplicationListener<ApplicationReadyEvent>, IHandler<SourceDeleteEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SourceDeleteEventHandler.class);

    @Autowired
    private ISubscriber subscriber;

    @Autowired
    private ISourceDeleteService sourceDeleteService;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        subscriber.subscribeTo(SourceDeleteEvent.class, this);
    }

    @Override
    public void handle(String tenant, SourceDeleteEvent message) {
        String source = message.getSource();
        long start = System.currentTimeMillis();

        LOGGER.trace("Handling deleting of source {} for tenant {}", source, tenant);
        sourceDeleteService.deleteSource(source);
        LOGGER.trace("Deleting of source {} for tenant {} handled in {}ms", source, tenant,
                 start - System.currentTimeMillis());

    }
}
