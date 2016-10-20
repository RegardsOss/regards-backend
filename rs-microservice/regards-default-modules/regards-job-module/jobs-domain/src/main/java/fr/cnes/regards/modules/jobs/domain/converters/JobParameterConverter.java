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

    private static final Logger LOG = LoggerFactory.getLogger(JobParameterConverter.class);

    private final ObjectMapper mapper = new ObjectMapper(); // jackson's objectmapper

    @Override
    public String convertToDatabaseColumn(JobParameters pAttribute) {
        mapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
        String json = "";
        try {
            json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(pAttribute.getMap());
        } catch (Exception e) {
            LOG.error("Failed to convert JobParameter POJO to string", e);
        }
        return json;
    }

    @Override
    public JobParameters convertToEntityAttribute(String pDbData) {
        mapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
        JobParameters jobParameters = new JobParameters();
        try {
            Map<String, Object> map = mapper.readValue(pDbData, new TypeReference<Map<String, Object>>() {
            });
            for (String key : map.keySet()) {
                jobParameters.add(key, map.get(key));
            }
        } catch (Exception e) {
            LOG.error("Failed to convert JobParameter persisted string to POJO", e);
        }
        return jobParameters;
    }

}