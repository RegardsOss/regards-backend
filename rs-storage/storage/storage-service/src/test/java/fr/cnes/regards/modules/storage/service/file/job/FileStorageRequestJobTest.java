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
package fr.cnes.regards.modules.storage.service.file.job;

import fr.cnes.regards.modules.fileaccess.dto.request.FileStorageRequestAggregationDto;
import fr.cnes.regards.modules.storage.domain.database.FileReferenceMetaInfo;
import fr.cnes.regards.modules.storage.domain.database.request.FileStorageRequestAggregation;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.util.MimeType;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * @author sbinda
 */
public class FileStorageRequestJobTest {

    @Test
    public void testImageSizeCalculation() throws MalformedURLException, URISyntaxException {
        String owner = "test";
        String sessionOwner = "source 1";
        String session = "session 1";
        String checksum = "12345";
        String algorithm = "plop";
        String fileName = "file.test";
        Long fileSize = 20L;
        MimeType mimeType = MediaType.IMAGE_PNG;
        FileReferenceMetaInfo metaInfos = new FileReferenceMetaInfo(checksum, algorithm, fileName, fileSize, mimeType);
        String originUrl = "file://" + Paths.get("src/test/resources/input/cnes.png").toAbsolutePath().toString();
        String storage = "storage";
        String groupId = "10";
        FileStorageRequestAggregationDto request = new FileStorageRequestAggregation(owner,
                                                                                     metaInfos,
                                                                                     originUrl,
                                                                                     storage,
                                                                                     Optional.empty(),
                                                                                     groupId,
                                                                                     sessionOwner,
                                                                                     session).toDto();
        FileStorageRequestJob.calculateImageDimension(request);

        Assert.assertEquals(Integer.valueOf(499), request.getMetaInfo().getWidth());
        Assert.assertEquals(Integer.valueOf(362), request.getMetaInfo().getHeight());
    }

}
