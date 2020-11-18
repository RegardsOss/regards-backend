package fr.cnes.regards.modules.processing.entity.converter;

import com.google.gson.Gson;
import fr.cnes.regards.modules.processing.entity.FileStatsByDataset;
import io.r2dbc.postgresql.codec.Json;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

@WritingConverter
@AllArgsConstructor
public class FileStatsByDatasetToJsonbConverter implements Converter<FileStatsByDataset, Json> {

    @Autowired private Gson gson;
    
    @Override public Json convert(FileStatsByDataset source) {
        return Json.of(gson.toJson(source));
    }
}
