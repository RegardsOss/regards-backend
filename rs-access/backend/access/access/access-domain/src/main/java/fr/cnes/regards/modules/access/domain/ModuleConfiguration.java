/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.access.domain;

import java.util.List;

/**
 * 
 * @author cmertz
 *
 */
public class ModuleConfiguration {

    private Long id_;

    private Project project_;

    private List<ConfigParameter> parameters_;

    private Module module_;

    public ModuleConfiguration() {
		super();
	}

	public ModuleConfiguration(Project project, List<ConfigParameter> parameters, Module module) {
        super();
        project_ = project;
        parameters_ = parameters;
        module_ = module;
    }

    public Long getId() {
        return id_;
    }

    public void setId(Long id) {
        id_ = id;
    }

    public Project getProject() {
        return project_;
    }

    public void setProject(Project project) {
        project_ = project;
    }

    public List<ConfigParameter> getParameters() {
        return parameters_;
    }

    public void setParameters(List<ConfigParameter> parameters) {
        parameters_ = parameters;
    }

    public Module getModule() {
        return module_;
    }

    public void setModule(Module module) {
        module_ = module;
    }

}
