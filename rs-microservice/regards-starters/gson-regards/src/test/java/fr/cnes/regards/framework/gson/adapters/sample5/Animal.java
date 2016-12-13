/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.gson.adapters.sample5;

/**
 *
 * Test on animal objects
 *
 * @author Marc Sordi
 *
 */
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
