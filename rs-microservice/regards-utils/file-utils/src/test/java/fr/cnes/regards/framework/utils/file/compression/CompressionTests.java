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
package fr.cnes.regards.framework.utils.file.compression;

import fr.cnes.regards.framework.utils.file.compression.impl.ZCompression;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

public class CompressionTests {

    @Rule
    public TemporaryFolder extractFolder = new TemporaryFolder();

    @Test
    public void decompressZFile() throws CompressionException, IOException {
        Path filePath = Paths.get("src", "test", "resources", "h2adata998.dat.Z");
        File outputPath = extractFolder.newFolder("z");

        ZCompression util = new ZCompression();
        util.uncompress(filePath.toFile(), outputPath);

        Path extractedFile = outputPath.toPath().resolve("h2adata998.dat");
        assertThat(extractedFile).exists();
    }
}
