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
package fr.cnes.regards.modules.indexer.dao;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.search.ClearScrollResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Sébastien Binda
 **/
public class DefaultScrollClearResponseActionListener implements ActionListener<ClearScrollResponse> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EsRepository.class);

    @Override
    public void onResponse(ClearScrollResponse clearScrollResponse) {
        LOGGER.debug("Elasticsearch scroll request cleared successfully");
    }

    @Override
    public void onFailure(Exception e) {
        LOGGER.error(String.format("Elasticsearch scroll request clear error. Cause : %s", e.getMessage()), e);
    }
}
