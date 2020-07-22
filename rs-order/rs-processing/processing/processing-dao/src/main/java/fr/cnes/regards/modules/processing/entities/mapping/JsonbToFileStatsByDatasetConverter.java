package fr.cnes.regards.modules.processing.entities.mapping;

import com.google.gson.Gson;
import fr.cnes.regards.modules.processing.entities.FileStatsByDataset;
import io.r2dbc.postgresql.codec.Json;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

@ReadingConverter
@AllArgsConstructor
public class JsonbToFileStatsByDatasetConverter implements Converter<Json, FileStatsByDataset> {

    @Autowired private Gson gson;

    @Override public FileStatsByDataset convert(Json source) {
        return gson.fromJson(source.asString(), FileStatsByDataset.class);
    }
}
