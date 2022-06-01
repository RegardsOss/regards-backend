/*
 * Copyright 2017-20XX CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.utils.file.compression;

import fr.cnes.regards.framework.utils.file.compression.impl.GZipCompression;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.catchThrowable;

public class GzipExtractionShould {

    @ClassRule
    public static TemporaryFolder sourceFolder = new TemporaryFolder();

    private static File THE_EXTRACTION_FOLDER;

    private static File A_GZ_ARCHIVE;

    private static File A_GZIP_ARCHIVE;

    private static File A_GZIP_ARCHIVE_WITH_BAD_EXTENSION;

    private static File A_NOT_FOUND_ARCHIVE;

    private static File AN_ALREADY_EXTRACTED_ARCHIVE;

    private GZipCompression extractor;

    @BeforeClass
    public static void setup() throws Exception {
        THE_EXTRACTION_FOLDER = sourceFolder.newFolder("extraction");

        A_NOT_FOUND_ARCHIVE = sourceFolder.getRoot().toPath().resolve("notFound.gZ").toFile();
        A_GZ_ARCHIVE = generateGzipArchive("gz_archive.Gz");
        A_GZIP_ARCHIVE = generateGzipArchive("gzip_archive.GzIp");
        A_GZIP_ARCHIVE_WITH_BAD_EXTENSION = generateGzipArchive("archive.oops");
        AN_ALREADY_EXTRACTED_ARCHIVE = generateGzipArchive("alreadyExisting.Gz");
        Files.createFile(THE_EXTRACTION_FOLDER.toPath().resolve("alreadyExisting"));
    }

    private static File generateGzipArchive(String archiveName) throws Exception {
        File intoGzArchive = sourceFolder.getRoot().toPath().resolve(archiveName).toFile();
        File aRandomFile = generateAFile();
        compress(aRandomFile, intoGzArchive);
        aRandomFile.delete();
        return intoGzArchive;
    }

    private static void compress(File aFile, File intoGzArchive) throws IOException {
        try (GZIPOutputStream archiveStream = new GZIPOutputStream(new FileOutputStream(intoGzArchive));
            FileInputStream fileStream = new FileInputStream(aFile)) {
            byte[] buffer = new byte[1024];
            int amountRead = fileStream.read(buffer);
            while (amountRead > 0) {
                archiveStream.write(buffer);
                amountRead = fileStream.read(buffer);
            }
        }
    }

    private static File generateAFile() throws IOException {
        Path fileToCompress = sourceFolder.getRoot().toPath().resolve("file_to_compress");
        Files.write(fileToCompress, "some content".getBytes());
        return fileToCompress.toFile();
    }

    @Before
    public void setupExtractor() throws Exception {
        extractor = new GZipCompression();
    }

    @Test
    public void extract_gz_archive_into_given_folder() throws Exception {
        extractor.uncompress(A_GZ_ARCHIVE, THE_EXTRACTION_FOLDER);
        Path extractedFile = THE_EXTRACTION_FOLDER.toPath().resolve("gz_archive");
        assertThat(extractedFile).exists();
    }

    @Test
    public void raise_exception_if_extraction_fails() throws Exception {
        Throwable thrown = catchThrowable(() -> extractor.uncompress(A_NOT_FOUND_ARCHIVE, THE_EXTRACTION_FOLDER));
        assertThat(thrown) //
                           .isInstanceOf(CompressionException.class) //
                           .hasMessage("IO error during GZIP uncompression");
    }

    @Test
    public void verify_extracted_file_existence_before_extraction() throws Exception {
        Throwable thrown = catchThrowable(() -> extractor.uncompress(AN_ALREADY_EXTRACTED_ARCHIVE,
                                                                     THE_EXTRACTION_FOLDER));
        assertThat(thrown) //
                           .isInstanceOf(FileAlreadyExistException.class) //
                           .hasMessage("File alreadyExisting already exist");
    }

    @Test
    public void validate_archive_extension() throws Exception {
        Throwable thrown = catchThrowable(() -> extractor.uncompress(A_GZIP_ARCHIVE_WITH_BAD_EXTENSION,
                                                                     THE_EXTRACTION_FOLDER));

        assertThat(thrown) //
                           .isInstanceOf(CompressionException.class) //
                           .hasMessage("Extension of \""
                                       + A_GZIP_ARCHIVE_WITH_BAD_EXTENSION
                                       + "\" isn't valid."
                                       + " Valid extensions are : .gz, .gzip");
    }

    @Test
    public void handle_gzip_extension() throws Exception {
        extractor.uncompress(A_GZIP_ARCHIVE, THE_EXTRACTION_FOLDER);
        Path extractedFile = THE_EXTRACTION_FOLDER.toPath().resolve("gzip_archive");
        assertThat(extractedFile).exists();
    }
}