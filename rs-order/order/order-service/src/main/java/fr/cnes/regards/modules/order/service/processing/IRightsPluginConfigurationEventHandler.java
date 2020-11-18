package fr.cnes.regards.modules.order.service.processing;

import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.modules.processing.event.RightsPluginConfigurationEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

public interface IRightsPluginConfigurationEventHandler
        extends ApplicationListener<ApplicationEvent>, IHandler<RightsPluginConfigurationEvent> {}
