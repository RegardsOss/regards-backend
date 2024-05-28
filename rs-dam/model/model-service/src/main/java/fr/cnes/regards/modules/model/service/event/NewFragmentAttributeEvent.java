package fr.cnes.regards.modules.model.service.event;

import fr.cnes.regards.modules.model.domain.attributes.AttributeModel;
import org.springframework.context.ApplicationEvent;

/**
 * Event navigating inside the microservice instance to be notified when a new attribute is created on a fragment so we can handle this properly in IModelAttrAssocService.
 * AttributeModel is into source attribute from inherited classes
 *
 * @author Sylvain VISSIERE-GUERINET
 */

public class NewFragmentAttributeEvent extends ApplicationEvent {

    /**
     * Constructor
     */
    public NewFragmentAttributeEvent(AttributeModel attributeAdded) {
        super(attributeAdded);
    }

}
