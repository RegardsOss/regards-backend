/*
 * LICENSE_PLACEHOLDER
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
