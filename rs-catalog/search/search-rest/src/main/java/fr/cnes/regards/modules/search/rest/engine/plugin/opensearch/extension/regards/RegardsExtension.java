package fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.extension.regards;

import java.util.Map;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.rometools.rome.feed.module.Module;

import fr.cnes.regards.framework.geojson.Feature;
import fr.cnes.regards.modules.entities.domain.AbstractEntity;
import fr.cnes.regards.modules.entities.domain.attribute.AbstractAttribute;
import fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.atom.modules.regards.RegardsModule;
import fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.atom.modules.regards.impl.RegardsModuleImpl;
import fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.extension.IOpenSearchExtension;

public class RegardsExtension implements IOpenSearchExtension {

    private boolean activated = false;

    @Override
    public boolean isActivated() {
        return activated;
    }

    @Override
    public void applyExtensionToGeoJsonFeature(AbstractEntity entity, Feature feature) {
        Map<String, Object> properties = Maps.newHashMap();
        for (AbstractAttribute<?> property : entity.getProperties()) {
            properties.put(property.getName(), property.getValue());
        }
        feature.setProperties(properties);
    }

    @Override
    public Module getAtomEntityBuilderModule(AbstractEntity entity, Gson gson) {
        RegardsModule rm = new RegardsModuleImpl();
        rm.setGsonBuilder(gson);
        rm.setEntity(entity);
        return rm;
    }

    public void setActivated(boolean activated) {
        this.activated = activated;
    }

}
