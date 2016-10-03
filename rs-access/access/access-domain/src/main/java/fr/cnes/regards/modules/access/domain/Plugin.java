/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.access.domain;

/**
 * 
 * @author cmertz
 *
 */
public class Plugin {

	private Long id_;

	private String name_;

	private String description_;

	private PluginType pluginType_;

	private String mainJsfFile_;

	public Plugin() {
		super();
	}

	public Plugin(String name, String description, PluginType pluginType, String mainJsfFile) {
		super();
		name_ = name;
		description_ = description;
		pluginType_ = pluginType;
		mainJsfFile_ = mainJsfFile;
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

	public PluginType getPluginType() {
		return pluginType_;
	}

	public void setPluginType(PluginType pluginType) {
		pluginType_ = pluginType;
	}

	public String getMainJsfFile() {
		return mainJsfFile_;
	}

	public void setMainJsfFile(String mainJsfFile) {
		mainJsfFile_ = mainJsfFile;
	}

}
