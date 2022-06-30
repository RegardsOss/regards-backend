package fr.cnes.regards.modules.indexer.service;

import fr.cnes.regards.modules.indexer.dao.IEsRepository;
import fr.cnes.regards.modules.indexer.dao.mapping.AttributeDescription;
import fr.cnes.regards.modules.indexer.dao.mapping.utils.AttrDescToJsonMapping;
import fr.cnes.regards.modules.model.domain.ModelAttrAssoc;
import fr.cnes.regards.modules.model.domain.attributes.AttributeModel;
import fr.cnes.regards.modules.model.domain.attributes.AttributeProperty;
import fr.cnes.regards.modules.model.domain.attributes.restriction.JsonSchemaRestriction;
import fr.cnes.regards.modules.model.domain.attributes.restriction.RestrictionType;
import fr.cnes.regards.modules.model.dto.properties.PropertyType;
import fr.cnes.regards.modules.model.gson.AbstractAttributeHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@Service
public class MappingService implements IMappingService {

    @Autowired
    private IEsRepository esRepos;

    @Override
    public void configureMappings(String tenant, List<ModelAttrAssoc> modelAttributes) {
        if (!modelAttributes.isEmpty()) {
            Set<AttributeDescription> mappings = new HashSet<>();
            for (ModelAttrAssoc modelAttribute : modelAttributes) {
                AttributeModel attribute = modelAttribute.getAttribute();
                // If attribute is JSON type, then generate virtual attributes from given jsonSchema to create mapping.
                if ((attribute.getType() == PropertyType.JSON)
                    && (RestrictionType.JSON_SCHEMA.equals(attribute.getRestrictionType())
                        && attribute.getEsMapping() == null)) {
                    JsonSchemaRestriction restriction = (JsonSchemaRestriction) attribute.getRestriction();
                    AbstractAttributeHelper.fromJsonSchema(attribute.getJsonPropertyPath(),
                                                           restriction.getJsonSchema(),
                                                           attribute.isIndexed(),
                                                           new ArrayList<>(restriction.getIndexableFields()))
                                           .forEach(a -> mappings.add(new AttributeDescription("feature."
                                                                                               + a.getJsonPath(),
                                                                                               a.getType(),
                                                                                               a.hasRestriction() ?
                                                                                                   a.getRestrictionType() :
                                                                                                   RestrictionType.NO_RESTRICTION,
                                                                                               a.getProperties()
                                                                                                .stream()
                                                                                                .collect(Collectors.toMap(
                                                                                                    AttributeProperty::getKey,
                                                                                                    AttributeProperty::getValue)),
                                                                                               a.getEsMapping(),
                                                                                               a.isIndexed())));
                } else {
                    Map<String, String> descriptionProperties = attribute.getProperties()
                                                                         .stream()
                                                                         .collect(Collectors.toMap(AttributeProperty::getKey,
                                                                                                   AttributeProperty::getValue));
                    descriptionProperties.put(AttrDescToJsonMapping.ELASTICSEARCH_MAPPING_PROP_NAME,
                                              attribute.getEsMapping());
                    mappings.add(new AttributeDescription("feature." + attribute.getJsonPath(),
                                                          attribute.getType(),
                                                          attribute.hasRestriction() ?
                                                              attribute.getRestrictionType() :
                                                              RestrictionType.NO_RESTRICTION,
                                                          descriptionProperties,
                                                          attribute.getEsMapping(),
                                                          attribute.isIndexed()));
                }
            }
            // now lets put the mappings into ES
            esRepos.putMappings(tenant, mappings);
        }
    }
}
