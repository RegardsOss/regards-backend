/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.jpa.utils.deserializer;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

/**
 * Deserializer for LocalDateTime serializing using ISO-8601
 *
 * @author svissier
 *
 */
public class LocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {

    /*
     * (non-Javadoc)
     *
     * @see com.fasterxml.jackson.databind.JsonDeserializer#deserialize(com.fasterxml.jackson.core.JsonParser,
     * com.fasterxml.jackson.databind.DeserializationContext)
     */
    @Override
    public LocalDateTime deserialize(JsonParser pP, DeserializationContext pCtxt) throws IOException {
        return LocalDateTime.parse(pP.getText(), DateTimeFormatter.ISO_DATE_TIME);
    }

}
