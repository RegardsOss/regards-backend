package fr.cnes.regards.modules.processing.entity.mapping;

import com.google.gson.Gson;
import fr.cnes.regards.modules.processing.entity.Steps;
import io.r2dbc.postgresql.codec.Json;
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
