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
package fr.cnes.regards.framework.geojson;

import org.springframework.hateoas.Link;

/**
 * Links to add in GEO+JSON open search responses.
 *
 * @author Sébastien Binda
 */
public class GeoJsonLink {

    public static final String LINK_ALTERNATE_REL = "alternate";

    private String rel;

    private String type;

    private String title;

    private String href;

    public GeoJsonLink() {
    }

    public GeoJsonLink(String rel, String type, String title, String href) {
        this.rel = rel;
        this.type = type;
        this.title = title;
        this.href = href;
    }

    /**
     * Construct a {@link GeoJsonLink} using {@link Link}
     *
     * @param springLink {@link Link}
     * @param type       MediaType to add into the geojson link
     */
    public GeoJsonLink(Link springLink, String type) {
        this(springLink.getRel().value(), type, null, springLink.getHref());
    }

    /**
     * Construct a {@link GeoJsonLink} using {@link Link}
     *
     * @param springLink {@link Link}
     * @param rel        use this rel instead of the one given in the spring {@link Link}
     * @param title      title to add into the geojson link
     * @param type       MediaType to add into the geojson link
     */
    public GeoJsonLink(Link springLink, String rel, String title, String type) {
        this(rel, type, title, springLink.getHref());
    }

    public String getRel() {
        return rel;
    }

    public void setRel(String rel) {
        this.rel = rel;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getHref() {
        return href;
    }

    public void setHref(String href) {
        this.href = href;
    }

}
