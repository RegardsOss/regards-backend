/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.jobs.domain.converters;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import fr.cnes.regards.modules.jobs.domain.Output;

/**
 *
 */
public class JobOutputConverterTest {

    @Test
    public void testConverter() throws URISyntaxException {
        final JobOuputConverter jobOutputConverter = new JobOuputConverter();
        final List<Output> outputList = new ArrayList<>();
        final Output output = new Output();
        output.setData(new URI("index.html"));
        output.setMimeType("data/json");
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
