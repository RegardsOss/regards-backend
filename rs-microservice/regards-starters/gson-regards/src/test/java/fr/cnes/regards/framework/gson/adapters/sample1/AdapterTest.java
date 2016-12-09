/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.gson.adapters.sample1;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;

/**
 * Test with {@link JsonAdapter} annotation. This test does not work for serialization! <br/>
 * {@link TypeAdapter} is only used when reading JSON and not writing. (GSON bug maybe)
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
     * Test {@link JsonAdapter}
     */
    @Test
    public void testSample1() {

        final GsonBuilder gsonBuilder = new GsonBuilder();
        // Sample 1 works with JsonAdapter annotation but cannot reach type adapter "write" (due to GSON bugs!!!!!!)
        final Gson gson = gsonBuilder.create();

        final Hawk hawk = new Hawk();

        final String jsonHawk = gson.toJson(hawk);
        LOGGER.info(jsonHawk);
        final Animal animal = gson.fromJson(jsonHawk, Animal.class);
        LOGGER.info("Animal type : " + animal.getType());

        Assert.assertTrue(animal instanceof Hawk);
    }
}
