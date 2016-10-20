/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.jobs.domain.converters;

import java.util.List;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import fr.cnes.regards.modules.jobs.domain.Output;

/**
 *
 */
@Converter(autoApply = true)
public class JobOuputConverter implements AttributeConverter<List<Output>, String> {

    private static final Logger LOG = LoggerFactory.getLogger(JobOuputConverter.class);

    private final ObjectMapper mapper = new ObjectMapper(); // jackson's objectmapper

    @Override
    public String convertToDatabaseColumn(final List<Output> pOuput) {
        mapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
        String json = "";
        try {
            json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(pOuput);
        } catch (final Exception e) {
            LOG.error("Failed to convert JobOutput POJO to string", e);
        }
        return json;
    }

    @Override
    public List<Output> convertToEntityAttribute(final String pDbData) {
        mapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
        List<Output> result = null;
        try {
            result = mapper.readValue(pDbData, new TypeReference<List<Output>>() {
            });
        } catch (final Exception e) {
            LOG.error("Failed to convert JobOutput persisted string to POJO", e);
        }
        return result;
    }

}