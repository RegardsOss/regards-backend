/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.opensearch.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import fr.cnes.regards.modules.indexer.domain.criterion.ICriterion;
import fr.cnes.regards.modules.opensearch.service.cache.attributemodel.IAttributeFinder;
import fr.cnes.regards.modules.opensearch.service.exception.OpenSearchParseException;
import fr.cnes.regards.modules.opensearch.service.parser.CircleParser;
import fr.cnes.regards.modules.opensearch.service.parser.FieldExistsParser;
import fr.cnes.regards.modules.opensearch.service.parser.GeometryParser;
import fr.cnes.regards.modules.opensearch.service.parser.IParser;
import fr.cnes.regards.modules.opensearch.service.parser.QueryParser;

/**
 * Parses generic OpenSearch requests like <code>q={searchTerms}&lat={geo:lat?}&lon={geo:lon?}&r={geo:radius?}&g=POLYGON((0.582%2040.496%2C%200.231%2040.737%2C%200.736%2042.869%2C%203.351%2042.386%2C%203.263%2041.814%2C%202.164%2041.265%2C%200.978%20%20%2040.957%2C%200.802%2040.781%2C%200.978%2040.649%2C%200.582%2040.496))</code>
 * <p>
 * It is coded so that you can add as many parsers as you want, each handling a specific part of the request.
 * You just need to implement a new {@link IParser}, and register it in the <code>aggregate</code> method.
 * @author Xavier-Alexandre Brochard
 */
@Service
public class OpenSearchService implements IOpenSearchService {

    // Thread safe parsers holder
    private static ThreadLocal<List<IParser>> parsersHolder;

    private final IAttributeFinder finder;

    public OpenSearchService(IAttributeFinder finder) {
        this.finder = finder;
        this.parsersHolder = ThreadLocal.withInitial(() -> Lists
                .newArrayList(new QueryParser(finder), new GeometryParser(), new CircleParser(),
                              new FieldExistsParser()));
    }

    @Override
    public ICriterion parse(Map<String, String> queryParameters) throws OpenSearchParseException {
        List<ICriterion> criteria = new ArrayList<>();
        for (IParser parser : parsersHolder.get()) {
            // Parse parameters ... may return null if parser required parameter(s) not set
            ICriterion crit = parser.parse(queryParameters);
            if (crit != null) {
                criteria.add(crit);
            }
        }
        return criteria.isEmpty() ? null : ICriterion.and(criteria);
    }

}
