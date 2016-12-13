/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.gson.adapters.sample7;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.framework.gson.adapters.PolymorphicTypeAdapterFactory;
import fr.cnes.regards.framework.gson.annotation.GsonTypeAdapterFactoryBean;

/**
 * @author Marc Sordi
 *
 */
@GsonTypeAdapterFactoryBean
@SuppressWarnings("rawtypes")
public class CustomPolymorphicTypeAdapterFactory extends PolymorphicTypeAdapterFactory<AbstractProperty> {

    /**
     * Class logger
     */
    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(CustomPolymorphicTypeAdapterFactory.class);

    protected CustomPolymorphicTypeAdapterFactory() {
        super(AbstractProperty.class, "name", false);
        registerSubtype(DateProperty.class, "date");
        registerSubtype(StringProperty.class, "string");
        registerSubtype(StringProperty.class, "CRS");
        registerSubtype(ObjectProperty.class, "GEO");
    }
}
