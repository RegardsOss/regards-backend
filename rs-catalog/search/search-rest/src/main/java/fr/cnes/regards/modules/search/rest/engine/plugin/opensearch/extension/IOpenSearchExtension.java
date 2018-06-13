package fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.extension;

import com.google.gson.Gson;
import com.rometools.rome.feed.module.Module;

import fr.cnes.regards.framework.geojson.Feature;
import fr.cnes.regards.modules.entities.domain.AbstractEntity;
import fr.cnes.regards.modules.search.schema.OpenSearchParameter;

public interface IOpenSearchExtension {

    /**
     * Does the extension actived ?
     * @return {@link boolean}
     */
    boolean isActivated();

    /**
     * Create the {@link Module} needed by the rome library to generate the specificity of the extension on each entity of the XML+Atom response.
     * @param entity {@link AbstractEntity} entity to write in XML+Atom format
     * @param gson {@link Gson} tool to serialize objects.
     * @return {@link Module} from rome library
     */
    Module getAtomEntityBuilderModule(AbstractEntity entity, Gson gson);

    /**
     * Add parameter into the given {@link Feature} for the {@link AbstractEntity} for Geojson response
     * @param entity {@link AbstractEntity} to write in geojson format.
     * @param feature {@link Feature} from geojson standard
     */
    void applyExtensionToGeoJsonFeature(AbstractEntity entity, Feature feature);

    /**
     * Apply extension for the ginve {@link OpenSearchParameter} during opensearch xml descriptor build.
     * @param parameter {@link OpenSearchParameter}
     */
    void applyExtensionToDescriptionParameter(OpenSearchParameter parameter);

}
