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
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.modules.acquisition.exception.ReadFileException;
import fr.cnes.regards.modules.acquisition.plugins.ICheckFilePlugin;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.IDoris1BPlugin;

/**
 * Manage Doris1B data prefixs.<br>
 * This {@link Plugin} checks that the file exists and is accessible and that the extension file is authorized.
 * 
 * @author Christophe Mertz
 *
 */
public abstract class AbstractDoris1BCheckingPlugin implements ICheckFilePlugin, IDoris1BPlugin {

    protected String productName;

    /**
     * {@link Map} of dataset name prefixs
     */
    protected Map<String, String> prefixMap = null;

    @Override
    public boolean runPlugin(String chainLabel, File fileToCheck, String datasetId) throws ModuleException {
        boolean result = false;

        initPrefixMap();

        // Check file exists
        if (fileToCheck.exists() && fileToCheck.canRead()) {
            if ((prefixMap != null) && prefixMap.containsKey(datasetId)) {
                String prefix = prefixMap.get(datasetId);
                productName = prefix + fileToCheck.getName();
            } else {
                throw new ModuleException("Prefix for " + datasetId + " does not exist!");
            }
            result = true;
        } else {
            throw new ReadFileException(fileToCheck.getAbsolutePath());
        }
        return result;
    }

    @Override
    public String getProductName() {
        return productName;
    }

    protected void addDatasetNamePrexif(String datasetName, String prefix) {
        if (prefixMap == null) {
            prefixMap = new HashMap<>();
        }
        prefixMap.put(datasetName, prefix);
    }
}
