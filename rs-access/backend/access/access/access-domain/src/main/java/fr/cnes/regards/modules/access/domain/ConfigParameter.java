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

	/**
	 * 
	 */
    private String name_;

	/**
	 * 
	 */
    private String value_;

	/**
	 * 
	 */
    public ConfigParameter() {
		super();
	}

	/**
	 * 
	 * @param name
	 * @param value
	 */
    public ConfigParameter(final String pName, final String pValue) {
		this.name_ = pName;
		this.value_ = pValue;
	}


    /**
     * @return name_ + "-" value_
     */
    public final String toString() {
		return this.name_ + " - " + this.value_;
	}

	@Override
    public final String getKey() {
		return this.name_;
	}

	@Override
    public final String getValue() {
		return this.value_;
	}

	@Override
    public final String setKey(String pKey) {
		this.name_ = pKey;
		return this.name_;
	}

	@Override
    public final String setValue(String pValue) {
		this.value_ = pValue;
		return this.value_;
	}

}
