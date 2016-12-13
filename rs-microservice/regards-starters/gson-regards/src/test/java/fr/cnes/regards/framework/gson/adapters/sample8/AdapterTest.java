/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.gson.adapters.sample8;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import fr.cnes.regards.framework.gson.annotation.Gsonable;
import fr.cnes.regards.framework.gson.reflection.GsonAnnotationProcessor;

/**
 * Test with {@link Gsonable} annotation with default behaviour and {@link GsonAnnotationProcessor} processing.
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
    public void testSample8() {

        // Init GSON
        final GsonBuilder gsonBuilder = new GsonBuilder();
        // Sample 3 : create factory dynamically via reflection
        GsonAnnotationProcessor.processGsonable(gsonBuilder, this.getClass().getPackage().getName());
        final Gson gson = gsonBuilder.create();

        AbstractEntity entity = new Collection();
        final String jsonCollection = gson.toJson(entity);
        LOGGER.info(jsonCollection);
        final Collection collection = gson.fromJson(jsonCollection, Collection.class);

        Assert.assertTrue(Collection.NAME.equals(collection.getName()));

        entity = new Dataset();
        final String jsonDataset = gson.toJson(entity);
        LOGGER.info(jsonDataset);
        final Dataset dataset = gson.fromJson(jsonDataset, Dataset.class);

        Assert.assertTrue(Dataset.NAME.equals(dataset.getName()));
    }
}
