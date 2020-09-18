package fr.cnes.regards.modules.processing.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import lombok.AllArgsConstructor;
import lombok.Value;

import java.util.UUID;

@Value
@AllArgsConstructor(onConstructor_={@JsonCreator})
public class ProcessLabelDTO {

    UUID processBusinessId;
    String label;

    public static ProcessLabelDTO fromPluginConfiguration(PluginConfiguration pc) {
        return  new ProcessLabelDTO(UUID.fromString(pc.getBusinessId()), pc.getLabel());
    }
}
