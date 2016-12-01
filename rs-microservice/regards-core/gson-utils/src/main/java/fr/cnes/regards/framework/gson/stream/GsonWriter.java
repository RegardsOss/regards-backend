/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.gson.stream;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.stream.JsonWriter;

import fr.cnes.regards.framework.gson.exception.GsonUtilException;

/**
 * GSON writer
 *
 * @author Marc Sordi
 *
 */
public final class GsonWriter {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(GsonWriter.class);

    private GsonWriter() {
    }

    /**
     * Write object to output stream
     *
     * @param <T>
     *            object type
     * @param pOutputStream
     *            output stream
     * @param pObject
     *            object to write
     * @param pClass
     *            object type
     * @throws GsonUtilException
     *             if error occurs!
     */
    public static <T> void write(OutputStream pOutputStream, T pObject, Class<T> pClass) throws GsonUtilException {
        final Gson gson = new GsonBuilder().setPrettyPrinting().create();

        // Write with GSON
        try (final JsonWriter writer = new JsonWriter(new OutputStreamWriter(pOutputStream, "UTF-8"))) {
            writer.setIndent("  ");
            gson.toJson(pObject, pClass, writer);
        } catch (IOException e) {
            final String message = String.format("Cannot init output stream writer for type %s.", pClass);
            LOGGER.error(message, e);
            throw new GsonUtilException(message);
        } catch (JsonParseException e) {
            final String message = String.format("Cannot write object of type %s.", pClass);
            LOGGER.error(message, e);
            throw new GsonUtilException(message);
        }
    }

}
