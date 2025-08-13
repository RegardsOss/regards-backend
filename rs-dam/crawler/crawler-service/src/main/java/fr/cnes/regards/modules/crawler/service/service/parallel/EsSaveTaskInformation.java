/*
 * Copyright 2017-2025 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
 * along with REGARDS. If not, see `<http://www.gnu.org/licenses/>`.
 */
package fr.cnes.regards.modules.crawler.service.service.parallel;

import fr.cnes.regards.modules.dam.domain.datasources.CrawlingCursor;
import fr.cnes.regards.modules.indexer.dao.BulkSaveResult;

import java.util.concurrent.Future;

/**
 * This record holds information about the save task, including the cursor and the bulk save result
 *
 * @param cursor               the cursor associated with the save task, needed to reset the crawling cursor in case of error
 * @param futureBulkSaveResult the future result of the bulk save operation, can be null if an exception occurred
 * @author tguillou
 */
public record EsSaveTaskInformation(CrawlingCursor cursor,
                                    Future<BulkSaveResult> futureBulkSaveResult) {

}