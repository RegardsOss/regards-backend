/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.gson.adapters.sample6;

import fr.cnes.regards.framework.gson.annotation.Gsonable;

/**
 * @author Marc Sordi
 *
 */
@Gsonable
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
