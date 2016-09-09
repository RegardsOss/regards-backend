package fr.cnes.regards.modules.access.domain;

import java.util.List;

public class Module {

    private Long id_;

    private String name_;

    private String description_;

    private List<ModuleConfiguration> configurations_;

    private ModuleType moduleType_;

    public Module(String name, String description, List<ModuleConfiguration> configurations, ModuleType moduleType) {
        super();
        name_ = name;
        description_ = description;
        configurations_ = configurations;
        moduleType_ = moduleType;
    }

    public Long getId() {
        return id_;
    }

    public void setId(Long id) {
        id_ = id;
    }

    public String getName() {
        return name_;
    }

    public void setName(String name) {
        name_ = name;
    }

    public String getDescription() {
        return description_;
    }

    public void setDescription(String description) {
        description_ = description;
    }

    public List<ModuleConfiguration> getConfigurations() {
        return configurations_;
    }

    public void setConfigurations(List<ModuleConfiguration> configurations) {
        configurations_ = configurations;
    }

    public ModuleType getModuleType() {
        return moduleType_;
    }

    public void setModuleType(ModuleType moduleType) {
        moduleType_ = moduleType;
    }

}
