/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.gson.adapters.sample2;

import fr.cnes.regards.framework.gson.adapters.MultitenantPolymorphicTypeAdapterFactory;
import fr.cnes.regards.framework.multitenant.test.SingleRuntimeTenantResolver;

/**
 * @author Marc Sordi
 *
 */
public class MultitenantAnimalAdapterFactory2 extends MultitenantPolymorphicTypeAdapterFactory<Animal> {

    public MultitenantAnimalAdapterFactory2(String pTenant) {
        super(new SingleRuntimeTenantResolver(pTenant), Animal.class, "type", true);
        registerSubtype(pTenant, Hawk.class, "bird");
        registerSubtype(pTenant, Lion.class, "mammal");
        registerSubtype(pTenant, Shark.class, "fish");
    }
}
