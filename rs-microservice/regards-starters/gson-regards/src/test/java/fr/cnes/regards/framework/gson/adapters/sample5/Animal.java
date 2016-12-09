/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.gson.adapters.sample5;

import fr.cnes.regards.framework.gson.annotation.GsonAdapterFactory;

/**
 *
 * Test on animal objects
 *
 * @author Marc Sordi
 *
 */
@GsonAdapterFactory(AnimalAdapterFactory5.class)
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
