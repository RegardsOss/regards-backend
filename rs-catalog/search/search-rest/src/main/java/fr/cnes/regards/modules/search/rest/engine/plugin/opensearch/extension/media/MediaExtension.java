package fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.extension.media;

import java.util.Collection;
import java.util.List;

import org.apache.commons.compress.utils.Lists;
import org.springframework.util.MultiValueMap;

import com.google.gson.Gson;
import com.rometools.modules.mediarss.MediaEntryModuleImpl;
import com.rometools.modules.mediarss.types.Category;
import com.rometools.modules.mediarss.types.MediaContent;
import com.rometools.modules.mediarss.types.MediaGroup;
import com.rometools.modules.mediarss.types.Metadata;
import com.rometools.modules.mediarss.types.UrlReference;
import com.rometools.rome.feed.module.Module;

import fr.cnes.regards.framework.geojson.Feature;
import fr.cnes.regards.framework.oais.urn.DataType;
import fr.cnes.regards.modules.entities.domain.AbstractDataEntity;
import fr.cnes.regards.modules.entities.domain.AbstractEntity;
import fr.cnes.regards.modules.indexer.domain.DataFile;
import fr.cnes.regards.modules.indexer.domain.criterion.ICriterion;
import fr.cnes.regards.modules.opensearch.service.cache.attributemodel.IAttributeFinder;
import fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.OpenSearchParameterConfiguration;
import fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.extension.IOpenSearchExtension;
import fr.cnes.regards.modules.search.schema.OpenSearchDescription;
import fr.cnes.regards.modules.search.schema.OpenSearchParameter;

/**
 * Geo&Time parameter extension for Opensearch standard.
 * @see <a href="https://github.com/dewitt/opensearch/blob/master/opensearch-1-1-draft-6.md">Opensearch</a>
 * @see <a href="http://www.opensearch.org/Specifications/OpenSearch/Extensions/Parameter/1.0/Draft_2">Opensearch parameter extension</a>
 *
 * @author Sébastien Binda
 */
public class MediaExtension implements IOpenSearchExtension {

    private boolean activated = false;

    @Override
    public boolean isActivated() {
        return activated;
    }

    @Override
    public void applyExtensionToGeoJsonFeature(AbstractEntity entity, Feature feature) {
        // TODO Auto-generated method stub
    }

    @Override
    public Module getAtomEntityBuilderModule(AbstractEntity entity, Gson gson) {
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
                return mediaMod;
            }
        }
        return null;
    }

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

    public void setActivated(boolean activated) {
        this.activated = activated;
    }

    @Override
    public void applyExtensionToDescriptionParameter(OpenSearchParameter parameter) {
        // Nothing to do
    }

    @Override
    public void applyExtensionToDescription(OpenSearchDescription openSearchDescription) {
        // Nothing to do
    }

    @Override
    public ICriterion buildCriterion(MultiValueMap<String, String> queryParams,
            List<OpenSearchParameterConfiguration> configurations, IAttributeFinder finder) {
        return ICriterion.all();
    }

}
