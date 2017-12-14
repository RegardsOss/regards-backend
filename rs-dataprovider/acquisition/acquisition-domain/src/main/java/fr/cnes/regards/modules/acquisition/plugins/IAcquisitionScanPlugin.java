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
import java.time.OffsetDateTime;
import java.util.Set;

import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginInterface;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFile;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionProcessingChain;
import fr.cnes.regards.modules.acquisition.domain.metadata.MetaFile;
import fr.cnes.regards.modules.acquisition.domain.metadata.MetaProduct;

/**
 * {@link Plugin} to scan data file.
 * 
 * @author Christophe Mertz
 *
 */
@PluginInterface(description = "Plugin to detect files to acquire")
public interface IAcquisitionScanPlugin {

    /**
     * Scan thee {@link Set} of {@link MetaFile} of a {@link MetaProduct}. 
     * @param chainLabel the {@link AcquisitionProcessingChain} label
     * @param metaProduct the {@link MetaProduct} of the file to scan
     * @param lastDateActivation the date of the previous acquisition for the current {@link AcquisitionProcessingChain} 
     * @return a {@link Set} of the {@link AcquisitionFile}
     */
    Set<AcquisitionFile> getAcquisitionFiles(String chainLabel, MetaProduct metaProduct,
            OffsetDateTime lastDateActivation);

    /**
     * Return the files that are scanned but they do not match the expected file of the {@link Set} of {@link MetaFile}.
     * @param chainLabel the {@link AcquisitionProcessingChain} label
     * @param metaFiles a {@link Set} of {@link MetaFile}
     * @return a {@link Set} of {@link File}
     */
    Set<File> getBadFiles(String chainLabel, Set<MetaFile> metaFiles);

}
