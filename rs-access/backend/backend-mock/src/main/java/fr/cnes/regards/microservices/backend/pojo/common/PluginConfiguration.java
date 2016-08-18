package fr.cnes.regards.microservices.backend.pojo.common;

import org.springframework.hateoas.ResourceSupport;

import java.util.List;

public class PluginConfiguration extends ResourceSupport {
    private List<Object> description;
    private boolean label;
    private String pluginVersion;
    private int priorityOrder;
    private List<PluginParameter> pluginParameters;

    public PluginConfiguration(List<Object> description, boolean label, String pluginVersion, int priorityOrder, List<PluginParameter> pluginParameters) {
        this.description = description;
        this.label = label;
        this.pluginVersion = pluginVersion;
        this.priorityOrder = priorityOrder;
        this.pluginParameters = pluginParameters;
    }

    public List<Object> getDescription() {
        return description;
    }

    public void setDescription(List<Object> description) {
        this.description = description;
    }

    public boolean isLabel() {
        return label;
    }

    public void setLabel(boolean label) {
        this.label = label;
    }

    public String getPluginVersion() {
        return pluginVersion;
    }

    public void setPluginVersion(String pluginVersion) {
        this.pluginVersion = pluginVersion;
    }

    public int getPriorityOrder() {
        return priorityOrder;
    }

    public void setPriorityOrder(int priorityOrder) {
        this.priorityOrder = priorityOrder;
    }

    public List<PluginParameter> getPluginParameters() {
        return pluginParameters;
    }

    public void setPluginParameters(List<PluginParameter> pluginParameters) {
        this.pluginParameters = pluginParameters;
    }
}
