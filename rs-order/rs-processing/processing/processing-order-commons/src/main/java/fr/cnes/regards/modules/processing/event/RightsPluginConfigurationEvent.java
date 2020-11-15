package fr.cnes.regards.modules.processing.event;

import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.JsonMessageConverter;
import fr.cnes.regards.framework.amqp.event.Target;
import fr.cnes.regards.modules.processing.dto.ProcessPluginConfigurationRightsDTO;

@Event(target = Target.ONE_PER_MICROSERVICE_TYPE, converter = JsonMessageConverter.GSON)
@lombok.Value
public class RightsPluginConfigurationEvent implements ISubscribable {

    public enum Type {
        CREATE, UPDATE, DELETE
    }

    Type type;
    ProcessPluginConfigurationRightsDTO before;
    ProcessPluginConfigurationRightsDTO after;

}
