package fr.cnes.regards.modules.processing.entity.converter;

import com.google.gson.Gson;
import fr.cnes.regards.modules.processing.entity.ParamValues;
import io.r2dbc.postgresql.codec.Json;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.core.convert.converter.Converter;

@ReadingConverter
@AllArgsConstructor
public class JsonbToParamValuesConverter  implements Converter<Json, ParamValues> {

    @Autowired private Gson gson;

    @Override public ParamValues convert(Json source) {
        return gson.fromJson(source.asString(), ParamValues.class);
    }
}
