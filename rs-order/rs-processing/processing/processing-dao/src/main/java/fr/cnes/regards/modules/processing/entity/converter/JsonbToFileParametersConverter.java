package fr.cnes.regards.modules.processing.entity.converter;

import com.google.gson.Gson;
import fr.cnes.regards.modules.processing.entity.FileParameters;
import io.r2dbc.postgresql.codec.Json;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

@ReadingConverter
@AllArgsConstructor
public class JsonbToFileParametersConverter implements Converter<Json, FileParameters> {

    @Autowired private Gson gson;

    @Override public FileParameters convert(Json source) {
        return gson.fromJson(source.asString(), FileParameters.class);
    }
}
