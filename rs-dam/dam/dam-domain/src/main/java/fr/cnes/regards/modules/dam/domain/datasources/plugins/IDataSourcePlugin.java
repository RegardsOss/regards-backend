/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

package fr.cnes.regards.modules.dam.domain.datasources.plugins;

import fr.cnes.regards.framework.modules.plugins.annotations.PluginInterface;
import fr.cnes.regards.modules.dam.domain.datasources.CrawlingCursor;
import fr.cnes.regards.modules.dam.domain.datasources.CrawlingCursorMode;
import fr.cnes.regards.modules.dam.domain.entities.feature.DataObjectFeature;

import jakarta.annotation.Nullable;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Class IDataSourcePlugin
 * <p>
 * Allows to search in a data source,
 *
 * @author Christophe Mertz
 * @author oroussel
 */
@PluginInterface(description = "Plugin to search in a data source")
public interface IDataSourcePlugin {

    /**
     * The model name
     *
     * @return model name
     */
    String getModelName();

    /**
     * The refresh rate of the data source
     *
     * @return the refresh rate value (in seconds)
     */
    int getRefreshRate();

    /**
     * Returns a {@link List} of new entities meeting the paging restriction provided in the {@code Pageable} object.
     *
     * @param tenant                    tenant to build URN
     * @param cursor                    indexation position information
     * @param lastIngestDate            Last ingestion process date
     * @param currentIngestionStartDate Current ingestion start process date
     * @return a page of entities
     * @throws DataSourceException in case anything wrong happened
     */
    List<DataObjectFeature> findAll(String tenant,
                                    CrawlingCursor cursor,
                                    @Nullable OffsetDateTime lastIngestDate,
                                    OffsetDateTime currentIngestionStartDate) throws DataSourceException;

    /**
     * @return a duration in second which indicates to the service an overlapping time
     * to be respected to avoid data loss.
     * <p>
     * Explanation : harvesting done at the same time as ingestion can cause data loss
     * due to the random duration of the transactions.
     */
    default long getOverlap() {
        return 0;
    }

    /**
     * @return the mode of crawling cursor. It used to determine how to crawl next pages of datasource, in case of
     * external bd crawling.
     */
    default CrawlingCursorMode getCrawlingCursorMode() {
        return CrawlingCursorMode.CRAWL_SINCE_LAST_UPDATE;
    }

}
