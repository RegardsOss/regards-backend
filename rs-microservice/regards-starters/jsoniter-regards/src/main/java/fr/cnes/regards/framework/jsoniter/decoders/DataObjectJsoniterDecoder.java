package fr.cnes.regards.framework.jsoniter.decoders;

import com.jsoniter.JsonIterator;
import com.jsoniter.any.Any;
import com.jsoniter.spi.Decoder;
import com.jsoniter.spi.JsoniterSpi;
import com.jsoniter.spi.TypeLiteral;
import fr.cnes.regards.modules.dam.domain.entities.DataObject;
import fr.cnes.regards.modules.dam.domain.entities.feature.DataObjectFeature;
import fr.cnes.regards.modules.dam.domain.entities.metadata.DataObjectMetadata;
import fr.cnes.regards.modules.model.domain.Model;
import org.elasticsearch.common.geo.GeoPoint;

import java.io.IOException;
import java.util.Set;

public class DataObjectJsoniterDecoder implements AbstractEntityDecoder<DataObjectFeature, DataObject> {

    public static Decoder selfRegister() {
        Decoder decoder = new DataObjectJsoniterDecoder().nullSafe();
        JsoniterSpi.registerTypeDecoder(DataObject.class, decoder);
        return decoder;
    }

    @Override
    public Object decode(JsonIterator iter) throws IOException {
        Any dataObj = iter.readAny();
        DataObjectFeature feature = dataObj.as(DataObjectFeature.class, "feature");
        DataObject result = new DataObject(
            asOrNull(dataObj.get("model"), Model.class),
            feature
        );

        readCommonFields(dataObj, feature, result);

        whenPresent(dataObj.get("dataSourceId"), Long.class, result::setDataSourceId);
        whenPresent(dataObj.get("datasetModelNames"), new TypeLiteral<Set<String>>(){}, result::setDatasetModelNames);
        result.setInternal(dataObj.toBoolean("internal"));
        whenPresent(dataObj.get("metadata"), DataObjectMetadata.class, result::setMetadata);

        whenPresent(dataObj.get("nwPoint"), Any.class, gp -> {
            double lat = gp.toDouble("lat");
            double lon = gp.toDouble("lon");
            result.setNwPoint(new GeoPoint(lat, lon));
        });
        whenPresent(dataObj.get("sePoint"), Any.class, gp -> {
            double lat = gp.toDouble("lat");
            double lon = gp.toDouble("lon");
            result.setSePoint(new GeoPoint(lat, lon));
        });

        return result;
    }
}
