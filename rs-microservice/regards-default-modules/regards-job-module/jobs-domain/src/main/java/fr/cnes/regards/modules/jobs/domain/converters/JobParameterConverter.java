/**
 *
 */
package fr.cnes.regards.modules.jobs.domain.converters;

import java.util.Map;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import fr.cnes.regards.modules.jobs.domain.JobParameters;

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
        mapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
        String json = "";
        try {
            json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(pAttribute.getParameters());
        } catch (final Exception e) {
            LOG.error("Failed to convert JobParameter POJO to string", e);
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
            for (final String key : map.keySet()) {
                jobParameters.add(key, map.get(key));
            }
        } catch (final Exception e) {
            LOG.error("Failed to convert JobParameter persisted string to POJO", e);
        }
        return jobParameters;
    }

}