/**
 *
 */
package fr.cnes.regards.modules.jobs.domain.converters;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import fr.cnes.regards.modules.jobs.domain.JobParameters;

/**
 * 
 * @author Léo Mieulet
 * @author Christophe Mertz
 *
 */
@Converter(autoApply = true)
public class JobParameterConverter implements AttributeConverter<JobParameters, String> {

    /**
     * Logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(JobParameterConverter.class);

    /**
     * Jackson mapper
     */
    private final ObjectMapper mapper = new ObjectMapper(); // jackson's objectmapper

    @Override
    public String convertToDatabaseColumn(final JobParameters pAttribute) {
        String json = "";
        if (pAttribute != null) {
            mapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
            try {
                json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(pAttribute.getParameters());
            } catch (final IOException e) {
                LOG.error("Failed to convert JobParameter POJO to string", e);
            }
        }
        return json;
    }

    @Override
    public JobParameters convertToEntityAttribute(final String pDbData) {
        mapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
        final JobParameters jobParameters = new JobParameters();
        try {
            final Map<String, Object> map = mapper.readValue(pDbData, new TypeReference<Map<String, Object>>() {
            });
            for (final Entry<String, Object> anEntry : map.entrySet()) {
                jobParameters.add(anEntry.getKey(), anEntry.getValue());
            }
        } catch (final IOException e) {
            LOG.error("Failed to convert JobParameter persisted string to POJO", e);
        }
        return jobParameters;
    }

}