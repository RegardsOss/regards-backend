/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.gson.adapters.sample1;

import com.google.gson.annotations.JsonAdapter;

/**
 *
 * Test on animal objects
 *
 * @author Marc Sordi
 *
 */
@JsonAdapter(AnimalAdapterFactory1.class)
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
