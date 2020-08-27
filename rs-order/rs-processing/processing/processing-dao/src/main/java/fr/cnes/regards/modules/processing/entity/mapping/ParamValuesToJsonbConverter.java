package fr.cnes.regards.modules.processing.entity.mapping;

import com.google.gson.Gson;
import fr.cnes.regards.modules.processing.entity.ParamValues;
import io.r2dbc.postgresql.codec.Json;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

@WritingConverter
@AllArgsConstructor
public class ParamValuesToJsonbConverter implements Converter<ParamValues, Json> {

    @Autowired private Gson gson;
    
    @Override public Json convert(ParamValues source) {
        return Json.of(gson.toJson(source));
    }
}
