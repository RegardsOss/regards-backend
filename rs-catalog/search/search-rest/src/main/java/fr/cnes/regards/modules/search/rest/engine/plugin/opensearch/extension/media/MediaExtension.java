package fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.extension.media;

import java.net.MalformedURLException;
import java.net.URI;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;

import org.apache.commons.compress.utils.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
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
import fr.cnes.regards.framework.oais.urn.DataType;
import fr.cnes.regards.modules.entities.domain.AbstractDataEntity;
import fr.cnes.regards.modules.entities.domain.AbstractEntity;
import fr.cnes.regards.modules.indexer.domain.DataFile;
import fr.cnes.regards.modules.indexer.domain.criterion.ICriterion;
import fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.OpenSearchParameterConfiguration;
import fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.description.DescriptionParameter;
import fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.exception.UnsupportedCriterionOperator;
import fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.extension.AbstractOpenSearchExtension;
import fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.extension.SearchParameter;
import fr.cnes.regards.modules.search.schema.OpenSearchDescription;
import fr.cnes.regards.modules.search.schema.parameters.OpenSearchParameter;

/**
 * Media extension for Opensearch standard.
 * @see <a href="https://github.com/dewitt/opensearch/blob/master/opensearch-1-1-draft-6.md">Opensearch</a>
 * @see <a href="http://www.opensearch.org/Specifications/OpenSearch/Extensions/Parameter/1.0/Draft_2">Opensearch parameter extension</a>
 *
 * @author Sébastien Binda
 */
public class MediaExtension extends AbstractOpenSearchExtension {

    private static final Logger LOGGER = LoggerFactory.getLogger(MediaExtension.class);

    public static final String GEO_JSON_QUICKLOOK_KEY = "quicklook";

    public static final String GEO_JSON_THUMBNAIL_KEY = "thumbnail";

    public static final String LINK_ICON_REL = "icon";

    public static final String LINK_ENCLOSURE_REL = "enclosure";

    public static final String ATOM_MEDIA_QUICKLOOK_CAT = "QUICKLOOK";

    public static final String ATOM_MEDIA_THUMBNAIL_CAT = "THUMBNAIL";

    public static final String ATOM_MEDIA_CAT_REF = "http://www.opengis.net/spec/EOMPOM/1.0";

    @Override
    public void formatGeoJsonResponseFeature(AbstractEntity entity,
            List<OpenSearchParameterConfiguration> paramConfigurations, Feature feature) {
        Multimap<DataType, DataFile> medias = getMedias(entity);
        Object obj = feature.getProperties().get("links");
        if (obj instanceof List<?>) {
            @SuppressWarnings("unchecked")
            List<GeoJsonLink> links = (List<GeoJsonLink>) obj;
            medias.get(DataType.RAWDATA).forEach(f -> {
                links.add(getGeoJsonLink(f));
            });
            medias.get(DataType.QUICKLOOK_SD).forEach(f -> {
                feature.addProperty(GEO_JSON_QUICKLOOK_KEY, f.getUri().toString());
                links.add(getGeoJsonLink(f));
            });
            medias.get(DataType.THUMBNAIL).forEach(f -> {
                feature.addProperty(GEO_JSON_THUMBNAIL_KEY, f.getUri().toString());
                links.add(getGeoJsonLink(f));
            });
        }
    }

    @Override
    public void formatAtomResponseEntry(AbstractEntity entity,
            List<OpenSearchParameterConfiguration> paramConfigurations, Entry entry, Gson gson) {
        Multimap<DataType, DataFile> medias = getMedias(entity);
        // Add module generator
        Module mediaMod = getAtomEntityResponseBuilder(medias);
        if (mediaMod != null) {
            entry.getModules().add(mediaMod);
        }
        // Add links
        entry.getAlternateLinks().addAll(getLinks(medias));
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
    protected ICriterion buildCriteria(SearchParameter parameter) throws UnsupportedCriterionOperator {
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
    private Multimap<DataType, DataFile> getMedias(AbstractEntity entity) {
        Multimap<DataType, DataFile> medias = HashMultimap.create();
        if (entity instanceof AbstractDataEntity) {
            // Find quicklook url from entity
            AbstractDataEntity dataEntity = (AbstractDataEntity) entity;
            medias.putAll(dataEntity.getFiles());
        }
        return medias;
    }

    /**
     * Generate ATOM {@link Link}s for the given medias.
     * @param medias
     * @return {@link Link}s
     */
    private Collection<Link> getLinks(Multimap<DataType, DataFile> medias) {
        List<Link> links = Lists.newArrayList();
        medias.forEach((type, file) -> {
            switch (type) {
                case QUICKLOOK_SD:
                case THUMBNAIL:
                    Link iconLink = new Link();
                    iconLink.setHref(file.getUri().toString());
                    iconLink.setRel(LINK_ICON_REL);
                    links.add(iconLink);
                    break;
                case RAWDATA:
                    Link enclosureLink = new Link();
                    enclosureLink.setHref(file.getUri().toString());
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
    private Module getAtomEntityResponseBuilder(Multimap<DataType, DataFile> medias) {
        if (!medias.isEmpty()) {
            List<MediaContent> contents = Lists.newArrayList();
            contents.addAll(generateMediaContents(medias.get(DataType.QUICKLOOK_SD),
                                                  new Category(ATOM_MEDIA_CAT_REF, null, ATOM_MEDIA_QUICKLOOK_CAT)));
            contents.addAll(generateMediaContents(medias.get(DataType.THUMBNAIL),
                                                  new Category(ATOM_MEDIA_CAT_REF, null, ATOM_MEDIA_THUMBNAIL_CAT)));
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
    private List<MediaContent> generateMediaContents(Collection<DataFile> files, Category cat) {
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

    private GeoJsonLink getGeoJsonLink(DataFile file) {
        URI href = file.getUri();
        String fileName = href.toString();
        try {
            fileName = Paths.get(href.toURL().getFile()).getFileName().toString();
        } catch (MalformedURLException e) {
            LOGGER.warn("Error getting filename for file {}", href.toString());
        }
        return new GeoJsonLink(LINK_ENCLOSURE_REL, file.getMimeType().toString(), fileName, href.toString());
    }

}
