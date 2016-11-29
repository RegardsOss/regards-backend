/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.microservice.web;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

/**
 * @author Christophe Mertz
 *
 */
public class PathAdapter extends TypeAdapter<Path> {

    @Override
    public void write(JsonWriter pOut, Path pValue) throws IOException {
        pOut.beginObject();
        if (pValue != null) {
            pOut.name("path").value(pValue.toString());
        }
        pOut.endObject();
    }

    @Override
    public Path read(JsonReader pIn) throws IOException {
        return Paths.get(pIn.nextString());
    }

}
