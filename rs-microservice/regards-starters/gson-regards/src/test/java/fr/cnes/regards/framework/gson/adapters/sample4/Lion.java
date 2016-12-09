/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.gson.adapters.sample4;

import fr.cnes.regards.framework.gson.annotation.GsonDiscriminator;

/**
 * @author Marc Sordi
 *
 */
@GsonDiscriminator("lion")
public class Lion extends Animal {

    public Lion() {
        super(AnimalType.MAMMAL);
    }

}
