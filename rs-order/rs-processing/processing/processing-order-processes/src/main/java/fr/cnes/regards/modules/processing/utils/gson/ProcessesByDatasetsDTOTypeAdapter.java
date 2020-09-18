package fr.cnes.regards.modules.processing.utils.gson;

import com.google.auto.service.AutoService;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;
import fr.cnes.regards.modules.processing.domain.PStep;
import fr.cnes.regards.modules.processing.domain.execution.ExecutionStatus;
import fr.cnes.regards.modules.processing.domain.step.PStepFinal;
import fr.cnes.regards.modules.processing.domain.step.PStepIntermediary;
import fr.cnes.regards.modules.processing.dto.ProcessLabelDTO;
import fr.cnes.regards.modules.processing.dto.ProcessesByDatasetsDTO;
import io.vavr.collection.List;
import io.vavr.collection.Map;

import java.time.OffsetDateTime;

@AutoService(TypedGsonTypeAdapter.class)
public class ProcessesByDatasetsDTOTypeAdapter implements TypedGsonTypeAdapter<ProcessesByDatasetsDTO> {

    @Override public Class<ProcessesByDatasetsDTO> type() {
        return ProcessesByDatasetsDTO.class;
    }

    @Override public JsonDeserializer<ProcessesByDatasetsDTO> deserializer() {
        return (json, typeOfT, context) -> {
            Map<String, List<ProcessLabelDTO>> map = context.deserialize(json, new TypeToken<Map<String, List<ProcessLabelDTO>>>(){}.getType());
            return new ProcessesByDatasetsDTO(map);
        };
    }

    @Override public JsonSerializer<ProcessesByDatasetsDTO> serializer() {
        return (src, typeOfSrc, context) -> context.serialize(src.getMap());
    }
}
