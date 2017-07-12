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

import java.nio.file.FileSystems;
import java.nio.file.Path;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import fr.cnes.regards.framework.modules.jobs.domain.converters.PathConverter;

/**
 *
 */
public class PathConverterTest {

    @Test
    public void testConverter() {
        final PathConverter pathConverter = new PathConverter();
        final Path path = FileSystems.getDefault().getPath("logs", "access.log");
        final String pathAsString = pathConverter.convertToDatabaseColumn(path);
        final Path pathAfterConvertion = pathConverter.convertToEntityAttribute(pathAsString);
        Assertions.assertThat(path).isEqualTo(pathAfterConvertion);
    }

    @Test
    public void testConverterWithUndefinedPath() {
        final PathConverter pathConverter = new PathConverter();
        final Path path = FileSystems.getDefault().getPath("logs", "access.log");
        final String pathAsString = pathConverter.convertToDatabaseColumn(null);
        Assertions.assertThat(pathAsString).isEmpty();
    }
}
