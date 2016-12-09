/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.gson.adapters.sample5;

import fr.cnes.regards.framework.gson.adapters.PolymorphicTypeAdapterFactory;

/**
 * @author Marc Sordi
 *
 */
public class AnimalAdapterFactory5 extends PolymorphicTypeAdapterFactory<Animal> {

    public AnimalAdapterFactory5() {
        super(Animal.class, "type");
        registerSubtype(Hawk.class, AnimalType.BIRD);
        registerSubtype(Lion.class, AnimalType.MAMMAL);
        registerSubtype(Shark.class, AnimalType.FISH);
    }

}
