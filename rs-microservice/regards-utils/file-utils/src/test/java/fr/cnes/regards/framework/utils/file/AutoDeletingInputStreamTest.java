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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * @author Thibaud Michaudel
 **/
public class AutoDeletingInputStreamTest {

    @TempDir
    Path tempDir;

    @Test
    public void test_delete_file_after_download() throws IOException {
        String fileContent = "This is the test file content";
        File sourceFile = tempDir.resolve("sourceFile.txt").toFile();
        File destFile = tempDir.resolve("destFile.txt").toFile();
        Files.writeString(sourceFile.toPath(), fileContent);
        try (AutoDeletingInputStream inputStream = new AutoDeletingInputStream(sourceFile)) {
            Assertions.assertTrue(Files.exists(sourceFile.toPath()));
            FileUtils.copyInputStreamToFile(inputStream, destFile);
        }
        Assertions.assertTrue(Files.notExists(sourceFile.toPath()));
        Assertions.assertEquals(fileContent, Files.readString(destFile.toPath()));
    }

    @Test
    public void test_delete_file_after_exception() throws IOException {
        byte[] fileData = "This is the test file content".getBytes();
        File sourceFile = tempDir.resolve("sourceFile.txt").toFile();
        Files.write(sourceFile.toPath(), fileData);
        try (AutoDeletingInputStream inputStream = new AutoDeletingInputStream(sourceFile)) {
            Assertions.assertTrue(Files.exists(sourceFile.toPath()));
            throw new TestException();
        } catch (TestException ignored) {
        }
        Assertions.assertTrue(Files.notExists(sourceFile.toPath()));
    }

    private static class TestException extends RuntimeException {

    }
}
