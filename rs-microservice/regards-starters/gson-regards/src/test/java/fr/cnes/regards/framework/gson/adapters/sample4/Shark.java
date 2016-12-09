/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.gson.adapters.sample4;

import fr.cnes.regards.framework.gson.annotation.GsonDiscriminator;

/**
 * @author Marc Sordi
 *
 */
@GsonDiscriminator("shark")
public class Shark extends Animal {

    public Shark() {
        super(AnimalType.FISH);
    }
}
