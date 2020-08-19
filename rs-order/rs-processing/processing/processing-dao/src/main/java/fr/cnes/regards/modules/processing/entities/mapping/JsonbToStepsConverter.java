package fr.cnes.regards.modules.processing.entities.mapping;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import fr.cnes.regards.modules.processing.domain.PStep;
import fr.cnes.regards.modules.processing.domain.PStepSequence;
import fr.cnes.regards.modules.processing.entities.FileParameters;
import fr.cnes.regards.modules.processing.entities.Steps;
import io.r2dbc.postgresql.codec.Json;
import io.vavr.collection.Seq;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

@ReadingConverter
@AllArgsConstructor
public class JsonbToStepsConverter implements Converter<Json, Steps> {

    @Autowired private Gson gson;

    @Override public Steps convert(Json source) {
        return gson.fromJson(source.asString(), Steps.class);
    }
}
