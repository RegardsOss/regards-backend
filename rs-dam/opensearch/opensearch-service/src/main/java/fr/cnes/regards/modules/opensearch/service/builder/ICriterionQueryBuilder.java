/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import fr.cnes.regards.modules.indexer.domain.criterion.ICriterion;
import fr.cnes.regards.modules.indexer.domain.criterion.ICriterion;
import fr.cnes.regards.modules.indexer.domain.criterion.StringMatchType;
import fr.cnes.regards.modules.opensearch.service.parser.QueryParser;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.core.builders.QueryBuilder;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.util.Pair;

/**
 * @author Marc Sordi
 */
@FunctionalInterface
public interface ICriterionQueryBuilder extends QueryBuilder {

    Logger LOGGER = LoggerFactory.getLogger(ICriterionQueryBuilder.class);

    @Override
    ICriterion build(QueryNode pQueryNode) throws QueryNodeException;

    /**
     * If field contains {@link QueryParser#STRING_MATCH_TYPE_SEPARATOR},
     * retrieve real field name and extract string matching behavior.<br/>
     * If not, just return field parameter as is!<br/>
     * Parsing must be done before finding related attribute to get the real attribute name to find.
     */
    default Pair<String, StringMatchType> parse(String field) {
        if (field.contains(QueryParser.STRING_MATCH_TYPE_SEPARATOR)) {
            String[] fieldParts = field.split(QueryParser.STRING_MATCH_TYPE_SEPARATOR);
            StringMatchType matchType;
            try {
                matchType = StringMatchType.valueOf(fieldParts[1]);
            } catch (Exception ex) {
                // Default behavior
                matchType = StringMatchType.KEYWORD;
                LOGGER.warn(
                        "Criterion builder cannot detect string matching behavior with field {} and behavior {}. Falling back to {}!",
                        field, fieldParts[1], matchType);
            }
            return Pair.of(fieldParts[0], matchType);
        }
        // Default behavior
        return Pair.of(field, StringMatchType.KEYWORD);
    }
}
