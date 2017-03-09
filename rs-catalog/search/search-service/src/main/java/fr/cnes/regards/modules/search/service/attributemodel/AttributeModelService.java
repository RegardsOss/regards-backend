/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.search.service.attributemodel;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import fr.cnes.regards.modules.models.domain.attributes.AttributeModel;

/**
 * {@link IAttributeModelService} implementation
 *
 * @author Xavier-Alexandre Brochard
 */
@Service
public class AttributeModelService implements IAttributeModelService {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(AttributeModelService.class);
    //
    // /**
    // * Feign client for rs-dam {@link AttributeModel} controller. Autowired by Spring.
    // */
    // private final IAttributeModelClient attributeModelClient;
    //
    // /**
    // * Creates a new instance of the service with passed services/repos
    // *
    // * @param pAttributeModelClient
    // * Service returning the list of attribute models and keeping the list up-to-date
    // */
    // public AttributeModelService(IAttributeModelClient pAttributeModelClient) {
    // super();
    // attributeModelClient = pAttributeModelClient;
    // }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.search.service.IAttributeModelService#getAttributeModels()
     */
    @Override
    public List<AttributeModel> getAttributeModels() {
        // TODO Auto-generated method stub
        return null;
    }

}
