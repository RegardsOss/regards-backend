package fr.cnes.regards.modules.processing.dto;

import com.fasterxml.jackson.annotation.JsonCreator;

import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import io.vavr.collection.List;
import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor(onConstructor_ = { @JsonCreator })
public class ProcessPluginConfigurationRightsDTO {

    @Value
    @AllArgsConstructor(onConstructor_ = { @JsonCreator })
    public static class Rights {

        String role;

        List<String> datasets;

    }

    PluginConfiguration pluginConfiguration;

    Rights rights;

}
