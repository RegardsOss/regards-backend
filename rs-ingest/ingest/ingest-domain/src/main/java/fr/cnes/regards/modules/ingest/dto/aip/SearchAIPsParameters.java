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
package fr.cnes.regards.modules.ingest.dto.aip;

/**
 * Concrete class that stores AIP criteria to do some research against AIP repository
 * @author sbinda
 * @author Léo Mieulet
 */
public class SearchAIPsParameters extends AbstractSearchAIPsParameters<SearchAIPsParameters> {
    // All attributes are already located {@link AbstractSearchAIPsParameters}

    public static SearchAIPsParameters build() {
        SearchAIPsParameters aipsParameters = new SearchAIPsParameters();
        return aipsParameters;
    }
}
