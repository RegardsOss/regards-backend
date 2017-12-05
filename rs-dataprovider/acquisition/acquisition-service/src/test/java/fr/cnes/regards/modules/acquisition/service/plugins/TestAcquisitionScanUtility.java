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
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;

import fr.cnes.regards.modules.acquisition.builder.FileAcquisitionInformationsBuilder;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFile;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFileStatus;
import fr.cnes.regards.modules.acquisition.domain.metadata.MetaFile;
import fr.cnes.regards.modules.acquisition.service.IMetaFileService;

/**
 * 
 * @author Christophe Mertz
 *
 */
public class TestAcquisitionScanUtility extends AcquisitionScanPluginHelper {

    @Autowired
    private IMetaFileService metaFileService;

    private static final String CHECKUM_ALGO = "SHA-256";

    protected AcquisitionFile createAcquisitionFileMandatory(Set<MetaFile> metaFiles, String dir, String name) {
        MetaFile metaFile = getMetaFileOptional(metaFiles, false);
        File file = new File(getClass().getClassLoader().getResource(dir + "/" + name).getFile());
        AcquisitionFile af = initAcquisitionFile(file, metaFileService.retrieve(metaFile.getId()), CHECKUM_ALGO);
        af.setAcqDate(OffsetDateTime.now());

        return af;
    }

    protected AcquisitionFile createAcquisitionFileOptional(Set<MetaFile> metaFiles, String dir, String name) {
        MetaFile metaFile = getMetaFileOptional(metaFiles, true);

        if (metaFile == null) {
            return null;
        }

        File file = new File(getClass().getClassLoader().getResource(dir + "/" + name).getFile());
        AcquisitionFile af = initAcquisitionFile(file, metaFileService.retrieve(metaFile.getId()), CHECKUM_ALGO);
        af.setAcqDate(OffsetDateTime.now());

        return af;
    }

    protected MetaFile getMetaFileOptional(Set<MetaFile> metaFiles, boolean optional) {
        MetaFile res = null;
        for (MetaFile mf : metaFiles) {
            if (optional && !mf.getMandatory()) {
                res = mf;
            }
            if (!optional && mf.getMandatory()) {
                res = mf;
            }
        }
        return res;
    }

    protected AcquisitionFile createBadAcquisitionFile(Set<MetaFile> metaFiles, String dir, String name) {
        AcquisitionFile af = new AcquisitionFile();
        af.setAcquisitionInformations(FileAcquisitionInformationsBuilder.build(dir).get());
        af.setFileName(name);
        af.setSize(123456L);
        af.setAcqDate(OffsetDateTime.now());
        af.setChecksumAlgorithm(CHECKUM_ALGO);
        af.setChecksum("unknown file");
        af.setStatus(AcquisitionFileStatus.IN_PROGRESS);

        af.setMetaFile(metaFileService.retrieve(getMetaFileOptional(metaFiles, false).getId()));

        return af;
    }

}
