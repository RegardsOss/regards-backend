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

package fr.cnes.regards.modules.acquisition.step;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;

import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFile;
import fr.cnes.regards.modules.acquisition.domain.metadata.dto.MetaFileDto;
import fr.cnes.regards.modules.acquisition.domain.metadata.dto.MetaProductDto;
import fr.cnes.regards.modules.acquisition.domain.metadata.dto.SetOfMetaFileDto;
import fr.cnes.regards.modules.acquisition.plugins.IAcquisitionScanDirectoryPlugin;
import fr.cnes.regards.modules.acquisition.service.IMetaFileService;
import fr.cnes.regards.modules.acquisition.service.IMetaProductService;

/**
 * A default {@link Plugin} of type {@link IAcquisitionScanDirectoryPlugin}.
 *
 * @author Christophe Mertz
 * @since 1.0-SNAPSHOT
 */
@Plugin(id = "TestScanDirectorySameFileCheckSumDifferentPlugin", version = "1.0.0-SNAPSHOT",
        description = "Scan directories to detect incoming data files", author = "REGARDS Team",
        contact = "regards@c-s.fr", licence = "LGPLv3.0", owner = "CSSI", url = "https://github.com/RegardsOss")
public class TestScanDirectorySameFileCheckSumDifferentPlugin implements IAcquisitionScanDirectoryPlugin {

    @Autowired
    private IMetaFileService metaFileService;

    private static final String CHECKUM_ALGO = "SHA-256";

    public final static String META_PRODUCT_PARAM = "meta-produt";

    public final static String META_FILE_PARAM = "meta-file";

    @PluginParameter(name = META_PRODUCT_PARAM, optional = true)
    MetaProductDto metaProductDto;

    // TODO CMZ à voir si fonctionne avec Set<MetaFileDto>
    @PluginParameter(name = META_FILE_PARAM, optional = true)
    SetOfMetaFileDto metaFiles;

    @Override
    public Set<AcquisitionFile> getAcquisitionFiles() {

        //        MetaProduct metaProduct = metaProductService.retrieve(metaProductDto.getLabel());

        Set<AcquisitionFile> acqFileList = new HashSet<>();

        AcquisitionFile a = new AcquisitionFile();
        String aFileName = "Coucou";
        a.setFileName(aFileName);
        a.setSize(33L);
        MetaFileDto metaFileDto = metaFiles.getSetOfMetaFiles().iterator().next();
        a.setMetaFile(metaFileService.retrieve(metaFileDto.getId()));
        a.setAcqDate(OffsetDateTime.now());
        a.setAlgorithm(CHECKUM_ALGO);
        a.setChecksum("hello I have a new checksum");
        acqFileList.add(a);

        AcquisitionFile b = new AcquisitionFile();
        String bFileName = "Hello Toulouse";
        b.setFileName(bFileName);
        b.setSize(156L);
        metaFileDto = metaFiles.getSetOfMetaFiles().iterator().next();
        b.setMetaFile(metaFileService.retrieve(metaFileDto.getId()));
        b.setAcqDate(OffsetDateTime.now());
        b.setAlgorithm(CHECKUM_ALGO);
        b.setChecksum("this my own checksum");
        acqFileList.add(b);

        return acqFileList;
    }

}
