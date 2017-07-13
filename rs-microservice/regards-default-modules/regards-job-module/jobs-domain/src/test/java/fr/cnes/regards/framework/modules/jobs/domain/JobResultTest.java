/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.modules.jobs.domain;

import java.net.URI;
import java.net.URISyntaxException;

import org.assertj.core.api.Assertions;
import org.junit.Test;

/**
 *
 */
public class JobResultTest {

    @Test
    public void testDomain() throws URISyntaxException {
        final JobResult jobResult = new JobResult();
        final URI uri = new URI("some/string");
        jobResult.setUri(uri);
        final String mimeType = "data/pdf";
        jobResult.setMimeType(mimeType);
        Assertions.assertThat(jobResult.getUri()).isEqualTo(uri);
        Assertions.assertThat(jobResult.getMimeType()).isEqualTo(mimeType);
    }
}
