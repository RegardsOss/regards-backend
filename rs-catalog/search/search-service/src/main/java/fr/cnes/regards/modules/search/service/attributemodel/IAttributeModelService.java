/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.search.service.attributemodel;

import java.util.List;

import fr.cnes.regards.modules.models.domain.attributes.AttributeModel;

/**
 * Attribute model service interface.<br>
 * Calls the AttributeModel feign client in order to retrieve the list and subscribes the RabbitMQ in order to keep the
 * cached result up-to-date.
 *
 * @author Xavier-Alexandre Brochard
 */
public interface IAttributeModelService {

    /**
     * @return the list of current attribute models
     */
    List<AttributeModel> getAttributeModels();
}
