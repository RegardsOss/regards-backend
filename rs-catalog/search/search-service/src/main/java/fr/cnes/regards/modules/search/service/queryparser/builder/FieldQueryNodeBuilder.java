/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.search.service.queryparser.builder;

import java.util.List;
import java.util.stream.Stream;

import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.core.nodes.FieldQueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.apache.lucene.queryparser.flexible.standard.nodes.PointQueryNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.modules.crawler.domain.criterion.BooleanMatchCriterion;
import fr.cnes.regards.modules.crawler.domain.criterion.ICriterion;
import fr.cnes.regards.modules.crawler.domain.criterion.IntMatchCriterion;
import fr.cnes.regards.modules.crawler.domain.criterion.RangeCriterion;
import fr.cnes.regards.modules.crawler.domain.criterion.StringMatchCriterion;
import fr.cnes.regards.modules.models.domain.attributes.AttributeModel;
import fr.cnes.regards.modules.search.service.attributemodel.IAttributeModelService;

/**
 * Builds a {@link BooleanMatchCriterion} from a {@link FieldQueryNode} object when the value is true or false.<br>
 * Builds a {@link StringMatchCriterion} from a {@link FieldQueryNode} object when the value is a String.<br>
 * Builds a {@link IntMatchCriterion} from a {@link PointQueryNode} object when the value is an Integer.<br>
 * Builds a {@link RangeCriterion} from a {@link PointQueryNode} object when the value is a double.<br>
 *
 * @author Marc Sordi
 * @author Xavier-Alexandre Brochard
 */
public class FieldQueryNodeBuilder implements ICriterionQueryBuilder {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(FieldQueryNodeBuilder.class);

    /**
     * Service retrieving the up-to-date list of {@link AttributeModel}s. Autowired by Spring.
     */
    private final IAttributeModelService attributeModelService;

    /**
     * @param pAttributeModelService
     *            Service retrieving the up-to-date list of {@link AttributeModel}s
     */
    public FieldQueryNodeBuilder(IAttributeModelService pAttributeModelService) {
        super();
        attributeModelService = pAttributeModelService;
    }

    @Override
    public ICriterion build(final QueryNode pQueryNode) throws QueryNodeException {
        final FieldQueryNode fieldNode = (FieldQueryNode) pQueryNode;

        final String field = fieldNode.getFieldAsString();
        final String value = fieldNode.getValue().toString();

        List<AttributeModel> attributeModels = attributeModelService.getAttributeModels();

        try (Stream<AttributeModel> stream = attributeModels.stream()) {
            stream.forEach(attributeModel -> LOGGER.debug(attributeModel.toString()));
        }
        // TODO Handle types
        return ICriterion.equals(fieldNode.getFieldAsString(), fieldNode.getTextAsString());
    }

}
