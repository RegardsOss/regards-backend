package fr.cnes.regards.framework.jsoniter.decoders;

import com.jsoniter.JsonIterator;
import com.jsoniter.any.Any;
import com.jsoniter.spi.Decoder;
import com.jsoniter.spi.JsoniterSpi;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.modules.dam.domain.entities.Dataset;
import fr.cnes.regards.modules.dam.domain.entities.feature.DatasetFeature;
import fr.cnes.regards.modules.dam.domain.entities.metadata.DatasetMetadata;
import fr.cnes.regards.modules.indexer.domain.criterion.ICriterion;
import fr.cnes.regards.modules.model.domain.Model;

import java.io.IOException;

public class DatasetJsoniterDecoder implements AbstractEntityDecoder<DatasetFeature, Dataset> {

    public static Decoder selfRegister() {
        Decoder decoder = new DatasetJsoniterDecoder().nullSafe();
        JsoniterSpi.registerTypeDecoder(Dataset.class, decoder);
        return decoder;
    }

    @Override
    public Object decode(JsonIterator iter) throws IOException {
        Any dataset = iter.readAny();
        DatasetFeature feature = asOrNull(dataset, DatasetFeature.class, "feature");

        Dataset result = new Dataset(
                dataset.as(Model.class, "model"),
                feature,
                asOrNull(dataset, PluginConfiguration.class, "plgConfDataSource"),
                dataset.toString("dataModel"),
                asOrNull(dataset, ICriterion.class, "subsettingClause"),
                dataset.toString("openSearchSubsettingClause"),
                asOrNull(dataset, DatasetMetadata.class, "metadata")
        );

        readCommonFields(dataset, feature, result);

        return result;
    }

}
