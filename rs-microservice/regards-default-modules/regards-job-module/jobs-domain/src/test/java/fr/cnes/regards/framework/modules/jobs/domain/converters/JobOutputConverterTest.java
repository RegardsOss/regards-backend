/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.modules.jobs.domain.converters;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.springframework.http.MediaType;

import fr.cnes.regards.framework.modules.jobs.domain.Output;
import fr.cnes.regards.framework.modules.jobs.domain.converters.JobOuputConverter;

/**
 * @author Léo Mieulet
 */
public class JobOutputConverterTest {

    @Test
    public void testConverter() throws URISyntaxException {
        final JobOuputConverter jobOutputConverter = new JobOuputConverter();
        final List<Output> outputList = new ArrayList<>();
        final Output output = new Output();
        output.setData(new URI("index.html"));
        output.setMimeType(MediaType.APPLICATION_JSON_VALUE);
        outputList.add(output);
        final String outputAsString = jobOutputConverter.convertToDatabaseColumn(outputList);
        final List<Output> outputAfterConvertion = jobOutputConverter.convertToEntityAttribute(outputAsString);
        Assertions.assertThat(outputList.get(0).getData()).isEqualTo(outputAfterConvertion.get(0).getData());
        Assertions.assertThat(outputList.get(0).getMimeType()).isEqualTo(outputAfterConvertion.get(0).getMimeType());
    }

    @Test
    public void testConverterWithUndefined() throws URISyntaxException {
        final JobOuputConverter jobOutputConverter = new JobOuputConverter();
        final List<Output> outputList = null;
        final List<Output> afterConvertion = jobOutputConverter.convertToEntityAttribute("");
        Assertions.assertThat(afterConvertion).isNull();

        final String outputAsString = jobOutputConverter.convertToDatabaseColumn(outputList);
        Assertions.assertThat(outputAsString).isEmpty();
    }
}
