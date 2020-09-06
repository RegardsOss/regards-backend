package fr.cnes.regards.modules.processing.service.handlers;

import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.modules.processing.events.PExecutionRequestEvent;

public interface IExecutionRequestEventHandler extends IHandler<PExecutionRequestEvent> {

}
