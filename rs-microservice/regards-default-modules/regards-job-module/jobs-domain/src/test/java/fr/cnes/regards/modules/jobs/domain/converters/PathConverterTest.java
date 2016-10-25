/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.jobs.domain.converters;

import java.nio.file.FileSystems;
import java.nio.file.Path;

import org.assertj.core.api.Assertions;
import org.junit.Test;

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
