package fr.cnes.regards.modules.processing.event;

import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.modules.processing.dto.ProcessPluginConfigurationRightsDTO;

@lombok.Value
public class RightsPluginConfigurationEvent implements ISubscribable {

    public enum Type {
        CREATE, UPDATE, DELETE
    }

    Type type;
    ProcessPluginConfigurationRightsDTO before;
    ProcessPluginConfigurationRightsDTO after;

}
