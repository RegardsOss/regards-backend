/*
 * Copyright 2017-2023 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.ingest.dto.request.dissemination;

import fr.cnes.regards.modules.ingest.dto.aip.SearchAIPsParameters;

import java.util.Set;

/**
 * A Dto which contains aip description to disseminate, and list of dissemination destination.
 *
 * @author Thomas GUILLOU
 **/
public record AIPDisseminationRequestDto(
    /**
     * The search parameter of aip to disseminate
     */
    SearchAIPsParameters filters,
    /**
     * Set of dissemination destinations
     */
    Set<String> recipients) {

}
