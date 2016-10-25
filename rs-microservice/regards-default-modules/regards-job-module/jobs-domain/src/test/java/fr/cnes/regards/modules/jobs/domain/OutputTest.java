/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.jobs.domain;

import java.net.URI;
import java.net.URISyntaxException;

import org.assertj.core.api.Assertions;
import org.junit.Test;

/**
 *
 */
public class OutputTest {

    @Test
    public void testDomain() throws URISyntaxException {
        final Output output = new Output();
        final URI uri = new URI("some/string");
        output.setData(uri);
        final String mimeType = "data/pdf";
        output.setMimeType(mimeType);
        Assertions.assertThat(output.getData()).isEqualTo(uri);
        Assertions.assertThat(output.getMimeType()).isEqualTo(mimeType);
    }
}
