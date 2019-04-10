/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Test;

import fr.cnes.regards.framework.utils.file.compression.impl.GZipCompression;
import fr.cnes.regards.framework.utils.file.compression.impl.ZCompression;

/**
 * Test Z decompression
 *
 * @author Marc SORDI
 *
 */
public class CompressionTests {

    /**
     * Decompress a Z compressed file
     * @throws IOException
     */
    @Test
    public void decompressZFile() throws CompressionException, IOException {

        Path filePath = Paths.get("src", "test", "resources", "h2adata998.dat.Z");
        Path outputPath = Paths.get("target", "z");
        Files.createDirectories(outputPath);

        ZCompression util = new ZCompression();
        util.uncompress(filePath.toFile(), outputPath.toFile());
    }

    /**
     * Decompress a Z compressed file
     * @throws IOException
     */
    @Test
    public void decompressGZipFile() throws CompressionException, IOException {

        Path filePath = Paths.get("src", "test", "resources", "RINEX_0120.tar.gz");
        Path outputPath = Paths.get("target", "gzip");
        Files.createDirectories(outputPath);

        GZipCompression util = new GZipCompression();
        util.uncompress(filePath.toFile(), outputPath.toFile());
    }

}
