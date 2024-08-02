/* Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.processing.domain.size;

import lombok.Value;

/**
 * This class defines data for statistics over a set of files given as input for a batch.
 * This allows to decide in advance whether the batch can be executed or not, based on
 * the process's size and duration forecasts.
 *
 * @author gandrieu
 */
@Value
public class FileSetStatistics {

    /**
     * The dataset ID this file set comes from
     */
    String dataset;

    /**
     * The total number of executions to be launched
     */
    int executionCount;

    /**
     * The total number of bytes to be treated
     */
    long totalBytes;

}
