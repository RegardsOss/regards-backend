package fr.cnes.regards.modules.indexer.dao.mapping.handlers;

import fr.cnes.regards.modules.model.domain.event.AttributeModelCreated;
import org.springframework.stereotype.Component;

@Component
public class EsMappingModelAttributeCreatedEventHandler
        extends AbstractEsMappingModelAttributeEventHandler<AttributeModelCreated> {

    @Override Class<AttributeModelCreated> eventType() {
        return AttributeModelCreated.class;
    }
}
