/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.gson.stream;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.stream.JsonReader;
import fr.cnes.regards.framework.gson.exception.GsonUtilException;

/**
 * GSON reader
 *
 * @author Marc Sordi
 *
 */
public final class GsonReader {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(GsonReader.class);

    private GsonReader() {
    }

    /**
     * Read object from input stream
     *
     * @param <T>
     *            object type
     * @param pInputStream
     *            input stream
     * @param pClass
     *            object type
     * @return object
     * @throws GsonUtilException
     *             if error occurs!
     */
    public static <T> T read(InputStream pInputStream, Class<T> pClass) throws GsonUtilException {
        final Gson gson = new GsonBuilder().setPrettyPrinting().create();

        // Read with GSON
        try (final JsonReader reader = new JsonReader(new InputStreamReader(pInputStream, "UTF-8"))) {
            return gson.fromJson(reader, pClass);
        } catch (IOException e) {
            final String message = String.format("Cannot init input stream reader for type %s.", pClass);
            LOGGER.error(message, e);
            throw new GsonUtilException(message);
        } catch (JsonParseException e) {
            final String message = String.format("Cannot read object of type %s.", pClass);
            LOGGER.error(message, e);
            throw new GsonUtilException(message);
        }
    }

}
