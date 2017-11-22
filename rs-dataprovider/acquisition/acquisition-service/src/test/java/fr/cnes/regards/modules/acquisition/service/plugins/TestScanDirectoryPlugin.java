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

import com.google.common.base.Strings;

import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFile;
import fr.cnes.regards.modules.acquisition.domain.metadata.MetaFile;
import fr.cnes.regards.modules.acquisition.domain.metadata.MetaProduct;
import fr.cnes.regards.modules.acquisition.plugins.IAcquisitionScanDirectoryPlugin;

/**
 * A simple {@link Plugin} of type {@link IAcquisitionScanDirectoryPlugin}, with two dynamic parameters.
 *
 * @author Christophe Mertz
 */
@Plugin(id = "TestScanDirectoryPlugin", version = "1.0.0-SNAPSHOT", description = "TestScanDirectoryPlugin",
        author = "REGARDS Team", contact = "regards@c-s.fr", licence = "LGPLv3.0", owner = "CSSI",
        url = "https://github.com/RegardsOss")
public class TestScanDirectoryPlugin extends TestAcquisitionScanUtility implements IAcquisitionScanDirectoryPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestScanDirectoryPlugin.class);

    private static final String DIR_DATA = "data";

    public static final String PARAM_1_NAME = "param-1";

    public static final String PARAM_2_NAME = "param-2";

    @PluginParameter(name = PARAM_1_NAME, optional = true)
    private String param1;

    @PluginParameter(name = PARAM_2_NAME, optional = true)
    private String param2;

    @Override
    public Set<AcquisitionFile> getAcquisitionFiles(String chainLabel, MetaProduct metaProduct,
            OffsetDateTime lastDateActivation) {
        LOGGER.info("Start scan for the chain <{}> ", chainLabel);

        if (Strings.isNullOrEmpty(param1) || Strings.isNullOrEmpty(param2)) {
            LOGGER.error("one or more plugin parameters are missing for plugin <{}>", getClass().getCanonicalName());
            return null;
        }

        Set<AcquisitionFile> acqFileList = new HashSet<>();

        acqFileList.add(createAcquisitionFileMandatory(metaProduct.getMetaFiles(), DIR_DATA,
                                                       "PAUB_MESURE_TC_20130701_091200.TXT"));
        acqFileList.add(createAcquisitionFileMandatory(metaProduct.getMetaFiles(), DIR_DATA,
                                                       "PAUB_MESURE_TC_20130701_103715.TXT"));
        acqFileList.add(createAcquisitionFileMandatory(metaProduct.getMetaFiles(), DIR_DATA,
                                                       "PAUB_MESURE_TC_20130701_105909.TXT"));
        acqFileList
                .add(createAcquisitionFileMandatory(metaProduct.getMetaFiles(), DIR_DATA,
                                                    "SMM_TUC_AXVCNE20081201_150235_19900101_000000_20380118_191407"));
        acqFileList.add(createAcquisitionFileMandatory(metaProduct.getMetaFiles(), "data/income",
                                                       "CS_OPER_AUX_DORUSO_20100704T073447_20100705T010524_0001.DBL"));

        AcquisitionFile optionalAcqFile = createAcquisitionFileOptional(metaProduct
                .getMetaFiles(), "data/income", "CS_OPER_AUX_DORUSO_20100704T073447_20100705T010524_0001.HDR");
        if (optionalAcqFile != null) {
            acqFileList.add(optionalAcqFile);
        }

        // Create an unknown acquisition file 
        acqFileList.add(createBadAcquisitionFile(metaProduct.getMetaFiles(), "data",
                                                 "PAUB_MESURE_TC_20130701_091200.TXTXX"));

        LOGGER.info("End scan for the chain <{}> ", chainLabel);

        return acqFileList;
    }

    @Override
    public Set<File> getBadFiles(String chainLabel, Set<MetaFile> metaFiles) {
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
