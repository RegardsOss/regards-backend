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
package fr.cnes.regards.framework.geojson.gson;

import java.io.IOException;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import fr.cnes.regards.framework.geojson.coordinates.Position;
import fr.cnes.regards.framework.gson.annotation.GsonTypeAdapter;

/**
 * Gson adapter for {@link Position}
 * @author Marc Sordi
 */
@GsonTypeAdapter(adapted = Position.class)
public class PositionTypeAdapter extends TypeAdapter<Position> {

    @Override
    public void write(JsonWriter out, Position value) throws IOException {
        out.beginArray();
        // Write longitude
        out.value(value.getLongitude());
        // Write latitude
        out.value(value.getLatitude());
        // Optionally write altitude
        if (value.getAltitude().isPresent()) {
            out.value(value.getAltitude().get());
        }
        out.endArray();
    }

    @Override
    public Position read(JsonReader in) throws IOException {
        Position parsed;

        in.beginArray();
        // Read longitude
        double longitude = in.nextDouble();
        // Read latitude
        double latitude = in.nextDouble();
        // Optionally read altitude
        if (in.peek().equals(JsonToken.NUMBER)) {
            double altitude = in.nextDouble();
            parsed = new Position(longitude, latitude, altitude);
        } else {
            parsed = new Position(longitude, latitude);
        }
        in.endArray();

        return parsed;
    }

}
