/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.atom;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

import com.google.gson.Gson;
import com.rometools.modules.georss.GMLModuleImpl;
import com.rometools.modules.georss.GeoRSSModule;
import com.rometools.modules.georss.geometries.AbstractGeometry;
import com.rometools.modules.georss.geometries.Point;
import com.rometools.modules.georss.geometries.Position;
import com.rometools.modules.opensearch.OpenSearchModule;
import com.rometools.modules.opensearch.entity.OSQuery;
import com.rometools.modules.opensearch.impl.OpenSearchModuleImpl;
import com.rometools.rome.feed.atom.Content;
import com.rometools.rome.feed.atom.Entry;
import com.rometools.rome.feed.atom.Feed;
import com.rometools.rome.feed.atom.Link;
import com.rometools.rome.feed.module.Module;
import com.rometools.rome.feed.synd.SyndPerson;
import com.rometools.rome.feed.synd.SyndPersonImpl;

import fr.cnes.regards.framework.geojson.geometry.IGeometry;
import fr.cnes.regards.modules.entities.domain.AbstractEntity;
import fr.cnes.regards.modules.indexer.dao.FacetPage;
import fr.cnes.regards.modules.search.domain.plugin.SearchContext;
import fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.atom.module.RegardsModule;
import fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.atom.module.impl.RegardsModuleImpl;

/**
 * Build open search responses in ATOM format handling :
 * - parameters extension
 * - time & geo extension
 * - regards extension
 * @author Sébastien Binda
 */
public class OpenSearchResponseBuilder {

    public static final String ATOM_VERSION = "atom_1.0";

    public static Feed buildFeedMetadata(String searchId, String searchTitle, String searchDescription,
            String openSearchDescriptionUrl, SearchContext context, FacetPage<AbstractEntity> page) {
        Feed feed = new Feed(ATOM_VERSION);
        // Fee general informations
        feed.setId(searchId);
        feed.setTitle(searchTitle);

        // Feed description
        Content content = new Content();
        content.setType(Content.TEXT);
        content.setValue(searchDescription);
        feed.setSubtitle(content);

        // Create feed author.
        SyndPerson author = new SyndPersonImpl();
        author.setEmail("regards@cnes.fr");
        author.setName("CNES - Centre national d'études spatiales");
        author.setUri("https://regardsoss.github.io/");
        feed.getAuthors().add(author);

        // Add search date
        feed.setUpdated(Date.valueOf(LocalDate.now()));

        // Add search language
        feed.setLanguage("en-US");

        // Add the opensearch module, you would get information like totalResults from the
        // return results of your search
        List<Module> mods = feed.getModules();
        OpenSearchModule osm = new OpenSearchModuleImpl();
        osm.setItemsPerPage(page.getSize());
        osm.setStartIndex(page.getNumber());
        osm.setTotalResults(page.getNumberOfElements());

        // Add the query from opensearch module
        OSQuery query = new OSQuery();
        if ((context.getQueryParams() != null) && context.getQueryParams().containsKey("q")) {
            query.setSearchTerms(context.getQueryParams().get("q").get(0));
        }
        query.setStartPage(context.getPageable().getPageNumber());
        query.setRole("request");
        osm.addQuery(query);
        // Add opensearch description link
        Link osDescLink = new Link();
        osDescLink.setHref(openSearchDescriptionUrl);
        osDescLink.setType("application/opensearchdescription+xml");
        osm.setLink(osDescLink);
        mods.add(osm);
        feed.setModules(mods);

        // Add simple link to opensearch description
        feed.getAlternateLinks().add(osDescLink);

        return feed;
    }

    public static Entry buildFeedAtomEntry(Feed feed, AbstractEntity entity, Gson gson) {
        Entry entry = new Entry();
        entry.setId(entity.getIpId().toString());
        if (entity.getCreationDate() != null) {
            entry.setPublished(Date.valueOf(entity.getCreationDate().toLocalDate()));
        }
        entry.setTitle(entity.getLabel());
        List<Module> mods = entry.getModules();
        RegardsModule rm = new RegardsModuleImpl();
        rm.setGsonBuilder(gson);
        rm.setEntity(entity);
        mods.add(rm);

        GeoRSSModule geoMod = new GMLModuleImpl();
        geoMod.setGeometry(buildGeometry(entity.getGeometry()));
        mods.add(geoMod);
        entry.setModules(mods);
        return entry;
    }

    private static AbstractGeometry buildGeometry(IGeometry geometry) {
        switch (geometry.getType()) {
            case POINT:
                fr.cnes.regards.framework.geojson.geometry.Point rp = (fr.cnes.regards.framework.geojson.geometry.Point) geometry;
                Point point = new Point();
                point.setPosition(new Position(rp.getCoordinates().getLatitude(), rp.getCoordinates().getLongitude()));
                return point;
            case FEATURE:
            case FEATURE_COLLECTION:
            case GEOMETRY_COLLECTION:
            case LINESTRING:
            case MULTILINESTRING:
            case MULTIPOINT:
            case MULTIPOLYGON:
            case POLYGON:
            case UNLOCATED:
            default:
                // TODO implement builders.
                return null;
        }
    }

}
