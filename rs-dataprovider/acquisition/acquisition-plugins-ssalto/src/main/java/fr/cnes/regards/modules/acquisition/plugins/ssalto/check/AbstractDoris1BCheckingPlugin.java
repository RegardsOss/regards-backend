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
package fr.cnes.regards.modules.acquisition.plugins.ssalto.check;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.acquisition.exception.ReadFileException;
import fr.cnes.regards.modules.acquisition.plugins.ICheckFilePlugin;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.IDoris1BPlugin;

/**
 * Gere les prefixes pour les donnees Doris1B
 * 
 * @author Christophe Mertz
 *
 */
public abstract class AbstractDoris1BCheckingPlugin implements ICheckFilePlugin, IDoris1BPlugin {

    protected String productName;

    protected int productVersion;

    protected int fileVersion;

    protected String logFilePath;

    protected String nodeIdentifier;

    /**
     * Liste des correspondances DatasetName => Prexix
     */
    protected Map<String, String> prefixMap = null;

    @Override
    public boolean runPlugin(File fileToCheck, String dataSetId) throws ModuleException {
        boolean result = false;

        initPrefixMap();

        // Check file exists
        if (fileToCheck.exists() && fileToCheck.canRead()) {
            // DATA_STORAGE_OBJECT_IDENTIFIER
            if ((prefixMap != null) && prefixMap.containsKey(dataSetId)) {
                String prefix = prefixMap.get(dataSetId);
                nodeIdentifier = prefix + fileToCheck.getName();
            } else {
                throw new ModuleException("Prefix for " + dataSetId + "does not exist!");
            }
            productName = nodeIdentifier;
            productVersion = 1;
            fileVersion = 1;
            logFilePath = null;
            result = true;
        } else {
            throw new ReadFileException(fileToCheck.getAbsolutePath());
        }
        return result;
    }

    @Override
    public int getFileVersion() {
        return fileVersion;
    }

    @Override
    public String getLogFile() {
        return logFilePath;
    }

    @Override
    public String getNodeIdentifier() {
        return nodeIdentifier;
    }

    @Override
    public String getProductName() {
        return productName;
    }

    @Override
    public int getProductVersion() {
        return productVersion;
    }

    protected void addDatasetNamePrexif(String datasetName, String prefix) {
        if (prefixMap == null) {
            prefixMap = new HashMap<>();
        }
        prefixMap.put(datasetName, prefix);
    }
}
