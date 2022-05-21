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
package fr.cnes.regards.modules.search.service.engine.plugin.opensearch.formatter.atom.modules.gml;

import com.rometools.modules.georss.GeoRSSModule;
import com.rometools.rome.feed.module.Module;

/**
 * Provides access to GML Time&Geo information.
 *
 * @author Sébastien Binda
 * @see <a href="https://rometools.github.io/rome/RssAndAtOMUtilitiEsROMEV0.5AndAboveTutorialsAndArticles/RssAndAtOMUtilitiEsROMEPluginsMechanism.html">rometools.github.io</a>
 */
public interface GmlTimeModule extends Module, GmlTimeResponse {

    public final static String URI = GeoRSSModule.GEORSS_GML_URI;

}
