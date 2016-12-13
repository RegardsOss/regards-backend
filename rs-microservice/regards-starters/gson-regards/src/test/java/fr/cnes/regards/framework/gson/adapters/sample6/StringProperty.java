/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.gson.adapters.sample6;

/**
 * @author Marc Sordi
 *
 */
public class StringProperty extends AbstractProperty<String> {

    /**
     * Property value
     */
    private String value;

    @Override
    public String getValue() {
        return value;
    }

    public void setValue(String pValue) {
        value = pValue;
    }

}
