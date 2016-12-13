/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.gson.adapters.sample7;

/**
 * @author Marc Sordi
 *
 */
public abstract class AbstractProperty<T> implements IProperty<T> {

    protected String name;

    @Override
    public String getName() {
        return name;
    }

    public void setName(String pName) {
        name = pName;
    }
}
