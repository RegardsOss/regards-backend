package fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.extension.geo;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.assertj.core.util.Lists;

import com.google.gson.Gson;
import com.rometools.modules.georss.geometries.AbstractGeometry;
import com.rometools.modules.georss.geometries.Point;
import com.rometools.modules.georss.geometries.Position;
import com.rometools.rome.feed.module.Module;

import fr.cnes.regards.framework.geojson.Feature;
import fr.cnes.regards.framework.geojson.geometry.IGeometry;
import fr.cnes.regards.modules.entities.domain.AbstractEntity;
import fr.cnes.regards.modules.entities.domain.attribute.AbstractAttribute;
import fr.cnes.regards.modules.entities.domain.attribute.ObjectAttribute;
import fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.atom.modules.gml.impl.GmlTimeModuleImpl;
import fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.extension.IOpenSearchExtension;

public class GeoTimeExtension implements IOpenSearchExtension {

    private boolean activated = false;

    private String timeStartAttribute;

    private String timeEndAttribute;

    @Override
    public boolean isActivated() {
        return activated;
    }

    @Override
    public void applyExtensionToGeoJsonFeature(AbstractEntity entity, Feature feature) {
        feature.setGeometry(entity.getGeometry());
    }

    @Override
    public Module getAtomEntityBuilderModule(AbstractEntity entity, Gson gson) {
        // Add GML with time module to handle geo & time extension
        GmlTimeModuleImpl gmlMod = new GmlTimeModuleImpl();
        Optional<AbstractAttribute<?>> startDate = findAttributeByName(entity.getProperties(), timeStartAttribute);
        Optional<AbstractAttribute<?>> stopDate = findAttributeByName(entity.getProperties(), timeEndAttribute);
        if (startDate.isPresent() && (startDate.get().getValue() instanceof OffsetDateTime) && stopDate.isPresent()
                && (stopDate.get().getValue() instanceof OffsetDateTime)) {
            gmlMod.setStartDate((OffsetDateTime) startDate.get().getValue());
            gmlMod.setStopDate((OffsetDateTime) stopDate.get().getValue());
        }
        gmlMod.setGsonBuilder(gson);
        gmlMod.setGeometry(buildGeometry(entity.getGeometry()));
        return gmlMod;
    }

    @SuppressWarnings("unchecked")
    private static Optional<AbstractAttribute<?>> findAttributeByName(Collection<AbstractAttribute<?>> attributes,
            String timeAttributeName) {
        Optional<AbstractAttribute<?>> attribute = Optional.empty();
        String name = timeAttributeName;
        List<String> names = Lists.newArrayList(timeAttributeName.split("\\."));
        if (names.size() > 0) {
            name = names.remove(0);
        }
        for (AbstractAttribute<?> att : attributes) {
            if (name.equals(att.getName()) && (names.size() > 0) && (att instanceof ObjectAttribute)) {
                attribute = findAttributeByName((Set<AbstractAttribute<?>>) att.getValue(), String.join(".", names));
            } else if (name.equals(att.getName())) {
                attribute = Optional.of(att);
            }
        }
        return attribute;
    }

    private AbstractGeometry buildGeometry(IGeometry geometry) {
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

    public void setActivated(boolean activated) {
        this.activated = activated;
    }

    public String getTimeStartAttribute() {
        return timeStartAttribute;
    }

    public void setTimeStartAttribute(String timeStartAttribute) {
        this.timeStartAttribute = timeStartAttribute;
    }

    public String getTimeEndAttribute() {
        return timeEndAttribute;
    }

    public void setTimeEndAttribute(String timeEndAttribute) {
        this.timeEndAttribute = timeEndAttribute;
    }

}
