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
package fr.cnes.regards.modules.search.service.engine.plugin.opensearch.extension.media;

import java.net.MalformedURLException;
import java.net.URI;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.commons.compress.utils.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.rometools.modules.mediarss.MediaEntryModuleImpl;
import com.rometools.modules.mediarss.types.Category;
import com.rometools.modules.mediarss.types.MediaContent;
import com.rometools.modules.mediarss.types.MediaGroup;
import com.rometools.modules.mediarss.types.Metadata;
import com.rometools.modules.mediarss.types.UrlReference;
import com.rometools.rome.feed.atom.Entry;
import com.rometools.rome.feed.atom.Link;
import com.rometools.rome.feed.module.Module;

import fr.cnes.regards.framework.geojson.Feature;
import fr.cnes.regards.framework.geojson.GeoJsonLink;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.modules.dam.domain.entities.AbstractEntity;
import fr.cnes.regards.modules.dam.domain.entities.feature.EntityFeature;
import fr.cnes.regards.modules.indexer.domain.DataFile;
import fr.cnes.regards.modules.indexer.domain.criterion.ICriterion;
import fr.cnes.regards.modules.search.schema.OpenSearchDescription;
import fr.cnes.regards.modules.search.schema.parameters.OpenSearchParameter;
import fr.cnes.regards.modules.search.service.engine.plugin.opensearch.ParameterConfiguration;
import fr.cnes.regards.modules.search.service.engine.plugin.opensearch.description.DescriptionParameter;
import fr.cnes.regards.modules.search.service.engine.plugin.opensearch.exception.ExtensionException;
import fr.cnes.regards.modules.search.service.engine.plugin.opensearch.extension.AbstractExtension;
import fr.cnes.regards.modules.search.service.engine.plugin.opensearch.extension.SearchParameter;
import fr.cnes.regards.modules.search.service.engine.plugin.opensearch.formatter.geojson.GeoJsonLinkBuilder;

/**
 * Media extension for Opensearch standard.
 * @see <a href="https://github.com/dewitt/opensearch/blob/master/opensearch-1-1-draft-6.md">Opensearch</a>
 * @see <a href="http://www.opensearch.org/Specifications/OpenSearch/Extensions/Parameter/1.0/Draft_2">Opensearch
 *      parameter extension</a>
 *
 * @author Sébastien Binda
 */
public class MediaExtension extends AbstractExtension {

    private static final Logger LOGGER = LoggerFactory.getLogger(MediaExtension.class);

    public static final String GEO_JSON_RAWDATA_KEY = "rawdata";

    public static final String GEO_JSON_QUICKLOOK_KEY = "quicklook";

    public static final String GEO_JSON_THUMBNAIL_KEY = "thumbnail";

    public static final String LINK_ICON_REL = "icon";

    public static final String LINK_ENCLOSURE_REL = "enclosure";

    public static final String ATOM_MEDIA_QUICKLOOK_CAT = "QUICKLOOK";

    public static final String ATOM_MEDIA_THUMBNAIL_CAT = "THUMBNAIL";

    public static final String ATOM_MEDIA_CAT_REF = "http://www.opengis.net/spec/EOMPOM/1.0";

    @Override
    public void formatGeoJsonResponseFeature(EntityFeature entity, List<ParameterConfiguration> paramConfigurations,
            Feature feature, String token) {
        Multimap<DataType, DataFile> medias = getMedias(entity);
        Object obj = feature.getProperties().get("links");
        if (obj instanceof List<?>) {
            @SuppressWarnings("unchecked")
            List<GeoJsonLink> links = (List<GeoJsonLink>) obj;
            medias.get(DataType.RAWDATA).forEach(f -> {
                feature.addProperty(GEO_JSON_RAWDATA_KEY, GeoJsonLinkBuilder.getDataFileHref(f, token));
                links.add(getGeoJsonLink(f, token));
            });
            Set<String> quicklooks = Sets.newHashSet();
            medias.get(DataType.QUICKLOOK_SD).forEach(f -> {
                quicklooks.add(GeoJsonLinkBuilder.getDataFileHref(f, token));
                links.add(getGeoJsonLink(f, token));
            });
            medias.get(DataType.QUICKLOOK_MD).forEach(f -> {
                quicklooks.add(GeoJsonLinkBuilder.getDataFileHref(f, token));
                links.add(getGeoJsonLink(f, token));
            });
            medias.get(DataType.QUICKLOOK_HD).forEach(f -> {
                quicklooks.add(GeoJsonLinkBuilder.getDataFileHref(f, token));
                links.add(getGeoJsonLink(f, token));
            });
            if (!quicklooks.isEmpty()) {
                feature.addProperty(GEO_JSON_QUICKLOOK_KEY, quicklooks);
            }
            medias.get(DataType.THUMBNAIL).forEach(f -> {
                feature.addProperty(GEO_JSON_THUMBNAIL_KEY, GeoJsonLinkBuilder.getDataFileHref(f, token));
                links.add(getGeoJsonLink(f, token));
            });
        }
    }

    @Override
    public void formatAtomResponseEntry(EntityFeature entity, List<ParameterConfiguration> paramConfigurations,
            Entry entry, Gson gson, String token) {
        Multimap<DataType, DataFile> medias = getMedias(entity);
        // Add module generator
        Module mediaMod = getAtomEntityResponseBuilder(medias, token);
        if (mediaMod != null) {
            entry.getModules().add(mediaMod);
        }
        // Add links
        entry.getAlternateLinks().addAll(getLinks(medias, token));
    }

    @Override
    public void applyToDescriptionParameter(OpenSearchParameter parameter, DescriptionParameter descParameter) {
        // This extension does not apply any modification to Url Parameters into opensearch descritor
    }

    @Override
    public List<OpenSearchParameter> addParametersToDescription() {
        return Lists.newArrayList();
    }

    @Override
    public void applyToDescription(OpenSearchDescription openSearchDescription) {
        // This extension does not apply any modification to global opensearch descritor information
    }

    @Override
    protected ICriterion buildCriteria(SearchParameter parameter) throws ExtensionException {
        // Media extension does not handle search queries.
        return ICriterion.all();
    }

    @Override
    protected boolean supportsSearchParameter(SearchParameter parameter) {
        // Media extension does not handle search queries.
        return false;
    }

    /**
     * Retrieve media files from the given {@link AbstractDataEntity}
     * @param entity {@link AbstractEntity}
     * @return medias by type (Quicklook or Thumbnail)
     */
    private Multimap<DataType, DataFile> getMedias(EntityFeature entity) {
        Multimap<DataType, DataFile> medias = HashMultimap.create();
        medias.putAll(entity.getFiles());
        return medias;
    }

    /**
     * Generate ATOM {@link Link}s for the given medias.
     * @param medias
     * @return {@link Link}s
     */
    private Collection<Link> getLinks(Multimap<DataType, DataFile> medias, String token) {
        List<Link> links = Lists.newArrayList();
        medias.forEach((type, file) -> {
            switch (type) {
                case QUICKLOOK_SD:
                case QUICKLOOK_MD:
                case QUICKLOOK_HD:
                case THUMBNAIL:
                    Link iconLink = new Link();
                    iconLink.setHref(GeoJsonLinkBuilder.getDataFileHref(file, token));
                    iconLink.setRel(LINK_ICON_REL);
                    links.add(iconLink);
                    break;
                case RAWDATA:
                    Link enclosureLink = new Link();
                    enclosureLink.setHref(GeoJsonLinkBuilder.getDataFileHref(file, token));
                    enclosureLink.setRel(LINK_ENCLOSURE_REL);
                    links.add(enclosureLink);
                    break;
                default:
                    break;
            }
        });
        return links;
    }

    /**
     * Build rome {@link Module} to generate media properties for entities into ATOM search response
     * @param medias
     * @return {@link Module}
     */
    private Module getAtomEntityResponseBuilder(Multimap<DataType, DataFile> medias, String token) {
        if (!medias.isEmpty()) {
            List<MediaContent> contents = Lists.newArrayList();
            contents.addAll(generateMediaContents(medias.get(DataType.QUICKLOOK_SD),
                                                  new Category(ATOM_MEDIA_CAT_REF, null, ATOM_MEDIA_QUICKLOOK_CAT),
                                                  token));
            contents.addAll(generateMediaContents(medias.get(DataType.THUMBNAIL),
                                                  new Category(ATOM_MEDIA_CAT_REF, null, ATOM_MEDIA_THUMBNAIL_CAT),
                                                  token));
            MediaEntryModuleImpl mediaMod = new MediaEntryModuleImpl();
            MediaContent[] contentsArr = new MediaContent[contents.size()];
            MediaGroup group = new MediaGroup(contents.toArray(contentsArr));
            MediaGroup[] groups = { group };
            mediaMod.setMediaGroups(groups);
            return mediaMod;
        }
        return null;

    }

    /**
     * Generate ATOM {@link MediaContent}s fro given {@link DataFile}s
     * @param files {@link DataFile}s
     * @param cat {@link Category}
     * @return {@link MediaContent}s
     */
    private List<MediaContent> generateMediaContents(Collection<DataFile> files, Category cat, String token) {
        List<MediaContent> contents = Lists.newArrayList();
        for (DataFile file : files) {
            String href = GeoJsonLinkBuilder.getDataFileHref(file, token);
            MediaContent content = new MediaContent(new UrlReference(URI.create(href)));
            content.setMedium("image");
            content.setType(file.getMimeType().toString());
            content.setHeight(file.getImageHeight().intValue());
            content.setWidth(file.getImageWidth().intValue());
            Metadata metadata = new Metadata();
            Category[] categories = { cat };
            metadata.setCategories(categories);
            content.setMetadata(metadata);
            contents.add(content);
        }
        return contents;
    }

    private GeoJsonLink getGeoJsonLink(DataFile file, String token) {
        URI href = file.asUri();
        String fileName = href.toString();
        try {
            fileName = Paths.get(href.toURL().getFile()).getFileName().toString();
        } catch (MalformedURLException e) {
            LOGGER.warn("Error getting filename for file {}", href.toString());
        }
        return new GeoJsonLink(LINK_ENCLOSURE_REL, file.getMimeType().toString(), fileName,
                GeoJsonLinkBuilder.getDataFileHref(file, token));
    }

}
