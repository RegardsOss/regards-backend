package fr.cnes.regards.modules.indexer.dao.mapping.handlers;

import fr.cnes.regards.modules.model.domain.event.AttributeModelUpdated;
import org.springframework.stereotype.Component;

@Component
public class EsMappingModelAttributeUpdatedEventHandler
        extends AbstractEsMappingModelAttributeEventHandler<AttributeModelUpdated> {

    @Override Class<AttributeModelUpdated> eventType() {
        return AttributeModelUpdated.class;
    }
}

