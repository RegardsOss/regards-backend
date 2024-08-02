/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.modules.opensearch.service.builder;

import fr.cnes.regards.modules.dam.domain.entities.criterion.IFeatureCriterion;
import fr.cnes.regards.modules.indexer.domain.criterion.ICriterion;
import fr.cnes.regards.modules.indexer.domain.criterion.StringMatchCriterion;
import fr.cnes.regards.modules.indexer.domain.criterion.StringMatchType;
import fr.cnes.regards.modules.model.domain.attributes.AttributeModel;
import fr.cnes.regards.modules.opensearch.service.cache.attributemodel.IAttributeFinder;
import fr.cnes.regards.modules.opensearch.service.exception.OpenSearchUnknownParameter;
import fr.cnes.regards.modules.opensearch.service.parser.QueryParser;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.apache.lucene.queryparser.flexible.messages.MessageImpl;
import org.apache.lucene.queryparser.flexible.standard.nodes.RegexpQueryNode;
import org.springframework.data.util.Pair;

import java.util.Set;

/**
 * Builds a {@link StringMatchCriterion} type REGEXP from a {@link RegexpQueryNode} object<br>
 *
 * @author Sébastien Binda
 */
public class RegexpQueryNodeBuilder implements ICriterionQueryBuilder {

    /**
     * Service retrieving the up-to-date list of {@link AttributeModel}s. Autowired by Spring.
     */
    private final IAttributeFinder finder;

    /**
     * @param finder Service permitting to retrieve up-to-date list of {@link AttributeModel}s
     */
    public RegexpQueryNodeBuilder(IAttributeFinder finder) {
        super();
        this.finder = finder;
    }

    @Override
    public ICriterion build(final QueryNode queryNode) throws QueryNodeException {
        RegexpQueryNode wildcardNode = (RegexpQueryNode) queryNode;

        String field = wildcardNode.getFieldAsString();
        String value = wildcardNode.getText().toString();

        // Manage multisearch
        if (QueryParser.MULTISEARCH.equals(field)) {
            try {
                Set<AttributeModel> atts = MultiSearchHelper.discoverFields(finder, value);
                return IFeatureCriterion.multiMatchStartWith(atts, value);
            } catch (OpenSearchUnknownParameter e) {
                throw new QueryNodeException(new MessageImpl(fr.cnes.regards.modules.opensearch.service.message.QueryParserMessages.FIELD_TYPE_UNDETERMINATED,
                                                             e.getMessage()), e);
            }
        }

        // Detect string matching behavior before finding related attribute to get real attribute name
        Pair<String, StringMatchType> fieldAndMatchType = IFeatureCriterion.parse(field);

        AttributeModel attributeModel;
        try {
            attributeModel = finder.findByName(fieldAndMatchType.getFirst());
        } catch (OpenSearchUnknownParameter e) {
            throw new QueryNodeException(new MessageImpl(fr.cnes.regards.modules.opensearch.service.message.QueryParserMessages.FIELD_TYPE_UNDETERMINATED,
                                                         e.getMessage()), e);
        }

        return IFeatureCriterion.regexp(attributeModel, value, fieldAndMatchType.getSecond());
    }

}
