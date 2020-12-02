/* Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
