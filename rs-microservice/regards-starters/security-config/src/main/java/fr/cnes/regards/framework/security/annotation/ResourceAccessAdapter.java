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

import fr.cnes.regards.framework.security.role.DefaultRole;

/**
 *
 * Class ResourceAccessAdapter
 *
 * GSON adapter for annotation {@link ResourceAccess}
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
public class ResourceAccessAdapter extends TypeAdapter<ResourceAccess> {

    /**
     * Label for name field
     */
    protected static final String ROLE_LABEL = "role";

    /**
     * Label for description field
     */
    protected static final String DESCRIPTION_LABEL = "description";

    @Override
    public void write(final JsonWriter pOut, final ResourceAccess pValue) throws IOException {

        pOut.beginObject();
        pOut.name(ROLE_LABEL).value(pValue.role().toString());
        pOut.name(DESCRIPTION_LABEL).value(pValue.description());
        pOut.endObject();
    }

    @Override
    public ResourceAccess read(final JsonReader pIn) throws IOException {
        final Map<String, Object> attributs = new HashMap<>();

        pIn.beginObject();
        while (pIn.hasNext()) {
            switch (pIn.nextName()) {
                case ROLE_LABEL:
                    attributs.put(ROLE_LABEL, DefaultRole.valueOf(pIn.nextString()));
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

    /**
     *
     * Instanciate a new ResourceAccess annotation
     *
     * @param pDescription
     *            description
     * @param pDefaultRole
     *            default role
     * @return {@link ResourceAccess}
     * @since 1.0-SNAPSHOT
     */
    public static ResourceAccess createResourceAccess(final String pDescription, final DefaultRole pDefaultRole) {
        final Map<String, Object> attributs = new HashMap<>();
        if (pDescription != null) {
            attributs.put(DESCRIPTION_LABEL, pDescription);
        }
        if (pDefaultRole != null) {
            attributs.put(ROLE_LABEL, pDefaultRole);
        }
        return AnnotationUtils.synthesizeAnnotation(attributs, ResourceAccess.class, null);
    }

}
