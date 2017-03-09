/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.search.service.queryparser.builder;

import java.util.List;

import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.core.nodes.FieldQueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.apache.lucene.queryparser.flexible.messages.MessageImpl;
import org.apache.lucene.queryparser.flexible.standard.nodes.PointQueryNode;

import fr.cnes.regards.modules.crawler.domain.criterion.ICriterion;
import fr.cnes.regards.modules.crawler.domain.criterion.IntMatchCriterion;
import fr.cnes.regards.modules.crawler.domain.criterion.RangeCriterion;
import fr.cnes.regards.modules.crawler.domain.criterion.StringMatchCriterion;
import fr.cnes.regards.modules.models.domain.attributes.AttributeModel;
import fr.cnes.regards.modules.search.service.attributemodel.IAttributeModelService;
import fr.cnes.regards.modules.search.service.queryparser.RegardsQueryParserMessages;

/**
 * Builds a {@link StringMatchCriterion} from a {@link FieldQueryNode} object when the value is a String.<br>
 * Builds a {@link IntMatchCriterion} from a {@link PointQueryNode} object when the value is an Integer.<br>
 * Builds a {@link RangeCriterion} from a {@link PointQueryNode} object when the value is a double.<br>
 *
 * @author Marc Sordi
 * @author Xavier-Alexandre Brochard
 */
public class FieldQueryNodeBuilder implements ICriterionQueryBuilder {

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

        AttributeModel attributeModel = attributeModels.stream().filter(el -> el.getName().equals(field)).findFirst()
                .orElseThrow(() -> new QueryNodeException(
                        new MessageImpl(RegardsQueryParserMessages.FIELD_TYPE_UNDETERMINATED, field)));

        switch (attributeModel.getType()) {
            case INTEGER:
                return ICriterion.eq(field, Integer.parseInt(value));
            case DOUBLE:
                Double asDouble = Double.parseDouble(value);
                return ICriterion.eq(field, asDouble, asDouble - Math.nextDown(asDouble));
            case LONG:
                Long asLong = Long.parseLong(value);
                return ICriterion.eq(field, asLong, asLong - Math.nextDown(asLong));
            case STRING:
                return ICriterion.equals(field, value);
            default:
                throw new QueryNodeException(
                        new MessageImpl(RegardsQueryParserMessages.UNSUPPORTED_ATTRIBUTE_TYPE, field));
        }
    }

}
