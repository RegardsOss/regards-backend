/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.gson.adapters.sample7;

/**
 * @author Marc Sordi
 *
 */
public class BooleanProperty extends AbstractProperty<Boolean> {

    private boolean value;

    @Override
    public Boolean getValue() {
        return isValue();
    }

    public boolean isValue() {
        return value;
    }

    public void setValue(boolean pValue) {
        value = pValue;
    }

}
