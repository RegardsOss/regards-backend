/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.swagger.gson;

import java.io.IOException;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import springfox.documentation.spring.web.json.Json;

/**
 * SpringFox factory
 *
 * @author Marc Sordi
 *
 */
public class SpringFoxTypeFactory implements TypeAdapterFactory {

    @Override
    public <T> TypeAdapter<T> create(Gson pGson, TypeToken<T> pType) {

        if (pType.getRawType() != Json.class) {
            return null;
        }

        return new TypeAdapter<T>() {

            @Override
            public void write(JsonWriter pOut, T pValue) throws IOException {
                pOut.jsonValue(((Json) pValue).value());
            }

            @Override
            public T read(JsonReader pIn) throws IOException {
                throw new UnsupportedOperationException();
            }
        };
    }

}
