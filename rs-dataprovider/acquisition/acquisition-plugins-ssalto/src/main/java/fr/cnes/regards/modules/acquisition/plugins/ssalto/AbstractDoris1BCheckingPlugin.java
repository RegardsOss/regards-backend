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
package fr.cnes.regards.modules.acquisition.plugins.ssalto;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.acquisition.plugins.ICheckFilePlugin;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.exception.ReadFileException;

/**
 * Gere les prefixes pour les donnees Doris1B
 * 
 * @author Christophe Mertz
 *
 */
public abstract class AbstractDoris1BCheckingPlugin implements ICheckFilePlugin, IDoris1BPlugin {

    protected String productName_;

    protected int productVersion_;

    protected int fileVersion_;

    protected String logFilePath_;

    protected String nodeIdentifier_;

    /**
     * Liste des correspondances DatasetName => Prexix
     */
    protected Map<String, String> prefixMap_ = null;

    @Override
    public boolean runPlugin(File pFileToCheck, String pDataSetId) throws ModuleException {
        boolean result = false;

        initPrefixMap();

        // Check file exists
        if (pFileToCheck.exists() && pFileToCheck.canRead()) {
            // DATA_STORAGE_OBJECT_IDENTIFIER
            if ((prefixMap_ != null) && prefixMap_.containsKey(pDataSetId)) {
                String prefix = prefixMap_.get(pDataSetId);
                nodeIdentifier_ = prefix + pFileToCheck.getName();
            } else {
                throw new ModuleException("Prefix for " + pDataSetId + "does not exist!");
            }
            productName_ = nodeIdentifier_;
            productVersion_ = 1;
            fileVersion_ = 1;
            logFilePath_ = null;
            result = true;
        } else {
            throw new ReadFileException(pFileToCheck.getAbsolutePath());
        }
        return result;
    }

    @Override
    public int getFileVersion() {
        return fileVersion_;
    }

    @Override
    public String getLogFile() {
        return logFilePath_;
    }

    @Override
    public String getNodeIdentifier() {
        return nodeIdentifier_;
    }

    @Override
    public String getProductName() {
        return productName_;
    }

    @Override
    public int getProductVersion() {
        return productVersion_;
    }

    protected void addDatasetNamePrexif(String pDatasetName, String pPrefix) {
        if (prefixMap_ == null) {
            prefixMap_ = new HashMap<>();
        }
        prefixMap_.put(pDatasetName, pPrefix);
    }
}
