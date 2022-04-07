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
package fr.cnes.regards.modules.search.service.engine.plugin.opensearch.extension.media;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.gson.Gson;
import com.rometools.modules.mediarss.MediaEntryModuleImpl;
import com.rometools.modules.mediarss.types.*;
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
import fr.cnes.regards.modules.search.service.engine.plugin.opensearch.extension.AbstractExtension;
import fr.cnes.regards.modules.search.service.engine.plugin.opensearch.extension.SearchParameter;
import fr.cnes.regards.modules.search.service.engine.plugin.opensearch.formatter.DataFileHrefBuilder;

import java.net.URI;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Media extension for Opensearch standard.
 *
 * @author Sébastien Binda
 * @see <a href="https://github.com/dewitt/opensearch/blob/master/opensearch-1-1-draft-6.md">Opensearch</a>
 * @see <a href="http://www.opensearch.org/Specifications/OpenSearch/Extensions/Parameter/1.0/Draft_2">Opensearch
 * parameter extension</a>
 */
public class MediaExtension extends AbstractExtension {

    public static final String GEO_JSON_RAWDATA_KEY = "rawdata";

    public static final String GEO_JSON_QUICKLOOK_KEY = "quicklook";

    public static final String GEO_JSON_THUMBNAIL_KEY = "thumbnail";

    public static final String LINK_ICON_REL = "icon";

    public static final String LINK_ENCLOSURE_REL = "enclosure";

    public static final String ATOM_MEDIA_QUICKLOOK_CAT = "QUICKLOOK";

    public static final String ATOM_MEDIA_THUMBNAIL_CAT = "THUMBNAIL";

    public static final String ATOM_MEDIA_CAT_REF = "http://www.opengis.net/spec/EOMPOM/1.0";

    private static final List<DataType> QUICKLOOK_DATA_TYPES = Lists.newArrayList(DataType.QUICKLOOK_SD,
                                                                                 DataType.QUICKLOOK_MD,
                                                                                 DataType.QUICKLOOK_HD);

    /**
     * Convert a {@link DataFile} to a {@link GeoJsonLink}. Only for publicly available resource like QUICKLOOK and THUMBNAIL
     *
     * @param dataFile {@link DataFile}
     * @param scope
     * @return {@link GeoJsonLink}
     */
    private static GeoJsonLink getGeoJsonLink(DataFile dataFile, String scope) {
        String fileName = getFileNameFromDataFile(dataFile);
        String href = DataFileHrefBuilder.getDataFileHref(dataFile, scope);
        return new GeoJsonLink(LINK_ENCLOSURE_REL, dataFile.getMimeType().toString(), fileName, href);
    }

    private static String getFileNameFromDataFile(DataFile dataFile) {
        return Paths.get(dataFile.asUri().getPath()).getFileName().toString();
    }

    @Override
    public void formatGeoJsonResponseFeature(AbstractEntity<EntityFeature> entity,
                                             List<ParameterConfiguration> paramConfigurations,
                                             Feature feature,
                                             String scope) {
        Multimap<DataType, DataFile> medias = getMedias(entity.getFeature());
        Object obj = feature.getProperties().get("links");
        if (obj instanceof List<?>) {
            @SuppressWarnings("unchecked") List<GeoJsonLink> links = (List<GeoJsonLink>) obj;

            links.addAll(getRawdataLinks(medias.get(DataType.RAWDATA), scope));
            links.addAll(getAllQuicklookTypesLinks(medias, scope));
            links.addAll(getImageLinks(medias.get(DataType.THUMBNAIL), scope));

            // Add rawdata property at the root of the Feature
            addFeatureProperty(feature,
                               GEO_JSON_RAWDATA_KEY,
                               getDataFileHrefOptByDataType(medias, DataType.RAWDATA, scope));

            // Add thumbnail property at the root of the Feature
            addFeatureProperty(feature,
                               GEO_JSON_THUMBNAIL_KEY,
                               getDataFileHrefOptByDataType(medias, DataType.THUMBNAIL, scope));

            // Add quicklook property at the root of the Feature
            addFeatureProperty(feature, GEO_JSON_QUICKLOOK_KEY, getQuicklookDataFileHrefOpt(medias, scope));
        }
    }

    private Collection<GeoJsonLink> getRawdataLinks(Collection<DataFile> rawdataDataFiles, String scope) {
        return rawdataDataFiles.stream()
            .map(dataFile -> getGeoJsonLink(dataFile, scope))
            .collect(Collectors.toList());
    }

    private Collection<GeoJsonLink> getAllQuicklookTypesLinks(Multimap<DataType, DataFile> medias, String scope) {
        List<DataFile> allQuicklooksTypesDataFiles = QUICKLOOK_DATA_TYPES.stream()
            .map(medias::get)
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
        return getImageLinks(allQuicklooksTypesDataFiles, scope);
    }

    private Collection<GeoJsonLink> getImageLinks(Collection<DataFile> thumbnailDataFiles, String scope) {
        return thumbnailDataFiles.stream()
            .map(dataFile -> getGeoJsonLink(dataFile, scope))
            .collect(Collectors.toList());
    }

    private void addFeatureProperty(Feature feature, String propertyKey, Optional<String> propertyValue) {
        propertyValue.ifPresent(href -> feature.addProperty(propertyKey, href));
    }

    /**
     * Get the href of the first DataFile to use in the root quickloook property.
     * Fallback to the thumbnail if there is no QUICKLOOK
     * Order of priority : QUICKLOOK_MD > QUICKLOOK_SD > QUICKLOOK_HD > THUMBNAIL
     */
    private Optional<String> getQuicklookDataFileHrefOpt(Multimap<DataType, DataFile> medias, String scope) {
        Optional<String> hrefQuicklookMD = getDataFileHrefOptByDataType(medias, DataType.QUICKLOOK_MD, scope);
        if (hrefQuicklookMD.isPresent()) {
            return hrefQuicklookMD;
        }
        Optional<String> hrefQuicklookSD = getDataFileHrefOptByDataType(medias, DataType.QUICKLOOK_SD, scope);
        if (hrefQuicklookSD.isPresent()) {
            return hrefQuicklookSD;
        }
        Optional<String> hrefQuicklookHD = getDataFileHrefOptByDataType(medias, DataType.QUICKLOOK_HD, scope);
        if (hrefQuicklookHD.isPresent()) {
            return hrefQuicklookHD;
        }
        return getDataFileHrefOptByDataType(medias, DataType.THUMBNAIL, scope);
    }

    private Optional<String> getDataFileHrefOptByDataType(Multimap<DataType, DataFile> medias,
                                                          DataType dataType,
                                                          String scope) {
        return medias.get(dataType)
            .stream()
            .findFirst()
            .map(dataFile -> DataFileHrefBuilder.getDataFileHref(dataFile, scope));

    }

    @Override
    public void formatAtomResponseEntry(AbstractEntity<EntityFeature> entity,
                                        List<ParameterConfiguration> paramConfigurations,
                                        Entry entry,
                                        Gson gson,
                                        String scope) {
        Multimap<DataType, DataFile> medias = getMedias(entity.getFeature());
        // Add module generator
        Module mediaMod = getAtomEntityResponseBuilder(medias, scope);
        if (mediaMod != null) {
            entry.getModules().add(mediaMod);
        }
        // Add links
        entry.getAlternateLinks().addAll(getAtomLinks(medias, scope));
    }

    @Override
    public Optional<String> getDescriptorParameterValue(DescriptionParameter descParameter) {
        // Media extension does not provide any search query param.
        return Optional.empty();
    }

    @Override
    public List<OpenSearchParameter> getDescriptorBasicExtensionParameters() {
        return Collections.emptyList();
    }

    @Override
    public void applyToDescription(OpenSearchDescription openSearchDescription) {
        // This extension does not apply any modification to global opensearch descriptor information
    }

    @Override
    protected ICriterion buildCriteria(SearchParameter parameter) {
        // Media extension does not provide any search query param
        return ICriterion.all();
    }

    @Override
    protected boolean supportsSearchParameter(SearchParameter parameter) {
        // Media extension does not handle search queries.
        return false;
    }

    /**
     * Retrieve media files from the given {@link EntityFeature}
     *
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
     *
     * @param medias
     * @return {@link Link}s
     */
    private Collection<Link> getAtomLinks(Multimap<DataType, DataFile> medias, String scope) {
        List<Link> links = new ArrayList<>();
        medias.forEach((type, file) -> {
            switch (type) {
                case QUICKLOOK_SD:
                case QUICKLOOK_MD:
                case QUICKLOOK_HD:
                case THUMBNAIL:
                    Link iconLink = new Link();
                    iconLink.setHref(DataFileHrefBuilder.getDataFileHref(file, scope));
                    iconLink.setRel(LINK_ICON_REL);
                    links.add(iconLink);
                    break;
                case RAWDATA:
                    Link enclosureLink = new Link();
                    enclosureLink.setHref(DataFileHrefBuilder.getDataFileHref(file, scope));
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
     *
     * @param medias
     * @return {@link Module}
     */
    private Module getAtomEntityResponseBuilder(Multimap<DataType, DataFile> medias, String scope) {
        if (!medias.isEmpty()) {
            List<MediaContent> contents = Lists.newArrayList();
            contents.addAll(generateAtomMediaContents(medias.get(DataType.QUICKLOOK_SD),
                                                      new Category(ATOM_MEDIA_CAT_REF, null, ATOM_MEDIA_QUICKLOOK_CAT),
                                                      scope));
            contents.addAll(generateAtomMediaContents(medias.get(DataType.THUMBNAIL),
                                                      new Category(ATOM_MEDIA_CAT_REF, null, ATOM_MEDIA_THUMBNAIL_CAT),
                                                      scope));
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
     *
     * @param files {@link DataFile}s
     * @param cat   {@link Category}
     * @return {@link MediaContent}s
     */
    private List<MediaContent> generateAtomMediaContents(Collection<DataFile> files, Category cat, String scope) {
        List<MediaContent> contents = Lists.newArrayList();
        for (DataFile file : files) {
            String href = DataFileHrefBuilder.getDataFileHref(file, scope);
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

}
