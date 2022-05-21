package fr.cnes.regards.framework.gson.adapters;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

import java.io.IOException;

/**
 * Gson adapter for {@link MimeType}
 *
 * @author Sylvain VISSIERE-GUERINET
 */
public class MimeTypeAdapter extends TypeAdapter<MimeType> {

    @Override
    public void write(JsonWriter out, MimeType value) throws IOException {
        out.value(value.toString());
    }

    @Override
    public MimeType read(JsonReader in) throws IOException {
        String mimeTypeString = in.nextString();
        return MimeTypeUtils.parseMimeType(mimeTypeString);
    }
}
