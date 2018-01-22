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
import java.util.ArrayList;
import java.util.List;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.acquisition.domain.Product;
import fr.cnes.regards.modules.acquisition.exception.ReadFileException;
import fr.cnes.regards.modules.acquisition.plugins.ICheckFilePlugin;

/**
 * Checks that the file exists and is accessible.<br>
 * The {@link Product} name is the file name less the extension file.
 * 
 * @author Christophe Mertz
 *
 */
public class CheckingFilePluginHelper implements ICheckFilePlugin {

    /**
     * The {@link Product} name
     */
    private String productName;

    /**
     * {@link List} of extension file that should be removed from the file name
     */
    protected final List<String> extensionList = new ArrayList<String>();

    public CheckingFilePluginHelper() {
        super();
    }

    /**
     * This methods checks that the file exists and is accessible.<br>
     * The {@link Product} name is the file name less the extension if the extension is presents in a {@link List}.
     */
    @Override
    public boolean runPlugin(File fileToCheck, String datasetId) throws ModuleException {
        boolean result = false;

        // Check file exists
        if (fileToCheck.exists() && fileToCheck.canRead()) {

            // Delete extension if any
            String name = fileToCheck.getName();
            int indexExtension;
            for (String extension : extensionList) {
                indexExtension = name.indexOf(extension);
                if (indexExtension != -1) {
                    // Compute product name
                    name = name.substring(0, indexExtension);
                    break;
                }
            }
            productName = name;
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
}
