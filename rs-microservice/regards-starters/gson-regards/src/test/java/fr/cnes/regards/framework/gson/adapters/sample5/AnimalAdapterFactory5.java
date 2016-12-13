/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.gson.adapters.sample5;

import fr.cnes.regards.framework.gson.adapters.PolymorphicTypeAdapterFactory;
import fr.cnes.regards.framework.gson.annotation.GsonTypeAdapterFactory;

/**
 * @author Marc Sordi
 *
 */
@GsonTypeAdapterFactory
public class AnimalAdapterFactory5 extends PolymorphicTypeAdapterFactory<Animal> {

    public AnimalAdapterFactory5() {
        super(Animal.class, "type");
        registerSubtype(Hawk.class, AnimalType.BIRD);
        registerSubtype(Lion.class, AnimalType.MAMMAL);
        registerSubtype(Shark.class, AnimalType.FISH);
    }

}
