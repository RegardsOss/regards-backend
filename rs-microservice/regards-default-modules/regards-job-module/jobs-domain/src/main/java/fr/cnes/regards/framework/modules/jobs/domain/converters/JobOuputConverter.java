/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.modules.jobs.domain.converters;

import java.io.IOException;
import java.util.List;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import fr.cnes.regards.framework.modules.jobs.domain.Output;

/**
 * 
 * @author Léo Mieulet
 * @author Christophe Mertz
 *
 */
@Converter(autoApply = true)
public class JobOuputConverter implements AttributeConverter<List<Output>, String> {

    /**
     * Logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(JobOuputConverter.class);

    /**
     * Jackson mapper
     */
    private final ObjectMapper mapper = new ObjectMapper(); // jackson's objectmapper

    @Override
    public String convertToDatabaseColumn(final List<Output> pOutput) {
        String json = "";
        if (pOutput != null) {
            mapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
            try {
                json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(pOutput);
            } catch (final IOException e) {
                LOG.error("Failed to convert JobOutput POJO to string", e);
            }
        }
        return json;
    }

    @Override
    public List<Output> convertToEntityAttribute(final String pDbData) {
        List<Output> result = null;
        if (pDbData != null && !pDbData.isEmpty()) {
            mapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);

            try {
                result = mapper.readValue(pDbData, new TypeReference<List<Output>>() {
                });
            } catch (final IOException e) {
                LOG.error("Failed to convert JobOutput persisted string to POJO", e);
            }
        }
        return result;
    }

}