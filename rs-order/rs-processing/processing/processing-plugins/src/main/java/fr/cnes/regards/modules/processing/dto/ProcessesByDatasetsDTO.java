package fr.cnes.regards.modules.processing.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.vavr.collection.List;
import io.vavr.collection.Map;
import lombok.AllArgsConstructor;
import lombok.Value;

@Value @AllArgsConstructor(onConstructor_={@JsonCreator})
public class ProcessesByDatasetsDTO {

    @JsonValue
    Map<String, List<ProcessLabelDTO>> map;

}
