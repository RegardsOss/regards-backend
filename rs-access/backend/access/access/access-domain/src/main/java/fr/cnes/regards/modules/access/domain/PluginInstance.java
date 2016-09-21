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
public class PluginInstance {

	private Long id_;

	private String name_;

	private Project project_;

	private List<ConfigParameter> parameters_;

	public PluginInstance() {
		super();
	}

	public PluginInstance(String name, Project project, List<ConfigParameter> parameters) {
		super();
		name_ = name;
		project_ = project;
		parameters_ = parameters;
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

}
