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

package fr.cnes.regards.modules.processing.order;

import fr.cnes.regards.framework.urn.UniformResourceName;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.With;

/**
 * This class defines how to interpret PInputFile metadata field for rs-order use of processing.
 *
 * @author gandrieu
 */
@lombok.Data @With
@AllArgsConstructor
@NoArgsConstructor
public class OrderInputFileMetadata {

    /** Whether the input file is internal (URL pointing to storage) or external (free URL) */
    Boolean internal;

    /** The ID for feature the data file came from. */
    UniformResourceName featureId;

    /** If a location is provided (in case the input is not a file) */
    String storedPath;
}
