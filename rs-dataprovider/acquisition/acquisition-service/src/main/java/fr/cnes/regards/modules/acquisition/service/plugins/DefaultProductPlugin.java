/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.nio.file.Path;
import java.util.List;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;
import fr.cnes.regards.modules.acquisition.plugins.IProductPlugin;

/**
 * Compute the product name removing extension from filename
 *
 * @author Marc Sordi
 *
 */
@Plugin(id = "DefaultProductPlugin", version = "1.0.0-SNAPSHOT",
        description = "Compute the product name from filename optionnaly removing extension or/and truncating product name",
        author = "REGARDS Team", contact = "regards@c-s.fr", license = "GPLv3", owner = "CSSI",
        url = "https://github.com/RegardsOss")
public class DefaultProductPlugin implements IProductPlugin {

    public static final String FIELD_REMOVE_EXT = "removeExtension";

    public static final String FIELD_EXTS = "extensions";

    public static final String FIELD_LENGTH = "maxLength";

    public static final String FIELD_LENGTH_REQUIRED = "maxLengthRequired";

    public static final String FIELD_PREFIX = "prefix";

    @PluginParameter(name = FIELD_REMOVE_EXT, label = "Enable extension removal",
            description = "Remove file extension truncating from last dot index if no extension list specified",
            optional = true)
    private Boolean removeExtension;

    @PluginParameter(name = FIELD_EXTS, label = "List of extensions to remove",
            description = "Full qualified extension strings", optional = true)
    private List<String> extensions;

    @PluginParameter(name = FIELD_LENGTH, label = "Max product name length",
            description = "Product name is truncated to max length", optional = true)
    private Integer maxLength;

    @PluginParameter(name = FIELD_LENGTH_REQUIRED, label = "Throw error if max length not reached",
            description = "Only available if max product name length is set", defaultValue = "false", optional = true)
    private Boolean maxLengthRequired;

    @PluginParameter(name = FIELD_PREFIX, label = "Optional prefix",
            description = "Prefix is concatenated with product name", optional = true)
    private String prefix;

    @Override
    public String getProductName(Path filePath) throws ModuleException {
        String productName = filePath.getFileName().toString();

        // Remove extension
        if ((removeExtension != null) && removeExtension) {
            int indexExtension = -1;
            if ((extensions != null) && extensions.isEmpty()) {
                for (String extension : extensions) {
                    indexExtension = productName.lastIndexOf(extension);
                    if (indexExtension > 0) {
                        break;
                    }
                }
            } else {
                indexExtension = productName.lastIndexOf('.');
            }
            if (indexExtension > 0) {
                productName = productName.substring(0, indexExtension);
            }
        }

        // Prefix
        if ((prefix != null) && !prefix.isEmpty()) {
            productName = prefix + productName;
        }

        // Truncate
        if (maxLength != null) {
            if (productName.length() >= maxLength) {
                productName = productName.substring(0, maxLength);
            } else {
                if (maxLengthRequired) {
                    String message = String.format("Product name \"%s\" is shorted than max length", productName);
                    throw new ModuleException(message);
                }
            }
        }
        return productName;
    }
}
