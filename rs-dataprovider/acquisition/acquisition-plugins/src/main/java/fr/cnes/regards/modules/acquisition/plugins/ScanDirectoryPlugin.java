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

package fr.cnes.regards.modules.acquisition.plugins;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFile;
import fr.cnes.regards.modules.acquisition.domain.metadata.MetaFile;
import fr.cnes.regards.modules.acquisition.domain.metadata.dto.MetaFileDto;
import fr.cnes.regards.modules.acquisition.domain.metadata.dto.MetaProductDto;
import fr.cnes.regards.modules.acquisition.domain.metadata.dto.ScanDirectoryDto;
import fr.cnes.regards.modules.acquisition.domain.metadata.dto.SetOfMetaFileDto;
import fr.cnes.regards.modules.acquisition.service.IMetaFileService;

/**
 * Class ScanDirectoryPlugin A default {@link Plugin} of type {@link IConnectionPlugin}. Allows to
 *
 * @author Christophe Mertz
 * @since 1.0-SNAPSHOT
 */
@Plugin(id = "ScanDirectoryPlugin", version = "1.0.0-SNAPSHOT",
        description = "Scan directories to detect incoming data files", author = "REGARDS Team",
        contact = "regards@c-s.fr", licence = "LGPLv3.0", owner = "CSSI", url = "https://github.com/RegardsOss")
public class ScanDirectoryPlugin extends AbstractAcquisitionScanPlugin implements IAcquisitionScanDirectoryPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScanDirectoryPlugin.class);

    @Autowired
    private IMetaFileService metaFileService;

    private static final String CHECKUM_ALGO = "SHA-256";

    public static final String META_PRODUCT_PARAM = "meta-produt";

    public static final String META_FILE_PARAM = "meta-file";

    @PluginParameter(name = META_PRODUCT_PARAM, optional = true)
    MetaProductDto metaProductDto;

    // TODO CMZ à voir si fonctionne avec Set<MetaFileDto>
    @PluginParameter(name = META_FILE_PARAM, optional = true)
    SetOfMetaFileDto metaFiles;

    @Override
    public Set<AcquisitionFile> getAcquisitionFiles() {

        // TODO CMZ à compléter
        // pour chaque MetaFile
        // pour chaque ScanDirectory
        // tester date de dernière acquisition
        // chercher des fichiers vérifiant le pattern
        // créer des AcquisitionFile pour les fichiers trouvés

        Set<AcquisitionFile> acqFileList = new HashSet<>();

        for (MetaFileDto metaFileDto : metaFiles.getSetOfMetaFiles()) {

            LOGGER.info("ScanDirectoryPlugin : scan Metafile <" + metaFileDto.getFileNamePattern() + ">");

            MetaFile metaFile = metaFileService.retrieve(metaFileDto.getId());

            String filePattern = metaFileDto.getFileNamePattern();
            String adaptedPattern = getAdaptedPattern(filePattern);
            RegexFilenameFilter filter = new RegexFilenameFilter(adaptedPattern, Boolean.TRUE, Boolean.FALSE);

            for (ScanDirectoryDto scanDirectoryDto : metaFileDto.getScanDirectories()) {
                LOGGER.info("ScanDirectoryPlugin : scan directory <" + scanDirectoryDto.getScanDir() + ">");

                scanDirectories(scanDirectoryDto, filter, metaFile, acqFileList);
            }
        }

        return acqFileList;
    }

    private void scanDirectories(ScanDirectoryDto scanDirDto, RegexFilenameFilter filter, MetaFile metaFile,
            Set<AcquisitionFile> acqFileList) {
        String dirPath = scanDirDto.getScanDir();
        File dirFile = new File(dirPath);
        // Check if directory exists and is readable
        if (dirFile.exists() && dirFile.isDirectory() && dirFile.canRead()) {
            addMatchedFile(dirFile, scanDirDto, filter, metaFile, acqFileList);
        }

    }

    private void addMatchedFile(File dirFile, ScanDirectoryDto scanDirDto, RegexFilenameFilter filter,
            MetaFile metaFile, Set<AcquisitionFile> acqFileList) {
        // TODO CMZ ajouter lasAcdDate dans DTO
        // gérer la première acquisition où il n'y aura pas de Date
        List<File> filteredFileList = filteredFileList(dirFile, filter, 0);

        for (File baseFile : filteredFileList) {
            AcquisitionFile acqFile = initAcquisitionFile(metaFile, baseFile, null);

            // calculer checksum si configuré

            // initAcquisitionInformation

            Long lastModifiedDate = new Long(baseFile.lastModified());

            // TODO convertir en OffSetDateTime
            acqFile.setAcqDate(null);

            acqFileList.add(acqFile);

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("new file to acquire : " + acqFile.getFileName());
            }
        }
    }

}
