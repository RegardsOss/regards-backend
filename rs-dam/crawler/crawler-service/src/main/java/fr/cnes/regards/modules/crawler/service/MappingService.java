package fr.cnes.regards.modules.crawler.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import fr.cnes.regards.modules.indexer.dao.IEsRepository;
import fr.cnes.regards.modules.indexer.dao.mapping.AttributeDescription;
import fr.cnes.regards.modules.model.domain.ModelAttrAssoc;
import fr.cnes.regards.modules.model.domain.attributes.AttributeModel;
import fr.cnes.regards.modules.model.domain.attributes.AttributeProperty;
import fr.cnes.regards.modules.model.domain.attributes.restriction.RestrictionType;
import fr.cnes.regards.modules.model.service.IModelAttrAssocService;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@Service
public class MappingService implements IMappingService {

    @Autowired
    private IModelAttrAssocService modelAttrAssocService;

    @Autowired
    private IEsRepository esRepos;

    @Override
    public void configureMappings(String tenant, String modelName) {
            List<ModelAttrAssoc> modelAttributes = modelAttrAssocService.getModelAttrAssocs(modelName);
            if(!modelAttributes.isEmpty()) {
                Set<AttributeDescription> mappings = new HashSet<>();
                for (ModelAttrAssoc modelAttribute : modelAttributes) {
                    AttributeModel attribute = modelAttribute.getAttribute();
                    mappings.add(new AttributeDescription("feature." + attribute.getJsonPath(),
                                                          attribute.getType(),
                                                          attribute.hasRestriction() ?
                                                                  attribute.getRestriction().getType() :
                                                                  RestrictionType.NO_RESTRICTION,
                                                          attribute.getProperties().stream().collect(Collectors.toMap(
                                                                  AttributeProperty::getKey,
                                                                  AttributeProperty::getValue))));
                }
                // now lets put the mappings into ES
                esRepos.putMappings(tenant, mappings);
            }
    }
}
