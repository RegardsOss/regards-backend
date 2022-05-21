package fr.cnes.regards.framework.jsoniter.decoders;

import com.jsoniter.any.Any;
import com.jsoniter.spi.TypeLiteral;
import fr.cnes.regards.framework.geojson.geometry.IGeometry;
import fr.cnes.regards.modules.dam.domain.entities.AbstractEntity;
import fr.cnes.regards.modules.dam.domain.entities.feature.EntityFeature;

import java.time.OffsetDateTime;
import java.util.Set;

public interface AbstractEntityDecoder<F extends EntityFeature, E extends AbstractEntity<F>>
    extends NullSafeDecoderBuilder {

    default void readCommonFields(Any jsonAny, F feature, E result) {
        result.setTags(feature.getTags());
        result.setIpId(feature.getId());

        result.setId(jsonAny.toLong("id"));

        whenPresent(jsonAny.get("wgs84"), IGeometry.class, result::setWgs84);
        whenPresent(jsonAny.get("creationDate"), OffsetDateTime.class, result::setCreationDate);
        whenPresent(jsonAny.get("lastUpdate"), OffsetDateTime.class, result::setLastUpdate);

        whenPresent(jsonAny.get("groups"), new TypeLiteral<Set<String>>() {

        }, result::setGroups);
    }

}
