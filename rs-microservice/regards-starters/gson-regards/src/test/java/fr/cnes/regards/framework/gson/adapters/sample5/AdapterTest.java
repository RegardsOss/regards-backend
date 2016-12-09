/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.gson.adapters.sample5;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.JsonAdapter;

import fr.cnes.regards.framework.gson.annotation.GsonAdapterFactory;
import fr.cnes.regards.framework.gson.reflection.GsonAnnotationProcessor;

/**
 * Test with custom {@link GsonAdapterFactory} annotation.
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
    public void testSample5() {

        final GsonBuilder gsonBuilder = new GsonBuilder();
        GsonAnnotationProcessor.processGsonAdapterFactory(gsonBuilder, this.getClass().getPackage().getName());
        final Gson gson = gsonBuilder.create();

        final Hawk hawk = new Hawk();

        final String jsonHawk = gson.toJson(hawk);
        LOGGER.info(jsonHawk);
        final Animal animal = gson.fromJson(jsonHawk, Animal.class);
        LOGGER.info("Animal type : " + animal.getType());

        Assert.assertTrue(animal instanceof Hawk);
    }
}
