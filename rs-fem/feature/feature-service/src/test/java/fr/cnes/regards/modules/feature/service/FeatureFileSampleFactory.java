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
package fr.cnes.regards.modules.feature.service;

import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.modules.feature.domain.FeatureEntity;
import fr.cnes.regards.modules.feature.dto.FeatureFile;
import fr.cnes.regards.modules.feature.dto.FeatureFileAttributes;
import fr.cnes.regards.modules.feature.dto.FeatureFileLocation;
import fr.cnes.regards.modules.fileaccess.dto.FileLocationDto;
import fr.cnes.regards.modules.fileaccess.dto.FileReferenceDto;
import fr.cnes.regards.modules.fileaccess.dto.FileReferenceMetaInfoDto;
import fr.cnes.regards.modules.fileaccess.dto.request.RequestResultInfoDto;
import org.springframework.util.MimeType;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

public class FeatureFileSampleFactory {

    private final String filename;

    private final Long fileSize;

    private final String checksum;

    private final String url;

    private final String storage;

    public FeatureFileSampleFactory(String filename, Long fileSize, String checksum, String url) {
        this(filename, fileSize, checksum, url, "GPFS");
    }

    public FeatureFileSampleFactory(String filename, Long fileSize, String checksum, String url, String storage) {
        this.filename = filename;
        this.fileSize = fileSize;
        this.checksum = checksum;
        this.url = url;
        this.storage = storage;
    }

    protected FeatureFile buildSampleFeatureFile(String filename,
                                                 Long fileSize,
                                                 String checksum,
                                                 String url,
                                                 String storage) {
        FeatureFileAttributes attributes = FeatureFileAttributes.build(DataType.RAWDATA,
                                                                       MimeType.valueOf("application/octet-stream"),
                                                                       filename,
                                                                       fileSize,
                                                                       "MD5",
                                                                       checksum);

        FeatureFileLocation location = FeatureFileLocation.build(url, storage);
        return FeatureFile.build(attributes, location);
    }

    public FeatureFile buildFeatureFile() {
        return buildSampleFeatureFile(filename, fileSize, checksum, url, storage);
    }

    public RequestResultInfoDto buildStorageResult(FeatureEntity feature) {
        FileReferenceMetaInfoDto metaInfoDTO = new FileReferenceMetaInfoDto(checksum,
                                                                            "MD5",
                                                                            filename,
                                                                            fileSize,
                                                                            null,
                                                                            null,
                                                                            "application/octet-stream",
                                                                            null);
        FileLocationDto locationDTO = new FileLocationDto(storage, url);
        List<String> owners = Collections.singletonList(feature.getUrn().toString());
        FileReferenceDto referenceDTO = new FileReferenceDto(OffsetDateTime.now(), metaInfoDTO, locationDTO, owners);
        return RequestResultInfoDto.build("groupId", checksum, storage, url, owners, referenceDTO, null);
    }
}
