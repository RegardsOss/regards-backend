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

package fr.cnes.regards.modules.acquisition.service.plugins;

import java.io.File;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFile;
import fr.cnes.regards.modules.acquisition.domain.FileAcquisitionInformationsBuilder;
import fr.cnes.regards.modules.acquisition.domain.metadata.dto.MetaFileDto;
import fr.cnes.regards.modules.acquisition.domain.metadata.dto.MetaProductDto;
import fr.cnes.regards.modules.acquisition.domain.metadata.dto.SetOfMetaFileDto;
import fr.cnes.regards.modules.acquisition.plugins.IAcquisitionScanDirectoryPlugin;
import fr.cnes.regards.modules.acquisition.service.IMetaFileService;

/**
 * A default {@link Plugin} of type {@link IAcquisitionScanDirectoryPlugin}.
 *
 * @author Christophe Mertz
 * @since 1.0-SNAPSHOT
 */
@Plugin(id = "TestScanDirectoryPlugin", version = "1.0.0-SNAPSHOT",
        description = "Scan directories to detect incoming data files", author = "REGARDS Team",
        contact = "regards@c-s.fr", licence = "LGPLv3.0", owner = "CSSI", url = "https://github.com/RegardsOss")
public class TestScanDirectoryPlugin extends AbstractAcquisitionScanPlugin implements IAcquisitionScanDirectoryPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestScanDirectoryPlugin.class);

    @Autowired
    private IMetaFileService metaFileService;

    public static final String EXISTING_PRODUCT = "SMM_TUC_AXVCNE20081201_150235_19900101_000000_20380118_191407";

    private static final String DIR_DATA = "data";

    private static final String CHECKUM_ALGO = "SHA-256";

    @PluginParameter(name = CHAIN_GENERATION_PARAM, optional = true)
    String chainLabel;

    @PluginParameter(name = META_PRODUCT_PARAM, optional = true)
    MetaProductDto metaProductDto;

    // TODO CMZ à voir si fonctionne avec Set<MetaFileDto>
    @PluginParameter(name = META_FILE_PARAM, optional = true)
    SetOfMetaFileDto metaFiles;

    @Override
    public Set<AcquisitionFile> getAcquisitionFiles() {

        LOGGER.info("start scanning for the chain <{}> ", chainLabel);

        Set<AcquisitionFile> acqFileList = new HashSet<>();

        acqFileList.add(createAcquisitionFile("data", "PAUB_MESURE_TC_20130701_091200.TXT"));
        acqFileList.add(createAcquisitionFile("data", EXISTING_PRODUCT));

        LOGGER.info("end scanning for the chain <{}> ", chainLabel);

        return acqFileList;
    }

    private AcquisitionFile createAcquisitionFile(String dir, String name) {
        File file = new File(getClass().getClassLoader().getResource(dir + "/" + name).getFile());

        AcquisitionFile af = new AcquisitionFile();
        af.setAcquisitionInformations(FileAcquisitionInformationsBuilder.build(file.getParent().toString()).get());
        af.setFileName(file.getName());
        af.setSize(file.length());
        af.setAcqDate(OffsetDateTime.now());
        af.setAlgorithm(CHECKUM_ALGO);
        af.setChecksum(null);// TODO CMZ à compléter
        af.setStatus(null);

        MetaFileDto metaFileDto = metaFiles.getSetOfMetaFiles().iterator().next();
        af.setMetaFile(metaFileService.retrieve(metaFileDto.getId()));

        return af;
    }

    @Override
    public Set<File> getBadFiles() {
        LOGGER.info("Start reporting bad files for the chain <{}> ", chainLabel);
        Set<File> badFiles = new HashSet<>();
        badFiles.add(new File(getClass().getClassLoader()
                .getResource(DIR_DATA + "/" + "PAUB_MESURE_TC_20130701_XXXXX.TXT").getFile()));
        badFiles.add(new File(getClass().getClassLoader()
                .getResource(DIR_DATA + "/" + "PAUB_MESURE_TC_20130701_YYYYY.TXT").getFile()));
        LOGGER.info("End reporting bad files for the chain <{}> ", chainLabel);
        return badFiles;
    }

}
