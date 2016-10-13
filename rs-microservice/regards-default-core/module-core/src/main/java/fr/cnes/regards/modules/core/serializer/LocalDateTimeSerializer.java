/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.core.serializer;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

/**
 * Serializer for LocalDateTime serializing using ISO-8601
 *
 * @author svissier
 *
 */
public class LocalDateTimeSerializer extends JsonSerializer<LocalDateTime> {

    /*
     * (non-Javadoc)
     *
     * @see com.fasterxml.jackson.databind.JsonSerializer#serialize(java.lang.Object,
     * com.fasterxml.jackson.core.JsonGenerator, com.fasterxml.jackson.databind.SerializerProvider)
     */
    @Override
    public void serialize(LocalDateTime pValue, JsonGenerator pGen, SerializerProvider pSerializers)
            throws IOException {
        pGen.writeObject(pValue.format(DateTimeFormatter.ISO_DATE_TIME));

    }

}
