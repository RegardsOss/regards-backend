/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
 * @author Marc Sordi
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
