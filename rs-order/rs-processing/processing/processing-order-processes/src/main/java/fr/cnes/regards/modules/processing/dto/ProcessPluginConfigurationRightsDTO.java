package fr.cnes.regards.modules.processing.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.modules.processing.entity.RightsPluginConfiguration;
import io.vavr.collection.List;
import lombok.AllArgsConstructor;
import lombok.Value;

import java.util.UUID;

@Value
@AllArgsConstructor(onConstructor_={@JsonCreator})
public class ProcessPluginConfigurationRightsDTO {

    @Value
    @AllArgsConstructor(onConstructor_={@JsonCreator})
    public static class Rights {
        String role;
        List<String> datasets;
    }

    PluginConfiguration pluginConfiguration;
    Rights rights;


    public static ProcessPluginConfigurationRightsDTO fromRightsPluginConfiguration(RightsPluginConfiguration rights) {
        return new ProcessPluginConfigurationRightsDTO(
            rights.getPluginConfiguration(),
            new Rights(
                rights.getRole(),
                List.ofAll(rights.getDatasets())
            )
        );
    }

    public RightsPluginConfiguration toRightsPluginConfiguration(String tenant) {
        return new RightsPluginConfiguration(
            null,
            pluginConfiguration,
            UUID.fromString(pluginConfiguration.getBusinessId()),
            tenant,
            rights.role,
            rights.datasets.toJavaArray(String[]::new)
        );
    }

}
