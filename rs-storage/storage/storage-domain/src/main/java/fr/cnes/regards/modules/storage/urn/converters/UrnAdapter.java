/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.urn.converters;

import java.io.IOException;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import fr.cnes.regards.modules.storage.urn.UniformResourceName;

/**
 * @author Sylvain Vissiere-Guerinet
 *
 */
public class UrnAdapter extends TypeAdapter<UniformResourceName> {

    @Override
    public UniformResourceName read(JsonReader pArg0) throws IOException {
        return UniformResourceName.fromString(pArg0.nextString());
    }

    @Override
    public void write(JsonWriter pArg0, UniformResourceName pArg1) throws IOException {
        pArg0.value(pArg1.toString());
    }

}
