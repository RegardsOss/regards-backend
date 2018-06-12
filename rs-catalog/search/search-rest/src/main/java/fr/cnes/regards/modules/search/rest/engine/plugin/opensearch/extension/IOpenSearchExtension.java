package fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.extension;

import com.google.gson.Gson;
import com.rometools.rome.feed.module.Module;

import fr.cnes.regards.framework.geojson.Feature;
import fr.cnes.regards.modules.entities.domain.AbstractEntity;

public interface IOpenSearchExtension {

    boolean isActivated();

    Module getAtomEntityBuilderModule(AbstractEntity entity, Gson gson);

    void applyExtensionToGeoJsonFeature(AbstractEntity entity, Feature feature);

}
