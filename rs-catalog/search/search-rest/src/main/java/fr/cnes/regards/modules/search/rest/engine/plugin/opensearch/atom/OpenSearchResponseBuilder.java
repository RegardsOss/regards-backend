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
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.apache.commons.compress.utils.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.rometools.modules.georss.geometries.AbstractGeometry;
import com.rometools.modules.georss.geometries.Point;
import com.rometools.modules.georss.geometries.Position;
import com.rometools.modules.mediarss.MediaEntryModuleImpl;
import com.rometools.modules.mediarss.types.Category;
import com.rometools.modules.mediarss.types.MediaContent;
import com.rometools.modules.mediarss.types.MediaGroup;
import com.rometools.modules.mediarss.types.Metadata;
import com.rometools.modules.mediarss.types.UrlReference;
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
import fr.cnes.regards.framework.oais.urn.DataType;
import fr.cnes.regards.modules.entities.domain.AbstractDataEntity;
import fr.cnes.regards.modules.entities.domain.AbstractEntity;
import fr.cnes.regards.modules.indexer.dao.FacetPage;
import fr.cnes.regards.modules.indexer.domain.DataFile;
import fr.cnes.regards.modules.search.domain.plugin.SearchContext;
import fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.atom.modules.gml.impl.GmlTimeModuleImpl;
import fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.atom.modules.regards.RegardsModule;
import fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.atom.modules.regards.impl.RegardsModuleImpl;

/**
 * Build open search responses in ATOM format handling :
 * - parameters extension
 * - time & geo extension
 * - regards extension
 * @author Sébastien Binda
 */
public class OpenSearchResponseBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenSearchResponseBuilder.class);

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

    public static Entry buildFeedAtomEntry(Feed feed, AbstractEntity entity, OffsetDateTime startDate,
            OffsetDateTime stopDate, Gson gson) {
        Entry entry = new Entry();
        entry.setId(entity.getIpId().toString());
        if (entity.getCreationDate() != null) {
            entry.setPublished(Date.valueOf(entity.getCreationDate().toLocalDate()));
        }
        if (entity.getLastUpdate() != null) {
            entry.setUpdated(Date.valueOf(entity.getLastUpdate().toLocalDate()));
        }
        entry.setTitle(entity.getLabel());
        List<Module> mods = entry.getModules();

        mods.add(generateRegardsModule(entity, gson));
        mods.add(generateGMLTimeModule(entity, startDate, stopDate, gson));
        generateMediaEntityModule(entity).ifPresent(mods::add);

        entry.setModules(mods);
        return entry;
    }

    private static RegardsModule generateRegardsModule(AbstractEntity entity, Gson gson) {
        RegardsModule rm = new RegardsModuleImpl();
        rm.setGsonBuilder(gson);
        rm.setEntity(entity);
        return rm;
    }

    private static GmlTimeModuleImpl generateGMLTimeModule(AbstractEntity entity, OffsetDateTime startDate,
            OffsetDateTime stopDate, Gson gson) {
        // Add GML with time module to handle geo & time extension
        GmlTimeModuleImpl gmlMod = new GmlTimeModuleImpl();
        gmlMod.setStartDate(startDate);
        gmlMod.setStopDate(stopDate);
        gmlMod.setGsonBuilder(gson);
        gmlMod.setGeometry(buildGeometry(entity.getGeometry()));
        return gmlMod;
    }

    private static Optional<MediaEntryModuleImpl> generateMediaEntityModule(AbstractEntity entity) {
        Optional<MediaEntryModuleImpl> result = Optional.empty();
        if (entity instanceof AbstractDataEntity) {
            // Find quicklook url from entity
            AbstractDataEntity dataEntity = (AbstractDataEntity) entity;
            Collection<DataFile> quicklooks = dataEntity.getFiles().get(DataType.QUICKLOOK_SD);

            // Find thumbnail url from entity
            Collection<DataFile> thumbnails = dataEntity.getFiles().get(DataType.THUMBNAIL);

            if (!quicklooks.isEmpty() || !thumbnails.isEmpty()) {
                List<MediaContent> contents = Lists.newArrayList();
                contents.addAll(generateMediaContents(quicklooks, new Category("http://www.opengis.net/spec/EOMPOM/1.0",
                        null, "QUICKLOOK")));
                contents.addAll(generateMediaContents(thumbnails, new Category("http://www.opengis.net/spec/EOMPOM/1.0",
                        null, "THUMBNAIL")));
                MediaEntryModuleImpl mediaMod = new MediaEntryModuleImpl();
                MediaContent[] contentsArr = new MediaContent[contents.size()];
                MediaGroup group = new MediaGroup(contents.toArray(contentsArr));
                MediaGroup[] groups = { group };
                mediaMod.setMediaGroups(groups);
                result = Optional.of(mediaMod);
            }
        }
        return result;
    }

    private static List<MediaContent> generateMediaContents(Collection<DataFile> files, Category cat) {
        List<MediaContent> contents = Lists.newArrayList();
        for (DataFile file : files) {
            MediaContent content = new MediaContent(new UrlReference(file.getUri()));
            content.setMedium("image");
            content.setType(file.getMimeType().toString());
            content.setHeight(file.getImageHeight());
            content.setWidth(file.getImageWidth());
            Metadata metadata = new Metadata();
            Category[] categories = { cat };
            metadata.setCategories(categories);
            content.setMetadata(metadata);
            contents.add(content);
        }
        return contents;
    }

    private static AbstractGeometry buildGeometry(IGeometry geometry) {
        if (geometry == null) {
            return null;
        }
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
