/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.gson.adapters.sample7;

import java.util.List;

/**
 * @author Marc Sordi
 *
 */
public class ObjectProperty extends AbstractProperty<List<AbstractProperty<?>>> {

    private List<AbstractProperty<?>> value;

    @Override
    public List<AbstractProperty<?>> getValue() {
        return value;
    }

    public void setValue(List<AbstractProperty<?>> pValue) {
        value = pValue;
    }
}
