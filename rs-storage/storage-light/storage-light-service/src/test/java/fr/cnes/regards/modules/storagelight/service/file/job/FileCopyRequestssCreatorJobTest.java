/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.storagelight.service.file.job;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for creation of copy requests
 *
 * @author Sébastien Binda
 *
 */
public class FileCopyRequestssCreatorJobTest {

    @Test
    public void calculateCopyPath() throws MalformedURLException {
        Optional<Path> filePath = FileCopyRequestsCreatorJob
                .getDestinationFilePath("file:/regards-input/storages/local/e1/f3/42/a1/123456789132456789",
                                        "/regards-input/storages/local/e1", "copied");
        Assert.assertTrue("Destination copy path should be created as the file is in the path to copy",
                          filePath.isPresent());
        Assert.assertEquals("Invalid copy destination path", "copied/f3/42/a1", filePath.get().toString());

        filePath = FileCopyRequestsCreatorJob
                .getDestinationFilePath("file:/regards-input/storages/local/e1/f3/42/a1/123456789132456789",
                                        "/regards-input/storages/local/e2", "copied");
        Assert.assertFalse("Destination copy path should be not created as the file is not in the path to copy",
                           filePath.isPresent());
    }

}
