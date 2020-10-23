package fr.cnes.regards.modules.order.dao.adapter;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import fr.cnes.regards.framework.gson.adapters.MapAdapter;
import fr.cnes.regards.framework.gson.annotation.GsonTypeAdapterBean;
import fr.cnes.regards.modules.order.domain.basket.StringToLongMap;

import java.io.IOException;
import java.util.Map;

@GsonTypeAdapterBean(adapted = StringToLongMap.class)
public class StringToLongMapTypeAdapter extends MapAdapter<StringToLongMap> {

    @Override
    public void write(JsonWriter out, StringToLongMap value) throws IOException {
        @SuppressWarnings("rawtypes")
        TypeAdapter<Map> mapAdapter = gson.getAdapter(Map.class);
        mapAdapter.write(out, value);
    }

    @Override
    public StringToLongMap read(JsonReader in) throws IOException {
        //let start by reading the opening brace, then lets handle each element thanks to readElement
        StringToLongMap result = new StringToLongMap();
        in.beginObject();
        while (in.peek() != JsonToken.END_OBJECT) {
            result.put(in.nextName(), (Long) readElement(in));
        }
        in.endObject();
        return result;
    }

}
