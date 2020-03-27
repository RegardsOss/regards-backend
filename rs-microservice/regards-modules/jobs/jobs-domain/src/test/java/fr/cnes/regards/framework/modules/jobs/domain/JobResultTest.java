/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
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
