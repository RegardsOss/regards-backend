/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.extension;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.modules.indexer.domain.criterion.ICriterion;
import fr.cnes.regards.modules.opensearch.service.exception.OpenSearchUnknownParameter;
import fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.exception.UnsupportedCriterionOperator;

public abstract class AbstractOpenSearchExtension implements IOpenSearchExtension {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractOpenSearchExtension.class);

    /**
     * Does the current extension activated ?
     */
    private boolean activated = false;

    @Override
    public ICriterion buildCriterion(List<SearchParameter> parameters) {
        List<ICriterion> criterion = new ArrayList<>();
        for (SearchParameter parameter : parameters) {
            try {
                if (supportsSearchParameter(parameter)) {
                    ICriterion crit = buildCriteria(parameter);
                    if (crit != null) {
                        criterion.add(crit);
                    }
                }
            } catch (UnsupportedCriterionOperator e) {
                LOGGER.warn(e.getMessage(), e);
            }
        }
        return criterion.isEmpty() ? ICriterion.all() : ICriterion.and(criterion);
    }

    @Override
    public boolean isActivated() {
        return activated;
    }

    public void setActivated(boolean activated) {
        this.activated = activated;
    }

    /**
     * Build a {@link ICriterion} for the given {@link attributeModel} and the given values.
     * @param attributeModel {@link attributeModel}
     * @param values {@link String}s
     * @return {@link ICriterion}
     * @throws OpenSearchUnknownParameter
     */
    protected abstract ICriterion buildCriteria(SearchParameter parameter) throws UnsupportedCriterionOperator;

    /**
     * Does the current extension can handle search for the given configured parameter.
     * @param parameter {@link SearchParameter}
     * @return {@link boolean}
     */
    protected abstract boolean supportsSearchParameter(SearchParameter parameter);

}
