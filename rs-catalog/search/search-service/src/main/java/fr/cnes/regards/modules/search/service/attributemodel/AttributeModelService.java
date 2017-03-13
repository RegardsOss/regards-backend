/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.search.service.attributemodel;

import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.modules.models.domain.attributes.AttributeModel;

/**
 * {@link IAttributeModelService} implementation
 *
 * @author Xavier-Alexandre Brochard
 */
@Service
public class AttributeModelService implements IAttributeModelService {

    /**
     * Feign client for rs-dam {@link AttributeModel} controller. Autowired by Spring.
     */
    private final IAttributeModelClient attributeModelClient;

    /**
     * Creates a new instance of the service with passed services/repos
     *
     * @param pAttributeModelClient
     *            Service returning the list of attribute models and keeping the list up-to-date
     */
    public AttributeModelService(IAttributeModelClient pAttributeModelClient) {
        super();
        attributeModelClient = pAttributeModelClient;
        // Add caching
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * fr.cnes.regards.modules.search.service.attributemodel.IAttributeModelService#getAttributeModelByName(java.lang.
     * String)
     */
    @Override
    public AttributeModel getAttributeModelByName(String pName) throws EntityNotFoundException {
        return attributeModelClient.getAttributeModels().stream().filter(el -> el.getName().equals(pName)).findFirst()
                .orElseThrow(() -> new EntityNotFoundException(pName, AttributeModel.class));
    }

}
