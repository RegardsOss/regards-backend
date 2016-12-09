/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.gson.adapters.sample4;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import fr.cnes.regards.framework.gson.annotation.Gsonable;
import fr.cnes.regards.framework.gson.reflection.GsonableAnnotationProcessor;

/**
 *
 * Test with {@link Gsonable} identifying an existing declaration field annotation and
 * {@link GsonableAnnotationProcessor} processing.
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
     * Dynamic factory
     */
    @Test
    public void testSample4() {

        // Init GSON
        final GsonBuilder gsonBuilder = new GsonBuilder();
        // Sample 3 : create factory dynamically via reflection
        GsonableAnnotationProcessor.process(gsonBuilder, this.getClass().getPackage().getName());
        final Gson gson = gsonBuilder.create();

        final Hawk hawk = new Hawk();

        final String jsonHawk = gson.toJson(hawk);
        LOGGER.info(jsonHawk);
        final Animal animal = gson.fromJson(jsonHawk, Animal.class);

        Assert.assertTrue(animal instanceof Hawk);
    }
}
