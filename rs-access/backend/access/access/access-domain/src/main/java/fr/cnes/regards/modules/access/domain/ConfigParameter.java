/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.access.domain;

/**
 * 
 * @author cmertz
 *
 */
public class ConfigParameter implements IPair<String, String> {

	private String name;

	private String value;

	public ConfigParameter() {
		super();
	}

	public ConfigParameter(String name, String value) {
		this.name = name;
		this.value = value;
	}

	public String toString() {
		return this.name + " - " + this.value;
	}

	@Override
	public String getKey() {
		return this.name;
	}

	@Override
	public String getValue() {
		return this.value;
	}

	@Override
	public String setKey(String pKey) {
		this.name = pKey;
		return this.name;
	}

	@Override
	public String setValue(String pValue) {
		this.value = pValue;
		return this.value;
	}

}
