package fr.cnes.regards.modules.processing.dto;

import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.modules.processing.entity.RightsPluginConfiguration;
import io.vavr.collection.List;
import lombok.Value;

@Value
public class ProcessRightsDTO {

    String role;
    List<Long> datasets;

    public static ProcessRightsDTO fromRightsPluginConfiguration(RightsPluginConfiguration rights) {
        return new ProcessRightsDTO(rights.getRole(), List.ofAll(rights.getDatasets()));
    }

    public RightsPluginConfiguration toRightsPluginConfiguration(String tenant, PluginConfiguration config) {
        return new RightsPluginConfiguration(null, config, tenant, getRole(), getDatasets().toJavaList());
    }

}
