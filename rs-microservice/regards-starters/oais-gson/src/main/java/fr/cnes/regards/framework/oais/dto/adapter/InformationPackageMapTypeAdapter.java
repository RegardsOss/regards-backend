package fr.cnes.regards.framework.oais.dto.adapter;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import fr.cnes.regards.framework.gson.adapters.MapAdapter;
import fr.cnes.regards.framework.gson.annotation.GsonTypeAdapterBean;
import fr.cnes.regards.framework.oais.dto.InformationPackageMapDto;

import java.io.IOException;
import java.util.Map;

/**
 * Hack Gson to deserialize long as longs and not double when no structure is given.
 * {@link InformationPackageMapTypeAdapter#read(JsonReader)} is greatly inspired from {@link com.google.gson.internal.bind.ObjectTypeAdapter}
 *
 * @author Sylvain VISSIERE-GUERINET
 */
@GsonTypeAdapterBean(adapted = InformationPackageMapDto.class)
public class InformationPackageMapTypeAdapter extends MapAdapter<InformationPackageMapDto> {

    @Override
    public void write(JsonWriter out, InformationPackageMapDto value) throws IOException {
        @SuppressWarnings("rawtypes") TypeAdapter<Map> mapAdapter = gson.getAdapter(Map.class);
        mapAdapter.write(out, value);

    }

    @Override
    public InformationPackageMapDto read(JsonReader in) throws IOException {
        //let start by reading the opening brace, then lets handle each element thanks to readElement
        InformationPackageMapDto result = new InformationPackageMapDto();
        in.beginObject();
        while (in.peek() != JsonToken.END_OBJECT) {
            result.put(in.nextName(), readElement(in));
        }
        in.endObject();
        return result;
    }

}
