package fr.cnes.regards.modules.configuration.domain;

public class ConfigurationDTO {

	private String configuration;

	public ConfigurationDTO(String configuration) {
		super();
		this.configuration = configuration;
	}
	
	public String getConfiguration() {
		return configuration;
	}

	public void setConfiguration(String configuration) {
		this.configuration = configuration;
	}
}
