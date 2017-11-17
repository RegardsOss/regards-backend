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
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;
import fr.cnes.regards.modules.acquisition.builder.FileAcquisitionInformationsBuilder;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFile;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFileStatus;
import fr.cnes.regards.modules.acquisition.domain.metadata.dto.MetaFileDto;
import fr.cnes.regards.modules.acquisition.domain.metadata.dto.MetaProductDto;
import fr.cnes.regards.modules.acquisition.domain.metadata.dto.SetOfMetaFileDto;
import fr.cnes.regards.modules.acquisition.plugins.IAcquisitionScanDirectoryPlugin;
import fr.cnes.regards.modules.acquisition.service.IMetaFileService;

/**
 * A simple {@link Plugin} of type {@link IAcquisitionScanDirectoryPlugin}.
 *
 * @author Christophe Mertz
 * @since 1.0-SNAPSHOT
 */
@Plugin(id = "TestScanDirectoryPlugin", version = "1.0.0-SNAPSHOT", description = "TestScanDirectoryPlugin",
        author = "REGARDS Team", contact = "regards@c-s.fr", licence = "LGPLv3.0", owner = "CSSI",
        url = "https://github.com/RegardsOss")
public class TestScanDirectoryPlugin extends AbstractAcquisitionScanPlugin implements IAcquisitionScanDirectoryPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestScanDirectoryPlugin.class);

    @Autowired
    private IMetaFileService metaFileService;

    private static final String DIR_DATA = "data";

    /**
     * @see MessageDigest for the possible values 
     */
    private static final String CHECKUM_ALGO = "SHA-256";

    @PluginParameter(name = CHAIN_GENERATION_PARAM, optional = true)
    private String chainLabel;

    @PluginParameter(name = LAST_ACQ_DATE_PARAM, optional = true)
    private String lastDateActivation;

    @PluginParameter(name = META_PRODUCT_PARAM, optional = true)
    private MetaProductDto metaProductDto;

    @PluginParameter(name = META_FILE_PARAM, optional = true)
    private SetOfMetaFileDto metaFiles;

    @Override
    public Set<AcquisitionFile> getAcquisitionFiles() {
        LOGGER.info("Start scan for the chain <{}> ", chainLabel);

        Set<AcquisitionFile> acqFileList = new HashSet<>();

        acqFileList.add(createAcquisitionFileMandatory(DIR_DATA, "PAUB_MESURE_TC_20130701_091200.TXT"));
        acqFileList.add(createAcquisitionFileMandatory(DIR_DATA, "PAUB_MESURE_TC_20130701_103715.TXT"));
        acqFileList.add(createAcquisitionFileMandatory(DIR_DATA, "PAUB_MESURE_TC_20130701_105909.TXT"));
        acqFileList
                .add(createAcquisitionFileMandatory(DIR_DATA,
                                                    "SMM_TUC_AXVCNE20081201_150235_19900101_000000_20380118_191407"));
        acqFileList.add(createAcquisitionFileMandatory("data/income",
                                                       "CS_OPER_AUX_DORUSO_20100704T073447_20100705T010524_0001.DBL"));

        AcquisitionFile optionalAcqFile = createAcquisitionFileOptional("data/income",
                                                                        "CS_OPER_AUX_DORUSO_20100704T073447_20100705T010524_0001.HDR");
        if (optionalAcqFile != null) {
            acqFileList.add(optionalAcqFile);
        }

        // Create an unknown acquisition file 
        acqFileList.add(createBadAcquisitionFile("data", "PAUB_MESURE_TC_20130701_091200.TXTXX"));

        LOGGER.info("End scan for the chain <{}> ", chainLabel);

        return acqFileList;
    }

    private AcquisitionFile createAcquisitionFileMandatory(String dir, String name) {
        MetaFileDto metaFileDto = getMetaFileOptional(false);
        File file = new File(getClass().getClassLoader().getResource(dir + "/" + name).getFile());
        AcquisitionFile af = initAcquisitionFile(file, metaFileService.retrieve(metaFileDto.getId()), CHECKUM_ALGO);
        af.setAcqDate(OffsetDateTime.now());

        return af;
    }

    private AcquisitionFile createAcquisitionFileOptional(String dir, String name) {
        MetaFileDto metaFileDto = getMetaFileOptional(true);

        if (metaFileDto == null) {
            return null;
        }

        File file = new File(getClass().getClassLoader().getResource(dir + "/" + name).getFile());
        AcquisitionFile af = initAcquisitionFile(file, metaFileService.retrieve(metaFileDto.getId()), CHECKUM_ALGO);
        af.setAcqDate(OffsetDateTime.now());

        return af;
    }

    MetaFileDto getMetaFileOptional(boolean optional) {
        MetaFileDto res = null;
        for (MetaFileDto mf : metaFiles.getSetOfMetaFiles()) {
            if (optional && !mf.getMandatory()) {
                res = mf;
            }
            if (!optional && mf.getMandatory()) {
                res = mf;
            }
        }
        return res;
    }

    private AcquisitionFile createBadAcquisitionFile(String dir, String name) {
        AcquisitionFile af = new AcquisitionFile();
        af.setAcquisitionInformations(FileAcquisitionInformationsBuilder.build(dir).get());
        af.setFileName(name);
        af.setSize(123456L);
        af.setAcqDate(OffsetDateTime.now());
        af.setChecksumAlgorithm(CHECKUM_ALGO);
        af.setChecksum("unknown file");
        af.setStatus(AcquisitionFileStatus.IN_PROGRESS);

        af.setMetaFile(metaFileService.retrieve(getMetaFileOptional(false).getId()));

        return af;
    }

    @Override
    public Set<File> getBadFiles() {
        LOGGER.info("Start report bad files for the chain <{}> ", chainLabel);
        Set<File> badFiles = new HashSet<>();
        badFiles.add(new File(getClass().getClassLoader()
                .getResource(DIR_DATA + "/" + "PAUB_MESURE_TC_20130701_XXXXX.TXT").getFile()));
        badFiles.add(new File(getClass().getClassLoader()
                .getResource(DIR_DATA + "/" + "PAUB_MESURE_TC_20130701_YYYYY.TXT").getFile()));
        LOGGER.info("End report bad files for the chain <{}> ", chainLabel);
        return badFiles;
    }

}
