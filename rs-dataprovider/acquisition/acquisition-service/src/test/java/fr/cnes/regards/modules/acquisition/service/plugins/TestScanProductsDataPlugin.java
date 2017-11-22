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

import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFile;
import fr.cnes.regards.modules.acquisition.domain.metadata.MetaFile;
import fr.cnes.regards.modules.acquisition.domain.metadata.MetaProduct;
import fr.cnes.regards.modules.acquisition.plugins.IAcquisitionScanDirectoryPlugin;

/**
 * A test {@link Plugin} of type {@link IAcquisitionScanDirectoryPlugin}.
 *
 * @author Christophe Mertz
 */
@Plugin(id = "TestScanProductsData", version = "1.0.0-SNAPSHOT", description = "TestScanProductsData",
        author = "REGARDS Team", contact = "regards@c-s.fr", licence = "LGPLv3.0", owner = "CSSI",
        url = "https://github.com/RegardsOss")
public class TestScanProductsDataPlugin extends TestAcquisitionScanUtility implements IAcquisitionScanDirectoryPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestScanProductsDataPlugin.class);

    @Override
    public Set<AcquisitionFile> getAcquisitionFiles(String chainLabel, MetaProduct metaProduct,
            OffsetDateTime lastDateActivation) {
        LOGGER.info("Start scan for the chain <{}> ", chainLabel);

        Set<AcquisitionFile> acqFileList = new HashSet<>();

        acqFileList.add(createAcquisitionFileMandatory(metaProduct.getMetaFiles(), "data/income",
                                                       "CS_OPER_STR1DAT_0__20100705T063000_20100705T064959_0001.DBL"));
        acqFileList.add(createAcquisitionFileMandatory(metaProduct.getMetaFiles(), "data/income",
                                                       "CS_OPER_STR1DAT_0__20100805T103000_20100805T110137_0001.DBL"));
        acqFileList.add(createAcquisitionFileMandatory(metaProduct.getMetaFiles(), "data/income",
                                                       "CS_OPER_TLM_DRTM___20100704T000000_20100704T231234_0005.DBL"));

        LOGGER.info("End scan for the chain <{}> ", chainLabel);

        return acqFileList;
    }

    @Override
    public Set<File> getBadFiles(String chainLabel, Set<MetaFile> metaFiles) {
        LOGGER.info("Start report bad files for the chain <{}> ", chainLabel);
        return new HashSet<>();
    }

}
