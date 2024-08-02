/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.utils.file;

import org.apache.commons.io.FileUtils;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Test for {@link CompressToZipUtils}.
 * <p>The purpose of this test is to check if the tool to compress files to a zip is properly working.</p>
 * TEST PLAN :
 * <ul>
 *  <li>Nominal cases :
 *    <ul>
 *      <li>{@link #givenSmallFiles_whenCompressedToZip_thenZipCreated()}</li>
 *      <li>{@link #givenLargeFiles_whenCompressedToZip_thenZipCreated()} (ignored for performance reasons)</li>
 *    </ul></li>
 *  <li>Error cases :
 *    <ul>
 *      <li>{@link #givenSmallFiles_whenCompressedToZipInSameDir_thenError()}</li>
 *    </ul></li>
 * </ul>
 *
 * @author Iliana Ghazali
 **/
public class CompressToZipUtilsTest {

    private static final Path RESOURCES_PATH = Path.of("src/test/resources/zip_utils");

    private static final Path SMALL_FILES_PATH = RESOURCES_PATH.resolve("small");

    private static final Path LARGE_FILES_PATH = RESOURCES_PATH.resolve("large");

    private static final Path WORKSPACE_PATH = Path.of("target/zip_utils");

    @Before
    public void init() throws IOException {
        FileUtils.deleteDirectory(WORKSPACE_PATH.toFile());
        Files.createDirectory(WORKSPACE_PATH);
    }

    @Test
    public void givenSmallFiles_whenCompressedToZip_thenZipCreated() throws IOException {
        // GIVEN
        Path zipDestPath = WORKSPACE_PATH.resolve("small_zip.zip");
        // WHEN
        CompressToZipUtils.compressDirectoriesToZip(SMALL_FILES_PATH, zipDestPath);
        // THEN
        File zipFileCreated = zipDestPath.toFile();
        Assertions.assertThat(zipFileCreated).exists();
        Assertions.assertThat(zipFileCreated).hasSize(1862348L);
    }

    @Test
    @Ignore("Zipping large files size is ignored due to its long execution time. Remove this annotation if you want "
            + "to test specifically this case.")
    public void givenLargeFiles_whenCompressedToZip_thenZipCreated() throws IOException {
        // GIVEN
        generateBigFile();
        Path zipDestPath = WORKSPACE_PATH.resolve("large_zip.zip");
        // WHEN
        CompressToZipUtils.compressDirectoriesToZip(LARGE_FILES_PATH, zipDestPath);
        // THEN
        File zipFileCreated = zipDestPath.toFile();
        Assertions.assertThat(zipFileCreated).exists();
        Assertions.assertThat(zipFileCreated).hasSize(1528904L);
    }

    /**
     * Generate random file containing only 0 bits
     * As this file will be compressed, the zip will be very small
     */
    //
    private void generateBigFile() throws IOException {
        File file = new File(LARGE_FILES_PATH.toFile(), "large_file.raw");
        file.getParentFile().mkdirs();

        try (RandomAccessFile f = new RandomAccessFile(file, "rw")) {
            // 1.5 gb file size
            f.setLength(1024 * 1024 * 1500);
        }
    }

    @Test
    public void givenSmallFiles_whenCompressedToZipInSameDir_thenError() {
        // GIVEN
        Path zipDestPath = SMALL_FILES_PATH.resolve("small_zip.zip");
        // WHEN / THEN
        Assertions.assertThatThrownBy(() -> CompressToZipUtils.compressDirectoriesToZip(SMALL_FILES_PATH, zipDestPath))
                  .isInstanceOf(IOException.class)
                  .hasMessageContaining("path must not be contained");
    }
}
