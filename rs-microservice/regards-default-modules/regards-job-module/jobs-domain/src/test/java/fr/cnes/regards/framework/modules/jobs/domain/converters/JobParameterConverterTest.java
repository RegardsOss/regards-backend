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
