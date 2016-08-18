package fr.cnes.regards.microservices.backend.pojo.common;

import org.springframework.hateoas.ResourceSupport;

import java.util.List;

public class Plugin extends ResourceSupport {
    private Long pluginId;
    private String label;
    private List<PluginParameter> metadata;

    public Plugin(Long pluginId, String label, List<PluginParameter> metadata) {
        this.pluginId = pluginId;
        this.label = label;
        this.metadata = metadata;
    }

    public Long getPluginId() {
        return pluginId;
    }

    public void setPluginId(Long pluginId) {
        this.pluginId = pluginId;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public List<PluginParameter> getMetadata() {
        return metadata;
    }

    public void setMetadata(List<PluginParameter> metadata) {
        this.metadata = metadata;
    }
}
