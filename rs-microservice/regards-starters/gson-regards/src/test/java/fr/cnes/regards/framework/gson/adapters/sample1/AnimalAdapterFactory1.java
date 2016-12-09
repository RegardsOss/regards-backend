/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.gson.adapters.sample1;

import fr.cnes.regards.framework.gson.adapters.PolymorphicTypeAdapterFactory;

/**
 * @author Marc Sordi
 *
 */
public class AnimalAdapterFactory1 extends PolymorphicTypeAdapterFactory<Animal> {

    public AnimalAdapterFactory1() {
        super(Animal.class, "type");
        registerSubtype(Hawk.class, AnimalType.BIRD);
        registerSubtype(Lion.class, AnimalType.MAMMAL);
        registerSubtype(Shark.class, AnimalType.FISH);
    }

}
