package fr.cnes.regards.modules.processing.dto;

import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.modules.processing.entity.RightsPluginConfiguration;
import io.vavr.collection.List;
import lombok.Value;

@Value
public class ProcessPluginConfigurationRightsDTO {

    @Value
    public static class Rights {
        String role;
        List<Long> datasets;
    }

    PluginConfiguration pluginConfiguration;
    Rights rights;


    public static ProcessPluginConfigurationRightsDTO fromRightsPluginConfiguration(RightsPluginConfiguration rights) {
        return new ProcessPluginConfigurationRightsDTO(rights.getPluginConfiguration(), new Rights(rights.getRole(), List.ofAll(rights.getDatasets())));
    }

    public RightsPluginConfiguration toRightsPluginConfiguration(String tenant) {
        return new RightsPluginConfiguration(null, pluginConfiguration, tenant, rights.role, rights.datasets.toJavaList());
    }

}
