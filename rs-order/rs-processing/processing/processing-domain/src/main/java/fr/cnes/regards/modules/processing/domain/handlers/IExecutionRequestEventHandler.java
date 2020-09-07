package fr.cnes.regards.modules.processing.domain.handlers;

import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.modules.processing.domain.events.PExecutionRequestEvent;

public interface IExecutionRequestEventHandler extends IHandler<PExecutionRequestEvent> {

}
