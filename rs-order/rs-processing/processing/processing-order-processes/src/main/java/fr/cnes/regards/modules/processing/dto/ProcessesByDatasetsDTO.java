package fr.cnes.regards.modules.processing.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.vavr.collection.List;
import io.vavr.collection.Map;
import lombok.AllArgsConstructor;
import lombok.Value;

@Value
public class ProcessesByDatasetsDTO {

    @JsonValue
    Map<String, List<ProcessLabelDTO>> map;

    @JsonCreator
    public ProcessesByDatasetsDTO(Map<String, List<ProcessLabelDTO>> map) {
        this.map = map;
    }
}
