package fr.cnes.regards.modules.order.service.processing;

import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.modules.processing.domain.events.PExecutionResultEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

public interface IProcessingExecutionResultEventHandler extends ApplicationListener<ApplicationEvent>, IHandler<PExecutionResultEvent> {}
