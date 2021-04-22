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
package fr.cnes.regards.modules.search.service.engine.plugin.opensearch.formatter.geojson;

import org.springframework.hateoas.Link;

import fr.cnes.regards.framework.geojson.GeoJsonLink;
import fr.cnes.regards.modules.indexer.domain.DataFile;

/**
 * Builder to convert a {@link Link} to a {@link GeoJsonLink}
 * @author Sébastien Binda
 */
public class GeoJsonLinkBuilder {

    private GeoJsonLinkBuilder() {
    }

    /**
     * Convert a {@link Link} to a {@link GeoJsonLink}
     * @param springLink {@link Link}
     * @return {@link GeoJsonLink}
     */
    public static GeoJsonLink build(Link springLink) {
        GeoJsonLink link = new GeoJsonLink();
        link.setHref(springLink.getHref());
        link.setRel(springLink.getRel().value());
        return link;
    }

    /**
     * Convert a {@link Link} to a {@link GeoJsonLink}
     * @param springLink {@link Link}
     * @param type MediaType to add into the geojson link
     * @return {@link GeoJsonLink}
     */
    public static GeoJsonLink build(Link springLink, String type, String token) {
        GeoJsonLink link = new GeoJsonLink();
        link.setHref(getDataFileHref(springLink.getHref(), token));
        link.setRel(springLink.getRel().value());
        link.setType(type);
        return link;
    }

    /**
     * Convert a {@link Link} to a {@link GeoJsonLink}
     * @param springLink {@link Link}
     * @param rel use this rel instead of the one given in the spring {@link Link}
     * @param title title to add into the geojson link
     * @param type MediaType to add into the geojson link
     * @return {@link GeoJsonLink}
     */
    public static GeoJsonLink build(Link springLink, String rel, String title, String type, String token) {
        GeoJsonLink link = GeoJsonLinkBuilder.build(springLink, type, token);
        link.setRel(rel);
        link.setTitle(title);
        return link;
    }

    public static String getDataFileHref(DataFile file, String token) {
        String href = file.getUri();
        if (!file.isReference()) {
            return getDataFileHref(href, token);
        }
        return href;
    }

    public static String getDataFileHref(String href, String token) {
        if (href.contains("?")) {
            return String.format("%s&token=%s", href, token);
        } else {
            return String.format("%s?token=%s", href, token);
        }
    }

}
