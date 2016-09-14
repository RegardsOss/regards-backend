/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.core.deserializer;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

/**
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
    public LocalDateTime deserialize(JsonParser pP, DeserializationContext pCtxt)
            throws IOException, JsonProcessingException {
        // TODO Auto-generated method stub
        return LocalDateTime.parse(pP.getText(), DateTimeFormatter.ISO_DATE_TIME);
    }

}
