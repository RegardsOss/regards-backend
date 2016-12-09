/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.gson.adapters.sample3;

import fr.cnes.regards.framework.gson.annotation.Gsonable;

/**
 *
 * Test on animal objects
 *
 * @author Marc Sordi
 *
 */
@Gsonable
public class Animal {

    /**
     * Animal type
     */
    private final AnimalType type;

    public Animal(AnimalType pType) {
        this.type = pType;
    }

    public AnimalType getType() {
        return type;
    }
}
