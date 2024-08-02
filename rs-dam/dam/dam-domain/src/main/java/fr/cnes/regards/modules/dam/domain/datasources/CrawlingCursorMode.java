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
package fr.cnes.regards.modules.dam.domain.datasources;

/**
 * The mode of CrawlingCursor. A cursor is used to iterate over products to ingest.
 * This cursor is optimized when used with a last_update property, or with an id property
 * See PM86 for more information about the optimisation
 *
 * @author Thomas GUILLOU
 **/
public enum CrawlingCursorMode {
    /**
     * Every data will be crawled every time the crawler is running
     */
    CRAWL_EVERYTHING,
    /**
     * Crawler will crawl only data where last update is greater than the last update of last data crawled.
     */
    CRAWL_SINCE_LAST_UPDATE,
    /**
     * Crawler will crawl only data where id is greater (or equals depending on plugin configuration)
     * than the last id crawled.
     */
    CRAWL_FROM_LAST_ID
}
