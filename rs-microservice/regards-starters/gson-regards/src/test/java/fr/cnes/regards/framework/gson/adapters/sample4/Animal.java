/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.gson.adapters.sample4;

import fr.cnes.regards.framework.gson.annotation.Gsonable;

/**
 *
 * Test on animal objects
 *
 * @author Marc Sordi
 *
 */
@Gsonable(value = "dtype")
public class Animal {

    /**
     * Animal type
     */
    private AnimalType type;

    public Animal(AnimalType pType) {
        this.type = pType;
    }

    public AnimalType getType() {
        return type;
    }

    public void setType(AnimalType pType) {
        type = pType;
    }
}
