/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.gson.adapters.sample8;

import fr.cnes.regards.framework.gson.annotation.Gsonable;

/**
 * @author Marc Sordi
 *
 */
@Gsonable
public abstract class AbstractEntitySample {

    /**
     * Collection name
     */
    public static final String NAME = "entity";

    /**
     * name to identify entity in test
     */
    private String name = NAME;

    public String getName() {
        return name;
    }

    public void setName(String pName) {
        name = pName;
    }

}
