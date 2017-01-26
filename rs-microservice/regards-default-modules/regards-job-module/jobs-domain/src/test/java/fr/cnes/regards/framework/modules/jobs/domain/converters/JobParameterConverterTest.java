/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.modules.jobs.domain.converters;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import fr.cnes.regards.framework.modules.jobs.domain.JobParameters;
import fr.cnes.regards.framework.modules.jobs.domain.converters.JobParameterConverter;

/**
 * @author Léo Mieulet
 */
public class JobParameterConverterTest {

    @Test
    public void testConverter() {
        final JobParameterConverter jobParameterConverter = new JobParameterConverter();
        final JobParameters attribute = new JobParameters();
        final String key = "param1";
        final int value = 17;
        attribute.add(key, value);
        final String jobParametersAsString = jobParameterConverter.convertToDatabaseColumn(attribute);

        final JobParameters jobParametersAfterConvertion = jobParameterConverter
                .convertToEntityAttribute(jobParametersAsString);
        Assertions.assertThat(jobParametersAfterConvertion.getParameters().get(key)).isEqualTo(value);

    }

    @Test
    public void testConverterWithUndefined() {

        final JobParameterConverter jobParameterConverter = new JobParameterConverter();
        final String afterNullConvertion = jobParameterConverter.convertToDatabaseColumn(null);
        Assertions.assertThat(afterNullConvertion).isEmpty();

        final JobParameters jobAttribute = jobParameterConverter.convertToEntityAttribute("");
        Assertions.assertThat(jobAttribute.getParameters().size()).isEqualTo(0);

    }
}
