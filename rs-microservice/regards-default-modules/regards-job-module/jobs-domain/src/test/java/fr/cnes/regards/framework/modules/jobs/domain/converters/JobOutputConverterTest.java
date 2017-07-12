/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
