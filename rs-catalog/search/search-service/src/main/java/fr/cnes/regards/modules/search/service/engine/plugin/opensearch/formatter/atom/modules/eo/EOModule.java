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
package fr.cnes.regards.modules.search.service.engine.plugin.opensearch.formatter.atom.modules.eo;

import com.rometools.rome.feed.module.Module;
import fr.cnes.regards.modules.search.service.engine.plugin.opensearch.extension.eo.EarthObservationAttribute;

import java.util.Map;

/**
 * Provides access to Earth Observation information.
 *
 * @author Léo Mieulet
 * @see <a href="https://docs.opengeospatial.org/is/13-026r9/13-026r9.html"> Annex D (informative): Metadata Mappings</a>
 */
public interface EOModule extends Module, EOResponse {

    String URI = "http://www.opengis.net/eop/2.1";

    Map<EarthObservationAttribute, Object> getActiveProperties();
}
