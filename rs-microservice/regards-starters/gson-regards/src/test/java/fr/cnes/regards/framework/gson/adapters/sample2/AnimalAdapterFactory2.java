/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.gson.adapters.sample2;

import fr.cnes.regards.framework.gson.adapters.PolymorphicTypeAdapterFactory;

/**
 * @author Marc Sordi
 *
 */
public class AnimalAdapterFactory2 extends PolymorphicTypeAdapterFactory<Animal> {

    public AnimalAdapterFactory2(boolean pInjectField) {
        super(Animal.class, "type", pInjectField);
        registerSubtype(Hawk.class, "bird");
        registerSubtype(Lion.class, "mammal");
        registerSubtype(Shark.class, "fish");
    }

}
