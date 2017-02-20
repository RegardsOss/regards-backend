/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.gson.adapters.sample2;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import fr.cnes.regards.framework.gson.adapters.PolymorphicTypeAdapterFactory;

/**
 * Test with a programmatic implementation of the {@link PolymorphicTypeAdapterFactory} with explicit sub types
 * declaration.
 *
 * @author Marc Sordi
 *
 */
public class AdapterTest {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(AdapterTest.class);

    /**
     * Do not inject missing field
     */
    @Test(expected = JsonParseException.class)
    public void testSample2MissingField() {

        // Init GSON
        final GsonBuilder gsonBuilder = new GsonBuilder();
        // Sample 2 has to reach both write and read method of type adapter
        // So we register factory programmatically (JsonAdapter doesn't work at the moment)
        gsonBuilder.registerTypeAdapterFactory(new AnimalAdapterFactory2(false));
        final Gson gson = gsonBuilder.create();

        final Hawk hawk = new Hawk();

        gson.toJson(hawk);
    }

    /**
     * Inject missing field
     */
    @Test
    public void testSample2() {

        // Init GSON
        final GsonBuilder gsonBuilder = new GsonBuilder();
        // Sample 2 has to reach both write and read method of type adapter
        // So we register factory programmatically (JsonAdapter doesn't work at the moment)
        gsonBuilder.registerTypeAdapterFactory(new AnimalAdapterFactory2(true));
        final Gson gson = gsonBuilder.create();

        final Hawk hawk = new Hawk();

        final String jsonHawk = gson.toJson(hawk);
        LOGGER.info(jsonHawk);
        final Animal animal = gson.fromJson(jsonHawk, Animal.class);

        Assert.assertTrue(animal instanceof Hawk);
    }

    /**
     * Multitenant factory test
     */
    @Test
    public void testMultitenant() {

        final GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapterFactory(new MultitenantAnimalAdapterFactory2("PROJECT"));
        final Gson gson = gsonBuilder.create();

        final Hawk hawk = new Hawk();

        final String jsonHawk = gson.toJson(hawk);
        LOGGER.info(jsonHawk);
        final Animal animal = gson.fromJson(jsonHawk, Animal.class);

        Assert.assertTrue(animal instanceof Hawk);
    }
}
