/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.framework.gson.adapters.sample8;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import fr.cnes.regards.framework.gson.GsonAnnotationProcessor;
import fr.cnes.regards.framework.gson.annotation.Gsonable;

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

        AbstractEntitySample entity = new CollectionSample();
        final String jsonCollection = gson.toJson(entity);
        LOGGER.info(jsonCollection);
        final CollectionSample collection = gson.fromJson(jsonCollection, CollectionSample.class);

        Assert.assertTrue(CollectionSample.NAME.equals(collection.getName()));

        entity = new DatasetSample();
        final String jsonDataset = gson.toJson(entity);
        LOGGER.info(jsonDataset);
        final DatasetSample dataset = gson.fromJson(jsonDataset, DatasetSample.class);

        Assert.assertTrue(DatasetSample.NAME.equals(dataset.getName()));
    }
}
