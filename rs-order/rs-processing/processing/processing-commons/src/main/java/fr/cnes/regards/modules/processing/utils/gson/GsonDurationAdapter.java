package fr.cnes.regards.modules.processing.utils.gson;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.time.Duration;

/**
 * A duration is represented by the number of nanoseconds in it (most precise possible form, albeit a bit longer).
 */
public class GsonDurationAdapter  extends TypeAdapter<Duration> {

    @Override public void write(JsonWriter out, Duration value) throws IOException {
        out.value(value.toNanos());
    }

    @Override public Duration read(JsonReader in) throws IOException {
        return Duration.ofNanos(in.nextLong());
    }
}
