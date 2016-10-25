/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.security.annotation;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.core.annotation.AnnotationUtils;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

/**
 *
 * Class ResourceAccessAdapter
 *
 * GSON adapter for annotation ResorucesAccess
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
public class ResourceAccessAdapter extends TypeAdapter<ResourceAccess> {

    /**
     * Label for name field
     */
    public static final String NAME_LABEL = "name";

    /**
     * Label for description field
     */
    public static final String DESCRIPTION_LABEL = "description";

    @Override
    public void write(final JsonWriter pOut, final ResourceAccess pValue) throws IOException {

        pOut.beginObject();
        pOut.name(NAME_LABEL).value(pValue.name());
        pOut.name(DESCRIPTION_LABEL).value(pValue.description());
        pOut.endObject();

    }

    @Override
    public ResourceAccess read(final JsonReader pIn) throws IOException {
        final Map<String, Object> attributs = new HashMap<>();

        pIn.beginObject();
        while (pIn.hasNext()) {
            switch (pIn.nextName()) {
                case NAME_LABEL:
                    attributs.put(NAME_LABEL, pIn.nextString());
                    break;
                case DESCRIPTION_LABEL:
                    attributs.put(DESCRIPTION_LABEL, pIn.nextString());
                    break;
                default:
                    break;
            }
        }
        pIn.endObject();

        return AnnotationUtils.synthesizeAnnotation(attributs, ResourceAccess.class, null);
    }

}
